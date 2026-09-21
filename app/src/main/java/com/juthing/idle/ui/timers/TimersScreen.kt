package com.juthing.idle.ui.timers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juthing.idle.R
import com.juthing.idle.core.ui.DurationFormat
import com.juthing.idle.core.ui.components.EmptyState
import com.juthing.idle.core.ui.components.RuleCard
import com.juthing.idle.core.ui.tap

/**
 * Lists the timers and how much of each daily quota is already spent.
 *
 * @param onEditTimer invoked with a rule id, or `0` to create a new timer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimersScreen(
    onEditTimer: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TimersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val resources = LocalResources.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.timers_title)) },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptics.tap()
                    onEditTimer(0)
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.timers_add)) },
            )
        },
    ) { innerPadding ->
        if (uiState.items.isEmpty() && !uiState.loading) {
            EmptyState(
                icon = Icons.Outlined.HourglassEmpty,
                title = stringResource(R.string.timers_empty_title),
                body = stringResource(R.string.timers_empty_body),
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.items, key = { it.rule.id }) { item ->
                    val limit = DurationFormat.duration(resources, item.rule.dailyLimitMinutes)
                    val used = DurationFormat.duration(resources, (item.usedMillis / 60_000L).toInt())
                    val subtitle = when {
                        viewModel.isExhausted(item) -> stringResource(R.string.timer_spent)
                        !item.rule.enabled -> stringResource(R.string.rule_disabled)
                        else -> stringResource(R.string.timer_progress, used, limit)
                    }

                    RuleCard(
                        title = item.rule.name,
                        subtitle = subtitle,
                        packageNames = item.rule.packageNames.toList(),
                        enabled = item.rule.enabled,
                        locked = item.locked,
                        onClick = { onEditTimer(item.rule.id) },
                        onEnabledChange = { viewModel.setEnabled(item.rule, it) },
                        content = {
                            LinearProgressIndicator(
                                progress = { item.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                color = if (viewModel.isExhausted(item)) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}
