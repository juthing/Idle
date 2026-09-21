package com.juthing.idle.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juthing.idle.R
import com.juthing.idle.core.ui.components.SettingsGroup
import com.juthing.idle.core.ui.components.SettingsPage
import com.juthing.idle.core.ui.components.SettingsRow
import com.juthing.idle.data.system.PermissionChecker

/**
 * What Idle needs from Android, whether it has it, and where to go and grant it.
 *
 * Two of these are not conveniences. Without the accessibility service Idle never learns that an
 * app was opened; without permission to draw over other apps, Android silently refuses to let the
 * block screen appear — the user sees an app that works sometimes and not others. Both are stated
 * as required, in those words.
 */
@Composable
fun PermissionsScreen(
    permissionChecker: PermissionChecker,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    SettingsPage(
        title = stringResource(R.string.settings_permissions),
        onBack = onBack,
        modifier = modifier,
    ) { contentModifier ->
        Column(
            modifier = contentModifier
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SettingsGroup(title = stringResource(R.string.permissions_required)) {
                PermissionRow(
                    icon = Icons.Outlined.Accessibility,
                    title = stringResource(R.string.permission_accessibility),
                    summary = stringResource(R.string.permission_accessibility_summary),
                    granted = uiState.accessibilityEnabled,
                    onClick = {
                        context.startActivity(permissionChecker.accessibilitySettingsIntent())
                    },
                )
                PermissionRow(
                    icon = Icons.Outlined.Layers,
                    title = stringResource(R.string.permission_overlay),
                    summary = stringResource(R.string.permission_overlay_summary),
                    granted = uiState.overlayAllowed,
                    onClick = { context.startActivity(permissionChecker.overlaySettingsIntent()) },
                )
            }

            SettingsGroup(title = stringResource(R.string.permissions_recommended)) {
                PermissionRow(
                    icon = Icons.Outlined.QueryStats,
                    title = stringResource(R.string.permission_usage_access),
                    summary = stringResource(R.string.permission_usage_access_summary),
                    granted = uiState.usageAccessGranted,
                    onClick = { context.startActivity(permissionChecker.usageAccessIntent()) },
                )
                PermissionRow(
                    icon = Icons.Outlined.Notifications,
                    title = stringResource(R.string.permission_notifications),
                    summary = stringResource(R.string.permission_notifications_summary),
                    granted = uiState.notificationsGranted,
                    onClick = { context.startActivity(permissionChecker.appSettingsIntent()) },
                )
                PermissionRow(
                    icon = Icons.Outlined.BatteryStd,
                    title = stringResource(R.string.permission_battery),
                    summary = stringResource(R.string.permission_battery_summary),
                    granted = uiState.batteryUnrestricted,
                    onClick = { context.startActivity(permissionChecker.batteryOptimizationIntent()) },
                )
            }

            Text(
                text = stringResource(R.string.permissions_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 28.dp),
            )
        }
    }
}

/** One permission, its state, and the way to Android's screen for it. */
@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    summary: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    SettingsRow(
        icon = icon,
        title = title,
        summary = summary,
        iconTint = if (granted) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.error
        },
        // A granted permission still opens its settings screen: the user may want to take it
        // back, and an app that only offers the door in one direction is not being honest.
        onClick = onClick,
        trailing = {
            Icon(
                imageVector = if (granted) {
                    Icons.Outlined.CheckCircle
                } else {
                    Icons.Outlined.ErrorOutline
                },
                contentDescription = stringResource(
                    if (granted) {
                        R.string.permission_status_granted
                    } else {
                        R.string.permission_status_missing
                    },
                ),
                tint = if (granted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        },
    )
}
