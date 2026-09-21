package com.juthing.idle.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juthing.idle.R
import com.juthing.idle.core.ui.components.SettingsGroup
import com.juthing.idle.core.ui.components.SettingsPage
import com.juthing.idle.core.ui.components.SettingsRow
import com.juthing.idle.domain.repository.ThemeMode

/** Light, dark, or whatever the phone is doing. */
@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsPage(
        title = stringResource(R.string.settings_appearance),
        onBack = onBack,
        modifier = modifier,
    ) { contentModifier ->
        Column(modifier = contentModifier.padding(top = 8.dp)) {
            SettingsGroup {
                Column(modifier = Modifier.selectableGroup()) {
                    ThemeMode.entries.forEach { mode ->
                        SettingsRow(
                            icon = mode.icon(),
                            title = stringResource(mode.labelRes()),
                            onClick = { viewModel.setThemeMode(mode) },
                            trailing = {
                                RadioButton(selected = uiState.themeMode == mode, onClick = null)
                            },
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.settings_appearance_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 16.dp),
            )
        }
    }
}

private fun ThemeMode.icon() = when (this) {
    ThemeMode.SYSTEM -> Icons.Outlined.Brightness4
    ThemeMode.LIGHT -> Icons.Outlined.LightMode
    ThemeMode.DARK -> Icons.Outlined.DarkMode
}
