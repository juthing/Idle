package com.juthing.idle.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juthing.idle.R
import com.juthing.idle.data.system.PermissionChecker
import kotlinx.coroutines.launch

/** The steps of the first-run flow, in order. */
private enum class OnboardingStep { WELCOME, USAGE, ACCESSIBILITY, NOTIFICATIONS, DONE }

/**
 * Explains what Idle needs and why, one permission at a time.
 *
 * The accessibility step is a Google Play requirement as much as a courtesy: an app using that
 * API outside accessibility must show a disclosure describing the data it reads and take explicit
 * consent before sending the user to the settings screen. Nothing here may be skipped past
 * silently, but nothing is mandatory either: refusing leaves a working app minus that feature.
 *
 * Completing the flow writes a preference; the activity watches that preference and swaps this
 * screen out, so no callback is needed here.
 */
@Composable
fun OnboardingScreen(
    permissionChecker: PermissionChecker,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val pagerState = rememberPagerState { OnboardingStep.entries.size }
    val coroutineScope = rememberCoroutineScope()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val goNext: () -> Unit = {
        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, userScrollEnabled = false) { page ->
            when (OnboardingStep.entries[page]) {
                OnboardingStep.WELCOME -> Step(
                    title = stringResource(R.string.onboarding_welcome_title),
                    body = stringResource(R.string.onboarding_welcome_body),
                    primaryLabel = stringResource(R.string.onboarding_next),
                    onPrimary = goNext,
                )

                OnboardingStep.USAGE -> Step(
                    title = stringResource(R.string.onboarding_usage_title),
                    body = stringResource(R.string.onboarding_usage_body),
                    granted = uiState.usageAccessGranted,
                    primaryLabel = stringResource(R.string.onboarding_open_settings),
                    onPrimary = { context.startActivity(permissionChecker.usageAccessIntent()) },
                    onSkip = goNext,
                )

                OnboardingStep.ACCESSIBILITY -> AccessibilityStep(
                    granted = uiState.accessibilityEnabled,
                    consentGiven = uiState.accessibilityConsentGiven,
                    onConsent = viewModel::giveAccessibilityConsent,
                    onOpenSettings = {
                        context.startActivity(permissionChecker.accessibilitySettingsIntent())
                    },
                    onSkip = goNext,
                )

                OnboardingStep.NOTIFICATIONS -> NotificationStep(
                    granted = uiState.notificationsGranted,
                    onSkip = goNext,
                )

                OnboardingStep.DONE -> Step(
                    title = stringResource(R.string.onboarding_done_title),
                    body = stringResource(R.string.onboarding_done_body),
                    primaryLabel = stringResource(R.string.onboarding_start),
                    onPrimary = viewModel::finish,
                )
            }
        }
    }
}

/** The disclosure Google Play requires, with consent gating the settings button. */
@Composable
private fun AccessibilityStep(
    granted: Boolean,
    consentGiven: Boolean,
    onConsent: () -> Unit,
    onOpenSettings: () -> Unit,
    onSkip: () -> Unit,
) {
    StepScaffold(
        title = stringResource(R.string.onboarding_accessibility_title),
        body = stringResource(R.string.onboarding_accessibility_body),
        granted = granted,
    ) {
        Text(
            text = stringResource(R.string.onboarding_accessibility_detail),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Checkbox(checked = consentGiven, onCheckedChange = { onConsent() })
            Text(
                text = stringResource(R.string.onboarding_accessibility_consent),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Button(
            onClick = onOpenSettings,
            // Play policy: no route to the settings screen before the user has agreed.
            enabled = consentGiven && !granted,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.onboarding_open_settings))
        }
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (granted) R.string.onboarding_next else R.string.onboarding_skip))
        }
    }
}

/** The notification permission, which is a runtime prompt from Android 13 onwards. */
@Composable
private fun NotificationStep(granted: Boolean, onSkip: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { onSkip() }

    Step(
        title = stringResource(R.string.onboarding_notifications_title),
        body = stringResource(R.string.onboarding_notifications_body),
        granted = granted,
        primaryLabel = stringResource(R.string.permission_grant),
        onPrimary = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onSkip()
            }
        },
        onSkip = onSkip,
    )
}

/** One step: a title, an explanation, an action, and a way past it. */
@Composable
private fun Step(
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    granted: Boolean = false,
    onSkip: (() -> Unit)? = null,
) {
    StepScaffold(title = title, body = body, granted = granted) {
        Button(
            onClick = onPrimary,
            enabled = !granted,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            Text(primaryLabel)
        }
        onSkip?.let { skip ->
            TextButton(onClick = skip, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (granted) R.string.onboarding_next else R.string.onboarding_skip))
            }
        }
    }
}

/** Shared layout: everything sits low on the screen, within thumb reach. */
@Composable
private fun StepScaffold(
    title: String,
    body: String,
    granted: Boolean,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp),
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
        if (granted) {
            Text(
                text = stringResource(R.string.onboarding_granted),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        content()
    }
}
