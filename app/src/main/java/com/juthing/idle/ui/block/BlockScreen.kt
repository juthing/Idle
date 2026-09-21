package com.juthing.idle.ui.block

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juthing.idle.R
import com.juthing.idle.core.ui.DurationFormat
import com.juthing.idle.core.ui.components.AppIcon
import com.juthing.idle.core.ui.components.CaptureState
import com.juthing.idle.core.ui.components.UnlockFailureMessage
import com.juthing.idle.core.ui.components.UnlockPanel
import com.juthing.idle.core.ui.messageRes
import com.juthing.idle.core.ui.tap
import com.juthing.idle.data.system.NfcTagReader
import com.juthing.idle.domain.model.BlockReason
import kotlinx.coroutines.delay

/**
 * The screen the user meets instead of the app they opened.
 *
 * It names the app, says plainly what is blocking and until when, offers the one method that
 * lifts it, and keeps the emergency escape hatch present but quiet. Nothing here is decorative
 * except the target itself: this screen exists at a moment when the user is impatient, and every
 * extra element is one more thing to argue with.
 */
@Composable
fun BlockScreen(
    nfcTagReader: NfcTagReader,
    onDismiss: () -> Unit,
    viewModel: BlockViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current

    LaunchedEffect(uiState.dismissed, uiState.unlocked) {
        if (!uiState.dismissed || uiState.loading) return@LaunchedEffect
        // When the block was actually lifted, the confirmation gets a moment on screen; when it
        // simply no longer applies, there is nothing to celebrate and the screen just goes.
        if (uiState.unlocked) delay(900)
        onDismiss()
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val reason = uiState.reason ?: return@Column

            BlockHeader(
                packageName = uiState.packageName,
                appLabel = uiState.appLabel,
                reason = reason,
                limitLabel = { minutes -> DurationFormat.duration(resources, minutes) },
            )

            val method = uiState.method
            if (method == null) {
                Text(
                    text = stringResource(R.string.block_method_broken),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            } else {
                UnlockPanel(
                    method = method,
                    nfcTagReader = nfcTagReader,
                    nfcAvailability = uiState.nfcAvailability,
                    state = when {
                        uiState.unlocked -> CaptureState.DONE
                        uiState.failure != null -> CaptureState.FAILED
                        else -> CaptureState.WAITING
                    },
                    checkingPosition = uiState.checkingPosition,
                    onScan = viewModel::submitScan,
                    onTag = viewModel::submitTag,
                    onCheckPosition = viewModel::submitPosition,
                )

                uiState.failure?.let { failure ->
                    UnlockFailureMessage(stringResource(failure.messageRes()))
                }
            }

            if (!uiState.unlocked) {
                HorizontalDivider(modifier = Modifier.fillMaxWidth(0.4f))

                EmergencyButton(
                    secondsLeft = uiState.emergencySecondsLeft,
                    onUse = viewModel::useEmergency,
                )

                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.block_close))
                }
            }
        }
    }
}

/** The app, its name, and the one sentence that says why it will not open. */
@Composable
private fun BlockHeader(
    packageName: String,
    appLabel: String,
    reason: BlockReason,
    limitLabel: (Int) -> String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // The app's own icon, dimmed behind a tonal disc: it is recognised instantly, and
        // showing it greyed says "not this one, not now" before a single word is read.
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(packageName = packageName, size = 40.dp)
        }

        Text(
            text = appLabel,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )

        Text(
            text = when (reason) {
                is BlockReason.DuringPeriod -> stringResource(
                    R.string.block_reason_period_short,
                    DurationFormat.clock(reason.endsAtMinute),
                )

                is BlockReason.TimerExhausted -> stringResource(
                    R.string.block_reason_timer_short,
                    limitLabel(reason.limitMinutes),
                )
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The daily escape hatch.
 *
 * A text button under the real unlock, never a second prominent action, and it states what it
 * costs before it is used. One confirming tap keeps it from being hit by reflex.
 */
@Composable
private fun EmergencyButton(
    secondsLeft: Int,
    onUse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    var confirming by remember { mutableStateOf(false) }

    if (secondsLeft <= 0) {
        Text(
            text = stringResource(R.string.block_emergency_spent),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (confirming) {
            Text(
                text = stringResource(R.string.block_emergency_confirm),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        TextButton(
            onClick = {
                haptics.tap()
                if (confirming) onUse() else confirming = true
            },
        ) {
            Text(stringResource(R.string.block_emergency, secondsLeft / 60))
        }
    }
}
