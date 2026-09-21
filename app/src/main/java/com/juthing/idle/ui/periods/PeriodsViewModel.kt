package com.juthing.idle.ui.periods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juthing.idle.domain.model.Rule
import com.juthing.idle.domain.repository.RuleRepository
import com.juthing.idle.domain.usecase.CanEditRuleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One period as the list draws it.
 *
 * @property locked whether the period is running right now, in which case it cannot be switched
 *   off or edited without going through its unlock method.
 */
data class PeriodItem(
    val rule: Rule.Period,
    val locked: Boolean,
)

/** What the Periods screen shows. */
data class PeriodsUiState(
    val items: List<PeriodItem> = emptyList(),
    val loading: Boolean = true,
)

/** Drives the list of periods. */
@HiltViewModel
class PeriodsViewModel @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val canEditRule: CanEditRuleUseCase,
) : ViewModel() {

    val uiState: StateFlow<PeriodsUiState> = ruleRepository.observePeriods()
        .map { periods ->
            PeriodsUiState(
                items = periods.map { PeriodItem(it, locked = !canEditRule(it)) },
                loading = false,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PeriodsUiState(),
        )

    /**
     * Turns a period on or off.
     *
     * Silently ignored while the period is running: the list already shows it as locked, and
     * failing quietly is better than pretending the switch worked.
     */
    fun setEnabled(rule: Rule.Period, enabled: Boolean) {
        viewModelScope.launch {
            if (canEditRule(rule)) ruleRepository.setEnabled(rule.id, enabled)
        }
    }
}
