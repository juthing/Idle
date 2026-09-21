package com.juthing.idle.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juthing.idle.R
import com.juthing.idle.core.ui.DurationFormat
import com.juthing.idle.core.ui.components.SettingsGroup
import com.juthing.idle.core.ui.components.SettingsRow
import com.juthing.idle.domain.repository.ThemeMode

/**
 * The way in to every setting, and nothing else.
 *
 * Each subject lives on its own page. A settings screen that scrolls through appearance, unlock
 * duration, methods, four permissions and an about box is a screen where the one line that says
 * blocking is switched off scrolls past unread — which is exactly what used to happen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenAppearance: () -> Unit,
    onOpenBlocking: () -> Unit,
    onOpenUnlockMethods: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Nothing below matters if this is red: a missing half means Idle notices nothing, or
            // notices and cannot show anything. It is said first, in full, and in colour.
            if (!uiState.blockingWorks) {
                BlockingBrokenBanner(onClick = onOpenPermissions)
            }

            SettingsGroup {
                SettingsRow(
                    icon = Icons.Outlined.Palette,
                    title = stringResource(R.string.settings_appearance),
                    summary = stringResource(uiState.themeMode.labelRes()),
                    onClick = onOpenAppearance,
                )
                SettingsRow(
                    icon = Icons.Outlined.Timelapse,
                    title = stringResource(R.string.settings_blocking),
                    summary = DurationFormat.duration(resources, uiState.unlockDurationMinutes),
                    onClick = onOpenBlocking,
                )
                SettingsRow(
                    icon = Icons.Outlined.Key,
                    title = stringResource(R.string.settings_unlock_methods),
                    summary = stringResource(R.string.settings_unlock_methods_summary),
                    onClick = onOpenUnlockMethods,
                )
            }

            SettingsGroup {
                SettingsRow(
                    icon = Icons.Outlined.Shield,
                    title = stringResource(R.string.settings_permissions),
                    summary = if (uiState.missingPermissions == 0) {
                        stringResource(R.string.settings_permissions_all_granted)
                    } else {
                        pluralStringResource(
                            R.plurals.settings_permissions_missing,
                            uiState.missingPermissions,
                            uiState.missingPermissions,
                        )
                    },
                    iconTint = if (uiState.missingPermissions == 0) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    onClick = onOpenPermissions,
                )
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.settings_about),
                    onClick = onOpenAbout,
                )
            }
        }
    }
}

/** The one thing on this screen the user cannot be allowed to scroll past. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockingBrokenBanner(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Column {
                Text(
                    text = stringResource(R.string.settings_blocking_broken_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(R.string.settings_blocking_broken_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** The localised name of a theme choice, used both on the hub and on the appearance page. */
internal fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}
