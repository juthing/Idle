package com.juthing.idle.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juthing.idle.R
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.juthing.idle.core.ui.DurationFormat
import com.juthing.idle.data.system.PermissionChecker
import com.juthing.idle.domain.repository.ThemeMode

/** Appearance, blocking behaviour, and the way into the unlock methods. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    permissionChecker: PermissionChecker,
    onOpenUnlockMethods: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            // A disabled service means nothing is being blocked at all, which the user has to
            // know before anything else on this screen.
            if (!uiState.accessibilityEnabled) {
                Text(
                    text = stringResource(R.string.permission_accessibility_off_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                )
            }

            SectionTitle(stringResource(R.string.settings_appearance))
            Column(modifier = Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setThemeMode(mode) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        RadioButton(selected = uiState.themeMode == mode, onClick = null)
                        Text(
                            text = stringResource(mode.labelRes()),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionTitle(stringResource(R.string.settings_blocking))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = stringResource(R.string.settings_unlock_duration),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = if (uiState.unlockDurationLocked) {
                        stringResource(R.string.settings_unlock_duration_locked)
                    } else {
                        stringResource(R.string.settings_unlock_duration_summary)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (uiState.unlockDurationLocked) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    text = DurationFormat.duration(resources, uiState.unlockDurationMinutes),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
                // 5 to 60 minutes in steps of five: longer than an hour stops being an unlock.
                Slider(
                    value = uiState.unlockDurationMinutes.toFloat(),
                    onValueChange = { viewModel.setUnlockDuration(it.toInt()) },
                    valueRange = 5f..60f,
                    steps = (60 - 5) / 5 - 1,
                    enabled = !uiState.unlockDurationLocked,
                )
            }

            NavigationRow(
                title = stringResource(R.string.settings_unlock_methods),
                summary = stringResource(R.string.settings_unlock_methods_summary),
                onClick = onOpenUnlockMethods,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionTitle(stringResource(R.string.settings_permissions))
            PermissionRow(
                title = stringResource(R.string.permission_usage_access),
                granted = uiState.usageAccessGranted,
                onClick = { context.startActivity(permissionChecker.usageAccessIntent()) },
            )
            PermissionRow(
                title = stringResource(R.string.permission_accessibility),
                granted = uiState.accessibilityEnabled,
                onClick = { context.startActivity(permissionChecker.accessibilitySettingsIntent()) },
            )
            PermissionRow(
                title = stringResource(R.string.permission_notifications),
                granted = uiState.notificationsGranted,
                onClick = { },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionTitle(stringResource(R.string.settings_about))
            Text(
                text = stringResource(R.string.settings_about_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

/** One permission and whether it is in place; tapping opens where Android grants it. */
@Composable
private fun PermissionRow(title: String, granted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !granted, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            text = stringResource(
                if (granted) R.string.permission_status_granted else R.string.permission_status_missing,
            ),
            style = MaterialTheme.typography.labelLarge,
            color = if (granted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun NavigationRow(title: String, summary: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}
