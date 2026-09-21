package com.juthing.idle.ui.ruleedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juthing.idle.R
import com.juthing.idle.core.ui.DurationFormat
import com.juthing.idle.ui.apppicker.AppPickerSheet

/** Which sheet, if any, is currently covering the edit screen. */
private enum class OpenSheet { NONE, APPS, METHOD, START_TIME, END_TIME }

/**
 * Creates or edits one period or timer.
 *
 * The same screen serves both shapes: they differ only in the middle section, and splitting them
 * would duplicate the name, apps and unlock method fields for no gain.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleEditScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RuleEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val locale = LocalConfiguration.current.locales[0]
    var openSheet by remember { mutableStateOf(OpenSheet.NONE) }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onClose()
    }

    val title = when {
        uiState.isPeriod && uiState.isNew -> R.string.new_period_title
        uiState.isPeriod -> R.string.edit_period_title
        uiState.isNew -> R.string.new_timer_title
        else -> R.string.edit_timer_title
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (!uiState.isNew && !uiState.locked) {
                        IconButton(onClick = viewModel::delete) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                            )
                        }
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (uiState.locked) {
                Text(
                    text = stringResource(R.string.rule_locked),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::setName,
                label = { Text(stringResource(R.string.rule_name_label)) },
                placeholder = {
                    Text(
                        stringResource(
                            if (uiState.isPeriod) {
                                R.string.rule_name_placeholder_period
                            } else {
                                R.string.rule_name_placeholder_timer
                            },
                        ),
                    )
                },
                singleLine = true,
                enabled = !uiState.locked,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )

            FieldRow(
                label = stringResource(R.string.rule_apps_label),
                value = if (uiState.packageNames.isEmpty()) {
                    stringResource(R.string.rule_apps_none)
                } else {
                    pluralStringResource(
                        R.plurals.app_count,
                        uiState.packageNames.size,
                        uiState.packageNames.size,
                    )
                },
                enabled = !uiState.locked,
                onClick = { openSheet = OpenSheet.APPS },
            )

            if (uiState.isPeriod) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.rule_days_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    DaySelector(
                        selected = uiState.daysOfWeek,
                        onToggle = viewModel::toggleDay,
                        enabled = !uiState.locked,
                        locale = locale,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FieldRow(
                        label = stringResource(R.string.rule_start_label),
                        value = DurationFormat.clock(uiState.startMinute),
                        enabled = !uiState.locked,
                        onClick = { openSheet = OpenSheet.START_TIME },
                        modifier = Modifier.weight(1f),
                    )
                    FieldRow(
                        label = stringResource(R.string.rule_end_label),
                        value = DurationFormat.clock(uiState.endMinute),
                        enabled = !uiState.locked,
                        onClick = { openSheet = OpenSheet.END_TIME },
                        modifier = Modifier.weight(1f),
                    )
                }

                if (uiState.crossesMidnight) {
                    Text(
                        text = stringResource(R.string.rule_crosses_midnight),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.rule_limit_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(
                            R.string.rule_limit_value,
                            DurationFormat.duration(resources, uiState.dailyLimitMinutes),
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    // Steps of five minutes: finer control invites fiddling, coarser is useless.
                    Slider(
                        value = uiState.dailyLimitMinutes.toFloat(),
                        onValueChange = { viewModel.setDailyLimitMinutes(it.toInt()) },
                        valueRange = 5f..480f,
                        steps = (480 - 5) / 5 - 1,
                        enabled = !uiState.locked,
                    )
                }
            }

            FieldRow(
                label = stringResource(R.string.rule_unlock_label),
                value = uiState.availableMethods
                    .firstOrNull { it.id == uiState.unlockMethodId }
                    ?.name
                    ?: stringResource(R.string.rule_unlock_none),
                enabled = !uiState.locked,
                onClick = { openSheet = OpenSheet.METHOD },
            )

            if (uiState.availableMethods.isEmpty() && !uiState.loading) {
                Text(
                    text = stringResource(R.string.rule_unlock_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Button(
                onClick = viewModel::save,
                enabled = uiState.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 32.dp),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }

    when (openSheet) {
        OpenSheet.APPS -> AppPickerSheet(
            apps = uiState.installedApps,
            selected = uiState.packageNames,
            onSelectionChange = viewModel::setPackages,
            onDismiss = { openSheet = OpenSheet.NONE },
        )

        OpenSheet.METHOD -> UnlockMethodSheet(
            methods = uiState.availableMethods,
            selectedId = uiState.unlockMethodId,
            onSelect = viewModel::setUnlockMethod,
            onDismiss = { openSheet = OpenSheet.NONE },
        )

        OpenSheet.START_TIME -> TimePickerSheet(
            initialMinuteOfDay = uiState.startMinute,
            onConfirm = viewModel::setStartMinute,
            onDismiss = { openSheet = OpenSheet.NONE },
        )

        OpenSheet.END_TIME -> TimePickerSheet(
            initialMinuteOfDay = uiState.endMinute,
            onConfirm = viewModel::setEndMinute,
            onDismiss = { openSheet = OpenSheet.NONE },
        )

        OpenSheet.NONE -> Unit
    }
}

/** A labelled value that opens a picker when tapped. */
@Composable
private fun FieldRow(
    label: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AssistChip(
            onClick = onClick,
            enabled = enabled,
            label = { Text(value) },
            modifier = Modifier.align(Alignment.Start),
        )
    }
}
