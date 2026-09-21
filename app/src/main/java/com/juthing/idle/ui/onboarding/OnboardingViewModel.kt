package com.juthing.idle.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juthing.idle.data.system.PermissionChecker
import com.juthing.idle.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Which permissions are in place, refreshed whenever the user comes back from Android settings.
 *
 * @property accessibilityConsentGiven whether the user has accepted the disclosure. Google Play
 *   requires this affirmative consent before the app may send them to the accessibility settings,
 *   so the button stays out of reach until it is given.
 */
data class OnboardingUiState(
    val usageAccessGranted: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val overlayAllowed: Boolean = false,
    val notificationsGranted: Boolean = false,
    val accessibilityConsentGiven: Boolean = false,
)

/** Drives the first-run flow. */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val permissionChecker: PermissionChecker,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** Re-reads every permission. Called on resume, since they are granted outside the app. */
    fun refresh() = _uiState.update {
        it.copy(
            usageAccessGranted = permissionChecker.hasUsageAccess(),
            accessibilityEnabled = permissionChecker.isAccessibilityServiceEnabled(),
            overlayAllowed = permissionChecker.canDrawOverlays(),
            notificationsGranted = permissionChecker.hasNotificationPermission(),
        )
    }

    fun giveAccessibilityConsent() =
        _uiState.update { it.copy(accessibilityConsentGiven = true) }

    /**
     * Marks the flow as done.
     *
     * Permissions are not required to finish: a user who refuses the accessibility service still
     * gets a working app for everything else, and the settings screen keeps saying what is
     * missing rather than holding them hostage at this screen.
     *
     * The written preference is what the activity watches, so nothing else has to be signalled.
     */
    fun finish() {
        viewModelScope.launch { settingsRepository.setOnboardingCompleted(true) }
    }
}
