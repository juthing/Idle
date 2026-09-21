package com.juthing.idle.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juthing.idle.domain.repository.SettingsRepository
import com.juthing.idle.domain.repository.ThemeMode
import com.juthing.idle.domain.usecase.AnyRuleActiveUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * What the settings screen shows.
 *
 * @property unlockDurationLocked set while any rule is being enforced. Raising the duration from
 *   fifteen minutes to eight hours mid-period would be the shortest way around every rule, so the
 *   setting is frozen until nothing is active.
 */
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val unlockDurationMinutes: Int = SettingsRepository.DEFAULT_UNLOCK_DURATION_MINUTES,
    val unlockDurationLocked: Boolean = false,
)

/** Drives the settings screen. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val anyRuleActive: AnyRuleActiveUseCase,
) : ViewModel() {

    private val lockRefresh = MutableStateFlow(0)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.themeMode,
        settingsRepository.unlockDurationMinutes,
        lockRefresh,
    ) { theme, minutes, _ ->
        SettingsUiState(
            themeMode = theme,
            unlockDurationMinutes = minutes,
            unlockDurationLocked = anyRuleActive(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    /** Changes the unlock duration, refused while a rule is running. */
    fun setUnlockDuration(minutes: Int) {
        viewModelScope.launch {
            if (!anyRuleActive()) settingsRepository.setUnlockDurationMinutes(minutes)
        }
    }

    /** Re-checks whether a rule became active while the screen was open. */
    fun refreshLockState() {
        lockRefresh.value += 1
    }
}
