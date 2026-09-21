package com.juthing.idle.ui.unlockmethods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juthing.idle.domain.model.UnlockMethod
import com.juthing.idle.domain.repository.RuleRepository
import com.juthing.idle.domain.repository.UnlockMethodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One method in the list.
 *
 * @property usedByRules how many rules depend on it; a method still in use cannot be deleted
 *   without disarming the rule it guards.
 */
data class UnlockMethodItem(
    val method: UnlockMethod,
    val usedByRules: Int,
)

/** What the unlock methods screen shows. */
data class UnlockMethodsUiState(
    val items: List<UnlockMethodItem> = emptyList(),
    val loading: Boolean = true,
)

/** Drives the list of unlock methods. */
@HiltViewModel
class UnlockMethodsViewModel @Inject constructor(
    private val unlockMethodRepository: UnlockMethodRepository,
    private val ruleRepository: RuleRepository,
) : ViewModel() {

    val uiState: StateFlow<UnlockMethodsUiState> = unlockMethodRepository.observeAll()
        .map { methods ->
            UnlockMethodsUiState(
                items = methods.map { UnlockMethodItem(it, ruleRepository.countRulesUsing(it.id)) },
                loading = false,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UnlockMethodsUiState(),
        )

    /**
     * Deletes a method.
     *
     * Refused while a rule still points at it: the database would refuse the write anyway, and
     * failing here lets the screen explain why instead of showing a crash.
     */
    fun delete(item: UnlockMethodItem) {
        if (item.usedByRules > 0) return
        viewModelScope.launch { unlockMethodRepository.delete(item.method.id) }
    }
}
