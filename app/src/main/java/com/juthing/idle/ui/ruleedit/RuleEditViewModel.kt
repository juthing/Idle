package com.juthing.idle.ui.ruleedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.juthing.idle.domain.model.InstalledApp
import com.juthing.idle.domain.model.Rule
import com.juthing.idle.domain.model.UnlockMethod
import com.juthing.idle.domain.repository.InstalledAppsRepository
import com.juthing.idle.domain.repository.RuleRepository
import com.juthing.idle.domain.repository.UnlockMethodRepository
import com.juthing.idle.domain.usecase.CanEditRuleUseCase
import com.juthing.idle.ui.navigation.RuleEditRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

/**
 * The rule being written, held as one flat state so that both shapes share a single screen.
 *
 * The fields the current shape does not use keep their defaults and are simply not shown; that is
 * cheaper and far easier to follow than two nearly identical states.
 *
 * @property locked set when an existing rule is running right now, in which case the screen is
 *   read-only until the rule is unlocked.
 * @property saved flipped once the rule is written, which is what tells the screen to leave.
 */
data class RuleEditUiState(
    val isPeriod: Boolean = true,
    val isNew: Boolean = true,
    val loading: Boolean = true,
    val locked: Boolean = false,
    val saved: Boolean = false,
    val ruleId: Long = 0,
    val name: String = "",
    /** Kept so that saving an edit does not quietly switch a disabled rule back on. */
    val enabled: Boolean = true,
    /** Kept so that saving an edit does not reset the rule's age and reorder the list. */
    val createdAt: Long = 0,
    val packageNames: Set<String> = emptySet(),
    val unlockMethodId: Long? = null,
    val daysOfWeek: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val startMinute: Int = 22 * 60,
    val endMinute: Int = 7 * 60,
    val dailyLimitMinutes: Int = 60,
    val installedApps: List<InstalledApp> = emptyList(),
    val availableMethods: List<UnlockMethod> = emptyList(),
) {
    /** A period whose end is not after its start runs overnight. */
    val crossesMidnight: Boolean get() = endMinute <= startMinute

    /** The method currently guarding this rule, if one is chosen and still exists. */
    val selectedMethod: UnlockMethod?
        get() = availableMethods.firstOrNull { it.id == unlockMethodId }

    /**
     * Whether the rule can be written.
     *
     * A rule with no apps would do nothing, and one with no unlock method could never be lifted,
     * so both are required before saving is offered.
     */
    val canSave: Boolean
        get() = !locked && packageNames.isNotEmpty() && unlockMethodId != null
}

/** Drives the screen that creates or edits one period or timer. */
@HiltViewModel
class RuleEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val ruleRepository: RuleRepository,
    private val unlockMethodRepository: UnlockMethodRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val canEditRule: CanEditRuleUseCase,
) : ViewModel() {

    private val route: RuleEditRoute = savedStateHandle.toRoute()

    private val _uiState = MutableStateFlow(
        RuleEditUiState(
            isPeriod = route.isPeriod,
            isNew = route.ruleId == 0L,
            ruleId = route.ruleId,
        ),
    )
    val uiState: StateFlow<RuleEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load() }
        // Observed rather than read once: a method can be created from the sheet on this very
        // screen, and the list has to grow under the user without the draft being rebuilt.
        viewModelScope.launch {
            unlockMethodRepository.observeAll().collect { methods ->
                _uiState.update { state ->
                    state.copy(
                        availableMethods = methods,
                        // With exactly one method there is nothing to choose, so it is preselected.
                        unlockMethodId = state.unlockMethodId ?: methods.singleOrNull()?.id,
                    )
                }
            }
        }
    }

    private suspend fun load() {
        val apps = installedAppsRepository.getLaunchableApps()
        val existing = if (route.ruleId != 0L) ruleRepository.getRule(route.ruleId) else null

        _uiState.update { state ->
            val base = state.copy(loading = false, installedApps = apps)
            when (existing) {
                null -> base
                is Rule.Period -> base.copy(
                    locked = !canEditRule(existing),
                    name = existing.name,
                    enabled = existing.enabled,
                    createdAt = existing.createdAt,
                    packageNames = existing.packageNames,
                    unlockMethodId = existing.unlockMethodId,
                    daysOfWeek = existing.daysOfWeek,
                    startMinute = existing.startMinute,
                    endMinute = existing.endMinute,
                )

                is Rule.Timer -> base.copy(
                    locked = !canEditRule(existing),
                    name = existing.name,
                    enabled = existing.enabled,
                    createdAt = existing.createdAt,
                    packageNames = existing.packageNames,
                    unlockMethodId = existing.unlockMethodId,
                    dailyLimitMinutes = existing.dailyLimitMinutes,
                )
            }
        }
    }

    /**
     * Re-reads whether the rule is still sealed.
     *
     * Called when the screen comes back to the foreground, which is how it learns that the user
     * has just unlocked the rule from the screen next door — or that the period it was waiting
     * out has ended on its own.
     */
    fun refreshLock() {
        val ruleId = route.ruleId
        if (ruleId == 0L) return
        viewModelScope.launch {
            val rule = ruleRepository.getRule(ruleId) ?: return@launch
            _uiState.update { it.copy(locked = !canEditRule(rule)) }
        }
    }

    /** Adopts a method the user has just created from the picker sheet. */
    fun adoptMethod(id: Long) = _uiState.update { it.copy(unlockMethodId = id) }

    fun setName(name: String) = _uiState.update { it.copy(name = name) }

    fun setPackages(packageNames: Set<String>) =
        _uiState.update { it.copy(packageNames = packageNames) }

    fun setUnlockMethod(id: Long) = _uiState.update { it.copy(unlockMethodId = id) }

    /** Toggles one day, keeping at least one selected so a period cannot become a no-op. */
    fun toggleDay(day: DayOfWeek) = _uiState.update { state ->
        val days = if (day in state.daysOfWeek) state.daysOfWeek - day else state.daysOfWeek + day
        if (days.isEmpty()) state else state.copy(daysOfWeek = days)
    }

    fun setStartMinute(minute: Int) = _uiState.update { it.copy(startMinute = minute) }

    fun setEndMinute(minute: Int) = _uiState.update { it.copy(endMinute = minute) }

    fun setDailyLimitMinutes(minutes: Int) =
        _uiState.update { it.copy(dailyLimitMinutes = minutes.coerceIn(5, 8 * 60)) }

    /** Writes the rule and signals the screen to close. */
    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        val unlockMethodId = state.unlockMethodId ?: return

        viewModelScope.launch {
            val fallbackName = state.installedApps
                .firstOrNull { it.packageName in state.packageNames }
                ?.label
                .orEmpty()
            val name = state.name.ifBlank { fallbackName }

            val rule = if (state.isPeriod) {
                Rule.Period(
                    id = route.ruleId,
                    name = name,
                    packageNames = state.packageNames,
                    unlockMethodId = unlockMethodId,
                    enabled = state.enabled,
                    createdAt = state.createdAt,
                    daysOfWeek = state.daysOfWeek,
                    startMinute = state.startMinute,
                    endMinute = state.endMinute,
                )
            } else {
                Rule.Timer(
                    id = route.ruleId,
                    name = name,
                    packageNames = state.packageNames,
                    unlockMethodId = unlockMethodId,
                    enabled = state.enabled,
                    createdAt = state.createdAt,
                    dailyLimitMinutes = state.dailyLimitMinutes,
                )
            }
            ruleRepository.save(rule)
            _uiState.update { it.copy(saved = true) }
        }
    }

    /** Deletes the rule, refused while it is running. */
    fun delete() {
        if (route.ruleId == 0L || _uiState.value.locked) return
        viewModelScope.launch {
            ruleRepository.delete(route.ruleId)
            _uiState.update { it.copy(saved = true) }
        }
    }
}
