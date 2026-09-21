package com.juthing.idle.ui.unlockmethods

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juthing.idle.R
import com.juthing.idle.core.ui.components.NfcReaderEffect
import com.juthing.idle.core.ui.components.QrScanner
import com.juthing.idle.data.system.NfcAvailability
import com.juthing.idle.data.system.NfcTagReader
import com.juthing.idle.domain.model.UnlockMethodType

/**
 * Registers a new unlock method.
 *
 * The screen asks for the camera or location permission at the moment it is actually needed,
 * with the reason on screen next to the request: a permission prompt that arrives out of context
 * is one the user refuses.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMethodScreen(
    nfcTagReader: NfcTagReader,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateMethodViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onClose()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.new_method_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
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
                        )

                        UnlockMethodType.NFC -> NfcCapture(
                            reader = nfcTagReader,
                            availability = uiState.nfcAvailability,
                            captured = uiState.capturedSecret != null,
                            onTag = viewModel::capture,
                        )

                        UnlockMethodType.LOCATION -> LocationCapture(
                            captured = uiState.latitude != null,
                            locating = uiState.locating,
                            radiusMeters = uiState.radiusMeters,
                            onCapture = viewModel::captureCurrentPlace,
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

/** Presents the three kinds of method, each with the gesture it will require. */
@Composable
private fun TypeChooser(onSelect: (UnlockMethodType) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        UnlockMethodType.entries.forEach { type ->
            Card(
                onClick = { onSelect(type) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(type.labelRes()),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(type.summaryRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

/** Shows the camera once the permission is granted, and confirms the capture. */
@Composable
private fun QrCapture(captured: Boolean, onDecoded: (String) -> Unit) {
    var granted by remember { mutableStateOf(false) }
    var refused by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { result ->
        granted = result
        refused = !result
    }

    when {
        captured -> CaptureDone(stringResource(R.string.method_capture_qr_done))

        granted -> {
            Text(stringResource(R.string.method_capture_qr), style = MaterialTheme.typography.bodyMedium)
            QrScanner(
                onDecoded = onDecoded,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp)),
            )
        }

        else -> PermissionRequest(
            rationale = stringResource(R.string.permission_camera_rationale),
            refused = refused,
            onRequest = { launcher.launch(Manifest.permission.CAMERA) },
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
) {
    when {
        captured -> CaptureDone(stringResource(R.string.method_capture_nfc_done))

        availability == NfcAvailability.UNSUPPORTED ->
            Text(
                text = stringResource(R.string.nfc_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )

        availability == NfcAvailability.DISABLED ->
            Text(
                text = stringResource(R.string.nfc_disabled),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )

        else -> {
            NfcReaderEffect(reader = reader, onTag = onTag)
            Text(
                text = stringResource(R.string.method_capture_nfc),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** Records the current position once, then lets the user widen or narrow the area. */
@Composable
private fun LocationCapture(
    captured: Boolean,
    locating: Boolean,
    radiusMeters: Int,
    onCapture: () -> Unit,
    onRadiusChange: (Int) -> Unit,
) {
    var granted by remember { mutableStateOf(false) }
    var refused by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { result ->
        granted = result
        refused = !result
        if (result) onCapture()
    }

    when {
        !granted && !captured -> PermissionRequest(
            rationale = stringResource(R.string.permission_location_rationale),
            refused = refused,
            onRequest = { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
        )

        locating -> Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator()
            Text(stringResource(R.string.method_locating), style = MaterialTheme.typography.bodyMedium)
        }

        else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (captured) {
                CaptureDone(stringResource(R.string.method_capture_location_done))
            }
            OutlinedButton(onClick = onCapture, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.method_capture_location))
            }
            Text(
                text = stringResource(R.string.method_radius, radiusMeters),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // 20 m is about a room, 500 m about a neighbourhood.
            Slider(
                value = radiusMeters.toFloat(),
                onValueChange = { onRadiusChange(it.toInt()) },
                valueRange = 20f..500f,
                steps = (500 - 20) / 10 - 1,
            )
        }
    }
}

/** A confirmation that something was registered, without echoing what it was. */
@Composable
private fun CaptureDone(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Explains why a permission is needed, right next to the button that asks for it. */
@Composable
private fun PermissionRequest(
    rationale: String,
    refused: Boolean,
    onRequest: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = rationale, style = MaterialTheme.typography.bodyMedium)
        if (refused) {
            Text(
                text = stringResource(R.string.permission_denied),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.permission_grant))
        }
    }
}
