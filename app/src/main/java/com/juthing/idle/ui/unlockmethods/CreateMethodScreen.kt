package com.juthing.idle.ui.unlockmethods

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juthing.idle.R
import com.juthing.idle.core.ui.components.BackButton
import com.juthing.idle.core.ui.components.CaptureStage
import com.juthing.idle.core.ui.components.CaptureState
import com.juthing.idle.core.ui.components.NfcReaderEffect
import com.juthing.idle.core.ui.components.PlacePicker
import com.juthing.idle.core.ui.components.QrScanner
import com.juthing.idle.core.ui.components.icon
import com.juthing.idle.core.ui.components.labelRes
import com.juthing.idle.core.ui.components.summaryRes
import com.juthing.idle.core.ui.step
import com.juthing.idle.core.ui.tap
import com.juthing.idle.data.system.NfcAvailability
import com.juthing.idle.data.system.NfcTagReader
import com.juthing.idle.domain.model.UnlockMethodType

/**
 * Registers a new unlock method.
 *
 * The screen asks for the camera or location permission at the moment it is actually needed,
 * with the reason on screen next to the request: a permission prompt that arrives out of context
 * is one the user refuses.
 *
 * Registering is also the only rehearsal the user gets for the gesture they will have to repeat
 * later, at a worse moment. It is worth making it read like the real thing, which is why it uses
 * the same target and the same confirmation as the block screen.
 *
 * @param onCreated called with the identifier of the method that was just saved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMethodScreen(
    nfcTagReader: NfcTagReader,
    onCreated: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateMethodViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.savedMethodId) {
        uiState.savedMethodId?.let(onCreated)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            uiState.type?.labelRes() ?: R.string.new_method_title,
                        ),
                    )
                },
                navigationIcon = {
                    BackButton(
                        // Backing out of a kind returns to the list of kinds rather than leaving
                        // the screen: choosing the wrong one should not cost the whole flow.
                        onBack = { if (uiState.type == null) onClose() else viewModel.clearType() },
                    )
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            when (val type = uiState.type) {
                null -> TypeChooser(onSelect = viewModel::selectType)

                else -> {
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = viewModel::setName,
                        label = { Text(stringResource(R.string.method_name_label)) },
                        placeholder = { Text(stringResource(R.string.method_name_placeholder)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )

                    when (type) {
                        UnlockMethodType.QR -> QrCapture(
                            captured = uiState.capturedSecret != null,
                            onDecoded = viewModel::capture,
                            onRetry = viewModel::recapture,
                        )

                        UnlockMethodType.NFC -> NfcCapture(
                            reader = nfcTagReader,
                            availability = uiState.nfcAvailability,
                            captured = uiState.capturedSecret != null,
                            onTag = viewModel::capture,
                            onRetry = viewModel::recapture,
                        )

                        UnlockMethodType.LOCATION -> LocationCapture(
                            uiState = uiState,
                            onCapture = viewModel::captureCurrentPlace,
                            onMove = viewModel::moveTo,
                            onRecentre = viewModel::recentre,
                            onRadiusChange = viewModel::setRadius,
                        )
                    }

                    Button(
                        onClick = viewModel::save,
                        enabled = uiState.canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp),
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                }
            }
        }
    }
}

/** Presents the three kinds of method, each with its own icon and the gesture it will require. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeChooser(onSelect: (UnlockMethodType) -> Unit) {
    val haptics = LocalHapticFeedback.current

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
    ) {
        UnlockMethodType.entries.forEach { type ->
            Card(
                onClick = {
                    haptics.tap()
                    onSelect(type)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = type.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(type.labelRes()),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(type.summaryRes()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Shows the camera once the permission is granted, and confirms the capture. */
@Composable
private fun QrCapture(captured: Boolean, onDecoded: (String) -> Unit, onRetry: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    var granted by remember { mutableStateOf(false) }
    var refused by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { result ->
        granted = result
        refused = !result
    }

    when {
        captured -> CapturedAgain(
            icon = UnlockMethodType.QR,
            message = stringResource(R.string.method_capture_qr_done),
            onRetry = onRetry,
        )

        granted -> CaptureStage(
            icon = UnlockMethodType.QR.icon,
            title = stringResource(R.string.method_capture_qr),
            subtitle = stringResource(R.string.method_capture_qr_hint),
            state = CaptureState.BUSY,
            content = { QrScanner(onDecoded = onDecoded, modifier = Modifier.fillMaxSize()) },
        )

        else -> PermissionRequest(
            icon = UnlockMethodType.QR,
            rationale = stringResource(R.string.permission_camera_rationale),
            refused = refused,
            onRequest = {
                haptics.tap()
                launcher.launch(Manifest.permission.CAMERA)
            },
        )
    }
}

/** Waits for a tag, or explains why NFC cannot be used on this phone. */
@Composable
private fun NfcCapture(
    reader: NfcTagReader,
    availability: NfcAvailability,
    captured: Boolean,
    onTag: (String) -> Unit,
    onRetry: () -> Unit,
) {
    if (captured) {
        CapturedAgain(
            icon = UnlockMethodType.NFC,
            message = stringResource(R.string.method_capture_nfc_done),
            onRetry = onRetry,
        )
        return
    }

    if (availability == NfcAvailability.AVAILABLE) {
        NfcReaderEffect(reader = reader, onTag = onTag)
    }

    CaptureStage(
        icon = UnlockMethodType.NFC.icon,
        title = when (availability) {
            NfcAvailability.UNSUPPORTED -> stringResource(R.string.nfc_unavailable)
            NfcAvailability.DISABLED -> stringResource(R.string.nfc_disabled)
            NfcAvailability.AVAILABLE -> stringResource(R.string.method_capture_nfc)
        },
        subtitle = if (availability == NfcAvailability.AVAILABLE) {
            stringResource(R.string.method_capture_nfc_hint)
        } else {
            null
        },
        state = if (availability == NfcAvailability.AVAILABLE) {
            CaptureState.WAITING
        } else {
            CaptureState.FAILED
        },
    )
}

/**
 * Records a place, then lets the user move the point and size the area.
 *
 * The position read is only a starting point. Someone registering "the office" is rarely standing
 * in the office while they set it up, and forcing them to be there was the difference between a
 * method they could create and one they could not.
 */
@Composable
private fun LocationCapture(
    uiState: CreateMethodUiState,
    onCapture: () -> Unit,
    onMove: (Double, Double) -> Unit,
    onRecentre: () -> Unit,
    onRadiusChange: (Int) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var granted by remember { mutableStateOf(false) }
    var refused by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { result ->
        granted = result
        refused = !result
        if (result) onCapture()
    }

    val anchorLatitude = uiState.anchorLatitude
    val anchorLongitude = uiState.anchorLongitude
    val latitude = uiState.latitude
    val longitude = uiState.longitude

    when {
        !granted && anchorLatitude == null -> PermissionRequest(
            icon = UnlockMethodType.LOCATION,
            rationale = stringResource(R.string.permission_location_rationale),
            refused = refused,
            onRequest = {
                haptics.tap()
                launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            },
        )

        uiState.locating -> CaptureStage(
            icon = UnlockMethodType.LOCATION.icon,
            title = stringResource(R.string.method_locating),
            state = CaptureState.BUSY,
        )

        anchorLatitude == null || anchorLongitude == null ||
            latitude == null || longitude == null -> Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CaptureStage(
                icon = UnlockMethodType.LOCATION.icon,
                title = stringResource(R.string.method_capture_location),
                subtitle = if (uiState.locationFailed) {
                    stringResource(R.string.method_location_failed)
                } else {
                    null
                },
                state = if (uiState.locationFailed) CaptureState.FAILED else CaptureState.WAITING,
            )
            Button(onClick = onCapture, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.MyLocation, contentDescription = null)
                Text(
                    text = stringResource(R.string.method_capture_location),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        else -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PlacePicker(
                anchorLatitude = anchorLatitude,
                anchorLongitude = anchorLongitude,
                latitude = latitude,
                longitude = longitude,
                radiusMeters = uiState.radiusMeters,
                onMove = onMove,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        haptics.tap()
                        onRecentre()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.MyLocation, contentDescription = null)
                    Text(
                        text = stringResource(R.string.method_recentre),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                OutlinedButton(
                    onClick = {
                        haptics.tap()
                        onCapture()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Text(
                        text = stringResource(R.string.method_refresh_position),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            Text(
                text = stringResource(R.string.method_radius, uiState.radiusMeters),
                style = MaterialTheme.typography.titleMedium,
            )
            // 20 m is about a room, 500 m about a neighbourhood.
            Slider(
                value = uiState.radiusMeters.toFloat(),
                onValueChange = {
                    val meters = it.toInt()
                    if (meters != uiState.radiusMeters) haptics.step()
                    onRadiusChange(meters)
                },
                valueRange = 20f..500f,
                steps = (500 - 20) / 10 - 1,
            )
            Text(
                text = stringResource(R.string.method_radius_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A confirmation that something was registered, without echoing what it was. */
@Composable
private fun CapturedAgain(icon: UnlockMethodType, message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CaptureStage(icon = icon.icon, title = message, state = CaptureState.DONE)
        // Offered rather than forced: the wrong tag is easy to present and starting the whole
        // method over because of it would be punishment for a slip.
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.method_capture_again))
        }
    }
}

/** Explains why a permission is needed, right next to the button that asks for it. */
@Composable
private fun PermissionRequest(
    icon: UnlockMethodType,
    rationale: String,
    refused: Boolean,
    onRequest: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CaptureStage(
            icon = icon.icon,
            title = rationale,
            subtitle = if (refused) stringResource(R.string.permission_denied) else null,
            state = if (refused) CaptureState.FAILED else CaptureState.WAITING,
        )
        Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.permission_grant))
        }
    }
}
