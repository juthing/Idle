package com.juthing.idle.core.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.juthing.idle.R
import com.juthing.idle.core.ui.tap
import com.juthing.idle.data.system.NfcAvailability
import com.juthing.idle.data.system.NfcTagReader
import com.juthing.idle.domain.model.UnlockMethod
import com.juthing.idle.domain.model.UnlockMethodType

/**
 * The part of a screen that takes an unlock attempt.
 *
 * Shared by the block screen and by the screen that opens a running rule for editing: both ask
 * for exactly the same proof, and a QR code that is framed one way in front of a blocked app and
 * another way inside Settings is a QR code the user has to think about twice.
 *
 * @param state what to draw on the target — waiting, working, done or refused.
 * @param checkingPosition whether a position read is in flight; the place method has no camera to
 *   show while it waits.
 */
@Composable
fun UnlockPanel(
    method: UnlockMethod,
    nfcTagReader: NfcTagReader,
    nfcAvailability: NfcAvailability,
    state: CaptureState,
    checkingPosition: Boolean,
    onScan: (String) -> Unit,
    onTag: (String) -> Unit,
    onCheckPosition: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        when (method.type) {
            UnlockMethodType.QR -> QrUnlock(method = method, state = state, onDecoded = onScan)

            UnlockMethodType.NFC -> NfcUnlock(
                method = method,
                reader = nfcTagReader,
                availability = nfcAvailability,
                state = state,
                onTag = onTag,
            )

            UnlockMethodType.LOCATION -> PlaceUnlock(
                method = method,
                state = state,
                checking = checkingPosition,
                onCheck = onCheckPosition,
            )
        }
    }
}

@Composable
private fun QrUnlock(method: UnlockMethod, state: CaptureState, onDecoded: (String) -> Unit) {
    val haptics = LocalHapticFeedback.current
    var granted by remember { mutableStateOf(false) }
    var refused by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { result ->
        granted = result
        refused = !result
    }

    if (granted || state == CaptureState.DONE) {
        CaptureStage(
            icon = UnlockMethodType.QR.icon,
            title = stringResource(
                if (state == CaptureState.DONE) {
                    R.string.capture_done_qr
                } else {
                    R.string.capture_prompt_qr
                },
            ),
            subtitle = method.name,
            state = if (state == CaptureState.WAITING) CaptureState.BUSY else state,
            content = {
                QrScanner(onDecoded = onDecoded, modifier = Modifier.fillMaxSize())
            },
        )
    } else {
        CaptureStage(
            icon = UnlockMethodType.QR.icon,
            title = stringResource(R.string.permission_camera_rationale),
            subtitle = if (refused) stringResource(R.string.permission_denied) else null,
            state = CaptureState.WAITING,
        )
        Button(
            onClick = {
                haptics.tap()
                launcher.launch(Manifest.permission.CAMERA)
            },
        ) {
            Text(stringResource(R.string.permission_grant))
        }
    }
}

@Composable
private fun NfcUnlock(
    method: UnlockMethod,
    reader: NfcTagReader,
    availability: NfcAvailability,
    state: CaptureState,
    onTag: (String) -> Unit,
) {
    if (availability == NfcAvailability.AVAILABLE && state != CaptureState.DONE) {
        NfcReaderEffect(reader = reader, onTag = onTag)
    }

    val title = when {
        state == CaptureState.DONE -> stringResource(R.string.capture_done_nfc)
        availability == NfcAvailability.UNSUPPORTED -> stringResource(R.string.nfc_unavailable)
        availability == NfcAvailability.DISABLED -> stringResource(R.string.nfc_disabled)
        else -> stringResource(R.string.capture_prompt_nfc)
    }

    CaptureStage(
        icon = UnlockMethodType.NFC.icon,
        title = title,
        subtitle = method.name,
        state = when {
            availability != NfcAvailability.AVAILABLE && state != CaptureState.DONE ->
                CaptureState.FAILED

            else -> state
        },
    )
}

@Composable
private fun PlaceUnlock(
    method: UnlockMethod,
    state: CaptureState,
    checking: Boolean,
    onCheck: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current

    CaptureStage(
        icon = UnlockMethodType.LOCATION.icon,
        title = stringResource(
            if (state == CaptureState.DONE) {
                R.string.capture_done_location
            } else {
                R.string.capture_prompt_location
            },
        ),
        subtitle = method.name,
        state = if (checking) CaptureState.BUSY else state,
    )

    if (state != CaptureState.DONE) {
        if (checking) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    haptics.tap()
                    onCheck()
                },
            ) {
                Icon(Icons.Outlined.MyLocation, contentDescription = null)
                Text(
                    text = stringResource(R.string.block_check_place),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

/** A refusal, said once, under the target rather than anywhere else. */
@Composable
fun UnlockFailureMessage(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}
