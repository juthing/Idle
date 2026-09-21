package com.juthing.idle.ui.ruleedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juthing.idle.R
import com.juthing.idle.core.ui.DurationFormat
import com.juthing.idle.core.ui.components.BackButton
import com.juthing.idle.core.ui.components.SettingsGroup
import com.juthing.idle.core.ui.components.SettingsRow
import com.juthing.idle.core.ui.components.icon
import com.juthing.idle.core.ui.confirm
import com.juthing.idle.core.ui.tap
import com.juthing.idle.ui.apppicker.AppPickerSheet

/** Which sheet, if any, is currently covering the edit screen. */
private enum class OpenSheet { NONE, APPS, METHOD, START_TIME, END_TIME }

/**
 * Creates or edits one period or timer.
 *
 * The same screen serves both shapes: they differ only in the middle section, and splitting them
 * would duplicate the name, apps and unlock method fields for no gain.
 *
 * @param createdMethodId a method registered from the picker sheet and handed back through
 *   navigation, adopted once and then cleared.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleEditScreen(
    createdMethodId: Long?,
    onCreatedMethodConsumed: () -> Unit,
    onCreateMethod: () -> Unit,
    onUnlock: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RuleEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    val haptics = LocalHapticFeedback.current
    var openSheet by remember { mutableStateOf(OpenSheet.NONE) }
    var confirmingDelete by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            haptics.confirm()
            onClose()
        }
    }

    LaunchedEffect(createdMethodId) {
        createdMethodId?.let {
            viewModel.adoptMethod(it)
            onCreatedMethodConsumed()
        }
    }

    // Coming back from the unlock screen is how this screen learns it may now be edited.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshLock()
        onPauseOrDispose { }
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
                navigationIcon = { BackButton(onClose) },
                actions = {
                    if (!uiState.isNew && !uiState.locked) {
                        IconButton(onClick = { confirmingDelete = true }) {
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
                .padding(top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (uiState.locked) {
                LockedBanner(onUnlock = { onUnlock(uiState.ruleId) })
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
                    .padding(horizontal = 16.dp),
            )

            SettingsGroup {
                SettingsRow(
                    icon = Icons.Outlined.Apps,
                    title = stringResource(R.string.rule_apps_label),
                    summary = if (uiState.packageNames.isEmpty()) {
                        stringResource(R.string.rule_apps_none)
                    } else {
                        pluralStringResource(
                            R.plurals.app_count,
                            uiState.packageNames.size,
                            uiState.packageNames.size,
                        )
                    },
                    onClick = if (uiState.locked) null else ({ openSheet = OpenSheet.APPS }),
                )
                SettingsRow(
                    icon = uiState.selectedMethod?.type?.icon ?: Icons.Outlined.Lock,
                    title = stringResource(R.string.rule_unlock_label),
                    summary = uiState.selectedMethod?.name
                        ?: stringResource(R.string.rule_unlock_none),
                    iconTint = if (uiState.selectedMethod == null && !uiState.loading) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    onClick = if (uiState.locked) null else ({ openSheet = OpenSheet.METHOD }),
                )
            }

            if (uiState.isPeriod) {
                PeriodSection(
                    uiState = uiState,
                    locale = locale,
                    onToggleDay = viewModel::toggleDay,
                    onEditStart = { openSheet = OpenSheet.START_TIME },
                    onEditEnd = { openSheet = OpenSheet.END_TIME },
                )
            } else {
                SettingsGroup(title = stringResource(R.string.rule_limit_label)) {
                    DailyLimitPicker(
                        minutes = uiState.dailyLimitMinutes,
                        onChange = viewModel::setDailyLimitMinutes,
                        enabled = !uiState.locked,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }

            Button(
                onClick = viewModel::save,
                enabled = uiState.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
            onCreate = {
                openSheet = OpenSheet.NONE
                onCreateMethod()
            },
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

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            title = { Text(stringResource(R.string.rule_delete_title)) },
            text = { Text(stringResource(R.string.rule_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingDelete = false
                        viewModel.delete()
                    },
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/** The days and hours a period covers. */
@Composable
private fun PeriodSection(
    uiState: RuleEditUiState,
    locale: java.util.Locale,
    onToggleDay: (java.time.DayOfWeek) -> Unit,
    onEditStart: () -> Unit,
    onEditEnd: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current

    SettingsGroup(title = stringResource(R.string.rule_when_label)) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            DaySelector(
                selected = uiState.daysOfWeek,
                onToggle = onToggleDay,
                enabled = !uiState.locked,
                locale = locale,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeField(
                    label = stringResource(R.string.rule_start_label),
                    value = DurationFormat.clock(uiState.startMinute),
                    enabled = !uiState.locked,
                    onClick = {
                        haptics.tap()
                        onEditStart()
                    },
                    modifier = Modifier.weight(1f),
                )
                TimeField(
                    label = stringResource(R.string.rule_end_label),
                    value = DurationFormat.clock(uiState.endMinute),
                    enabled = !uiState.locked,
                    onClick = {
                        haptics.tap()
                        onEditEnd()
                    },
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
        }
    }
}

/** One end of a period's range, big enough to read at a glance and to hit without aiming. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeField(
    label: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Says the rule is sealed, and offers the one way through.
 *
 * Being told a rule cannot be changed without being told how to change it is what makes a blocker
 * feel like a trap rather than a decision the user made. The way through is still the method they
 * chose — the friction is intact — it is simply reachable from here.
 */
@Composable
private fun LockedBanner(onUnlock: () -> Unit) {
    val haptics = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = stringResource(R.string.rule_locked_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                text = stringResource(R.string.rule_locked),
                style = MaterialTheme.typography.bodyMedium,
            )
            FilledTonalButton(
                onClick = {
                    haptics.tap()
                    onUnlock()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.LockOpen, contentDescription = null)
                Text(
                    text = stringResource(R.string.rule_unlock_to_edit),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
