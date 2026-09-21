package com.juthing.idle.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juthing.idle.domain.repository.SettingsRepository
import com.juthing.idle.domain.repository.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Holds the one preference the activity needs before drawing anything: the theme.
 *
 * Kept separate from the settings screen's own view model so that the choice survives navigating
 * away from that screen.
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
}
