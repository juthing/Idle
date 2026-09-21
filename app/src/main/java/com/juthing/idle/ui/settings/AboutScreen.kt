package com.juthing.idle.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.juthing.idle.BuildConfig
import com.juthing.idle.R
import com.juthing.idle.core.ui.components.SettingsGroup
import com.juthing.idle.core.ui.components.SettingsPage
import com.juthing.idle.core.ui.components.SettingsRow

/**
 * What Idle is, and the four things it will never do.
 *
 * Stated as a list of promises rather than a paragraph, because a promise that can be checked one
 * line at a time is one the user can actually hold the app to.
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsPage(
        title = stringResource(R.string.settings_about),
        onBack = onBack,
        modifier = modifier,
    ) { contentModifier ->
        Column(
            modifier = contentModifier
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Outlined.Payments,
                    title = stringResource(R.string.about_free_title),
                    summary = stringResource(R.string.about_free_body),
                )
                SettingsRow(
                    icon = Icons.Outlined.Block,
                    title = stringResource(R.string.about_ads_title),
                    summary = stringResource(R.string.about_ads_body),
                )
                SettingsRow(
                    icon = Icons.Outlined.VisibilityOff,
                    title = stringResource(R.string.about_tracking_title),
                    summary = stringResource(R.string.about_tracking_body),
                )
                SettingsRow(
                    icon = Icons.Outlined.CloudOff,
                    title = stringResource(R.string.about_offline_title),
                    summary = stringResource(R.string.about_offline_body),
                )
            }

            Text(
                text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 28.dp),
            )
        }
    }
}
