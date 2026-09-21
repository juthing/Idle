package com.juthing.idle.ui.block

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juthing.idle.R
import com.juthing.idle.core.ui.DurationFormat
import com.juthing.idle.core.ui.components.NfcReaderEffect
import com.juthing.idle.core.ui.components.QrScanner
import com.juthing.idle.data.system.NfcTagReader
import com.juthing.idle.domain.model.BlockReason
import com.juthing.idle.domain.model.UnlockMethodType
import com.juthing.idle.domain.usecase.UnlockFailure

/**
 * The screen the user meets instead of the app they opened.
 *
 * It says plainly what is blocking and why, offers the one method that lifts it, and keeps the
 * emergency escape hatch present but quiet. Nothing here is decorative: this screen exists at a
 * moment when the user is impatient, and every extra element is one more thing to argue with.
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

    LaunchedEffect(uiState.dismissed) {
        if (uiState.dismissed && !uiState.loading) onDismiss()
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val reason = uiState.reason ?: return@Column

            Text(
                text = stringResource(R.string.block_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = when (reason) {
                    is BlockReason.DuringPeriod -> stringResource(
                        R.string.block_reason_period,
                        uiState.appLabel,
                        DurationFormat.clock(reason.endsAtMinute),
                    )

                    is BlockReason.TimerExhausted -> stringResource(
                        R.string.block_reason_timer,
                        DurationFormat.duration(resources, reason.limitMinutes),
                        uiState.appLabel,
                    )
                },
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )

            uiState.failure?.let { failure ->
                Text(
                    text = stringResource(failure.messageRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            UnlockPrompt(
                viewModel = viewModel,
                nfcTagReader = nfcTagReader,
                modifier = Modifier.padding(top = 24.dp),
            )

            EmergencyButton(
                secondsLeft = uiState.emergencySecondsLeft,
                onUse = viewModel::useEmergency,
                modifier = Modifier.padding(top = 24.dp),
            )

            TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 4.dp)) {
                Text(stringResource(R.string.block_close))
            }
        }
    }
}

/** The part of the screen that actually takes the unlock attempt. */
@Composable
private fun UnlockPrompt(
    viewModel: BlockViewModel,
    nfcTagReader: NfcTagReader,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val method = uiState.method

    if (method == null) {
        Text(
            text = stringResource(R.string.block_method_broken),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.block_unlock, method.name),
            style = MaterialTheme.typography.titleMedium,
        )

        when (method.type) {
            UnlockMethodType.QR -> QrUnlock(onDecoded = viewModel::submitScan)

            UnlockMethodType.NFC -> {
                NfcReaderEffect(reader = nfcTagReader, onTag = viewModel::submitTag)
                Text(
                    text = stringResource(R.string.block_tap_again),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            UnlockMethodType.LOCATION -> {
                Text(
                    text = stringResource(R.string.block_go_to_place, method.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (uiState.checkingPosition) {
                    CircularProgressIndicator()
                } else {
                    Button(onClick = viewModel::submitPosition) {
                        Text(stringResource(R.string.block_check_place))
                    }
                }
            }
        }
    }
}

/** The camera, once the user has allowed it from here. */
@Composable
private fun QrUnlock(onDecoded: (String) -> Unit) {
    var granted by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted = it }

    if (granted) {
        Text(
            text = stringResource(R.string.block_scan_again),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        QrScanner(
            onDecoded = onDecoded,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp)),
        )
    } else {
        Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
            Text(stringResource(R.string.block_unlock_generic))
        }
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
        TextButton(onClick = { if (confirming) onUse() else confirming = true }) {
            Text(stringResource(R.string.block_emergency, secondsLeft / 60))
        }
    }
}

/** What to tell the user about a refused attempt. */
private fun UnlockFailure.messageRes(): Int = when (this) {
    UnlockFailure.MISMATCH -> R.string.block_wrong_code
    UnlockFailure.OUT_OF_AREA -> R.string.block_out_of_area
    UnlockFailure.WRONG_KIND -> R.string.block_wrong_tag
    UnlockFailure.METHOD_INCOMPLETE -> R.string.block_method_broken
}
