package com.juthing.idle.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juthing.idle.domain.repository.SettingsRepository
import com.juthing.idle.domain.repository.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Holds what the activity needs before drawing anything: the theme, and whether the first-run
 * flow still has to be shown.
 *
 * Kept separate from the settings screen's own view model so that both survive navigating away
 * from that screen.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ThemeMode.SYSTEM,
    )

    /**
     * Whether the first-run flow has been completed.
     *
     * `null` until the first value arrives, so the activity draws nothing rather than flashing
     * the main screen at someone who has never seen the app.
     */
    val onboardingCompleted: StateFlow<Boolean?> = settingsRepository.onboardingCompleted
        .map { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )
}
