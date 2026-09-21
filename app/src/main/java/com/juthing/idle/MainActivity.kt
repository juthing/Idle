package com.juthing.idle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juthing.idle.core.ui.theme.IdleTheme
import com.juthing.idle.data.system.NfcTagReader
import com.juthing.idle.domain.repository.ThemeMode
import com.juthing.idle.ui.IdleApp
import com.juthing.idle.data.system.PermissionChecker
import com.juthing.idle.ui.MainViewModel
import com.juthing.idle.ui.onboarding.OnboardingScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The single activity hosting the whole Compose UI.
 *
 * The blocking overlay lives in its own activity so that it can be shown on top of
 * other apps without dragging this one into the back stack.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var nfcTagReader: NfcTagReader

    @Inject
    lateinit var permissionChecker: PermissionChecker

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()

            IdleTheme(darkTheme = darkTheme) {
                when (onboardingCompleted) {
                    // Still loading: draw nothing rather than flash a screen and replace it.
                    null -> Unit
                    false -> OnboardingScreen(permissionChecker = permissionChecker)

                    true -> IdleApp(
                        nfcTagReader = nfcTagReader,
                        permissionChecker = permissionChecker,
                    )
                }
            }
        }
    }
}
