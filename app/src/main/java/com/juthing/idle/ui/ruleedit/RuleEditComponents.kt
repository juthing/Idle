package com.juthing.idle.ui.ruleedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.juthing.idle.R
import com.juthing.idle.core.ui.DurationFormat
import com.juthing.idle.core.ui.components.icon
import com.juthing.idle.core.ui.components.labelRes
import com.juthing.idle.core.ui.step
import com.juthing.idle.core.ui.tap
import com.juthing.idle.core.ui.toggle
import com.juthing.idle.domain.model.UnlockMethod
import java.time.DayOfWeek
import java.util.Locale

/**
 * A row of day toggles, laid out Monday first regardless of locale conventions.
 *
 * Each chip is given an equal share of the row and its letter is centred inside that share. A
 * chip's label is normally laid out from the start edge, which on a stretched chip left every
 * letter hugging the left of its own box and made the week look misaligned.
 */
@Composable
fun DaySelector(
    selected: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit,
    enabled: Boolean,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DayOfWeek.entries.forEach { day ->
            val isSelected = day in selected
            FilterChip(
                selected = isSelected,
                onClick = {
                    haptics.toggle(!isSelected)
                    onToggle(day)
                },
                enabled = enabled,
                label = {
                    Text(
                        text = DurationFormat.dayInitial(day, locale),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** The daily budgets most people actually pick, so the common case is one tap. */
private val LIMIT_PRESETS = listOf(15, 30, 60, 120, 180)

/** One nudge of the fine control. Five minutes is small enough to matter, large enough to reach. */
private const val LIMIT_STEP_MINUTES = 5

private const val LIMIT_MIN_MINUTES = 5
private const val LIMIT_MAX_MINUTES = 8 * 60

/**
 * Picks how long a timer allows per day.
 *
 * A bare slider made every value equally hard to hit: landing on exactly an hour meant nudging a
 * thumb across a 475-minute range. The presets cover what people actually choose, and the two
 * buttons move five minutes at a time when the answer is not on the list — so the common case is
 * one tap and the uncommon one is still exact.
 */
@Composable
fun DailyLimitPicker(
    minutes: Int,
    onChange: (Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FilledTonalIconButton(
                onClick = {
                    haptics.step()
                    onChange(minutes - LIMIT_STEP_MINUTES)
                },
                enabled = enabled && minutes > LIMIT_MIN_MINUTES,
            ) {
                // A minus sign rather than an icon: the glyph is unambiguous at any size, and the
                // icon set has no minus that matches the plus.
                Text(text = "−", style = MaterialTheme.typography.titleLarge)
            }

            Text(
                text = DurationFormat.duration(resources, minutes),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )

            FilledTonalIconButton(
                onClick = {
                    haptics.step()
                    onChange(minutes + LIMIT_STEP_MINUTES)
                },
                enabled = enabled && minutes < LIMIT_MAX_MINUTES,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LIMIT_PRESETS.forEach { preset ->
                FilterChip(
                    selected = minutes == preset,
                    onClick = {
                        haptics.tap()
                        onChange(preset)
                    },
                    enabled = enabled,
                    label = {
                        Text(
                            text = DurationFormat.duration(resources, preset),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Picks the method that will be required to lift the rule.
 *
 * Offered as a sheet over the edit screen so the draft stays untouched, and it can create a
 * method as well as choose one: being told to go to Settings, find the right page and come back
 * was a dead end in the middle of writing a rule.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockMethodSheet(
    methods: List<UnlockMethod>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current

    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Text(
            text = stringResource(R.string.rule_unlock_label),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
        )

        if (methods.isEmpty()) {
            Text(
                text = stringResource(R.string.rule_unlock_none_yet),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.padding(top = 4.dp)) {
                items(methods, key = { it.id }) { method ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = method.id == selectedId,
                                onClick = {
                                    haptics.tap()
                                    onSelect(method.id)
                                    onDismiss()
                                },
                            )
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        RadioButton(selected = method.id == selectedId, onClick = null)
                        Icon(
                            imageVector = method.type.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column {
                            Text(text = method.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = stringResource(method.type.labelRes()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = false,
                    onClick = {
                        haptics.tap()
                        onCreate()
                    },
                )
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.unlock_methods_add),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * A Material 3 time picker in a sheet.
 *
 * Expanded from the outset and scrollable: at its partial height the sheet cut the clock face in
 * half, leaving the user to drag the sheet up before they could reach the hours. The choice is
 * also confirmed with a button rather than on dismissal, so swiping the sheet away is a way of
 * changing one's mind instead of an accidental commitment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerSheet(
    initialMinuteOfDay: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val state: TimePickerState = rememberTimePickerState(
        initialHour = initialMinuteOfDay / 60,
        initialMinute = initialMinuteOfDay % 60,
        is24Hour = true,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.padding(vertical = 8.dp)) {
                TimePicker(state = state)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
                TextButton(
                    onClick = {
                        haptics.tap()
                        onConfirm(state.hour * 60 + state.minute)
                        onDismiss()
                    },
                ) {
                    Text(stringResource(R.string.action_done))
                }
            }
        }
    }
}