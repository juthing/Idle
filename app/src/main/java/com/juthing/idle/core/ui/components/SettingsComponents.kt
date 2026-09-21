package com.juthing.idle.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.juthing.idle.R
import com.juthing.idle.core.ui.tap

/**
 * The frame every settings page wears.
 *
 * Settings are now one page per subject rather than one long scroll, which only works if the
 * pages are visibly the same object: same compact title, same back arrow, same margins. This is
 * that frame.
 *
 * The bar is the small one on purpose. A large or medium Material 3 app bar reserves 112 to 152dp
 * before a single row is drawn, which on a phone reads as an empty band above every screen rather
 * than as breathing room.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { innerPadding ->
        content(Modifier.padding(innerPadding))
    }
}

/** The back arrow, identical on every screen that has one. */
@Composable
fun BackButton(onBack: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    IconButton(
        onClick = {
            haptics.tap()
            onBack()
        },
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.action_back),
        )
    }
}

/** A group of related rows, drawn as one card so the page reads as a few blocks, not a list. */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 28.dp, end = 16.dp, bottom = 8.dp),
            )
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column { content() }
        }
    }
}

/**
 * One tappable line inside a [SettingsGroup].
 *
 * @param trailing what sits at the end of the row; a chevron by default, but a switch, a status
 *   word or nothing at all when the row is not a door to somewhere else.
 */
@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    iconBackground: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    val clickable = if (onClick == null) {
        Modifier
    } else {
        Modifier.clickable {
            haptics.tap()
            onClick()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickable)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
