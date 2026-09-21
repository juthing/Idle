package com.juthing.idle.ui.timers

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.filled.Add
import com.juthing.idle.R
import com.juthing.idle.core.ui.components.EmptyState

/**
 * Placeholder for the timers section.
 *
 * Lays out the shell the real screen will keep — title, empty state and the action
 * that creates a new rule — so that later steps only swap the content in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimersScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.timers_title)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* Wired up in a later step. */ },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.timers_add)) },
            )
        },
    ) { innerPadding ->
        EmptyState(
            icon = Icons.Outlined.HourglassEmpty,
            title = stringResource(R.string.timers_empty_title),
            body = stringResource(R.string.timers_empty_body),
            modifier = Modifier.padding(innerPadding),
        )
    }
}
