package com.juthing.idle.ui.timers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juthing.idle.domain.model.Rule
import com.juthing.idle.domain.repository.RuleRepository
import com.juthing.idle.domain.repository.UsageRepository
import com.juthing.idle.domain.usecase.CanEditRuleUseCase
import com.juthing.idle.domain.usecase.TimerEvaluator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One timer as the list draws it, with today's progress already resolved.
 *
 * @property usedMillis time spent today across every app the timer covers, since they share a
 *   single budget.
 */
data class TimerItem(
    val rule: Rule.Timer,
    val usedMillis: Long,
    val locked: Boolean,
) {
    /** Progress through the daily quota, clamped to `0f..1f` for the progress indicator. */
    val progress: Float
        get() = (usedMillis.toFloat() / (rule.dailyLimitMinutes * 60_000f)).coerceIn(0f, 1f)
}

/** What the Timers screen shows. */
data class TimersUiState(
    val items: List<TimerItem> = emptyList(),
    val loading: Boolean = true,
)

/** Drives the list of timers and their progress for the day. */
@HiltViewModel
class TimersViewModel @Inject constructor(
    private val ruleRepository: RuleRepository,
    usageRepository: UsageRepository,
    private val timerEvaluator: TimerEvaluator,
    private val canEditRule: CanEditRuleUseCase,
) : ViewModel() {

    val uiState: StateFlow<TimersUiState> = combine(
        ruleRepository.observeTimers(),
        usageRepository.observeTodayUsage(),
    ) { timers, usage ->
        TimersUiState(
            items = timers.map { timer ->
                val used = timer.packageNames.sumOf { usage[it] ?: 0L }
                TimerItem(rule = timer, usedMillis = used, locked = !canEditRule(timer))
            },
            loading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TimersUiState(),
    )

    /** Turns a timer on or off, ignored while its quota is spent and the timer is therefore locked. */
    fun setEnabled(rule: Rule.Timer, enabled: Boolean) {
        viewModelScope.launch {
            if (canEditRule(rule)) ruleRepository.setEnabled(rule.id, enabled)
        }
    }

    /** Whether [item] has run out of budget, used to word its subtitle. */
    fun isExhausted(item: TimerItem): Boolean =
        timerEvaluator.isExhausted(item.rule, item.usedMillis)
}
