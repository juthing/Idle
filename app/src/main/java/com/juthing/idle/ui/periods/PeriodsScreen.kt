package com.juthing.idle.ui.periods

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalConfiguration
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
 * Lists the periods the user has set up.
 *
 * @param onEditPeriod invoked with a rule id, or `0` to create a new period.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodsScreen(
    onEditPeriod: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PeriodsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val resources = LocalResources.current
    val locale = LocalConfiguration.current.locales[0]

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.periods_title)) },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptics.tap()
                    onEditPeriod(0)
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.periods_add)) },
            )
        },
    ) { innerPadding ->
        if (uiState.items.isEmpty() && !uiState.loading) {
            EmptyState(
                icon = Icons.Outlined.Schedule,
                title = stringResource(R.string.periods_empty_title),
                body = stringResource(R.string.periods_empty_body),
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.items, key = { it.rule.id }) { item ->
                    val subtitle = buildString {
                        append(DurationFormat.range(resources, item.rule.startMinute, item.rule.endMinute))
                        append(" · ")
                        append(DurationFormat.days(resources, item.rule.daysOfWeek, locale))
                        if (!item.rule.enabled) {
                            append(" · ")
                            append(resources.getString(R.string.rule_disabled))
                        }
                    }
                    RuleCard(
                        title = item.rule.name,
                        subtitle = subtitle,
                        packageNames = item.rule.packageNames.toList(),
                        enabled = item.rule.enabled,
                        locked = item.locked,
                        onClick = { onEditPeriod(item.rule.id) },
                        onEnabledChange = { viewModel.setEnabled(item.rule, it) },
                    )
                }
            }
        }
    }
}
