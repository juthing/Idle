package com.juthing.idle.ui.ruleedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.juthing.idle.R
import com.juthing.idle.core.ui.DurationFormat
import com.juthing.idle.domain.model.UnlockMethod
import java.time.DayOfWeek
import java.util.Locale

/** A row of day toggles, laid out Monday first regardless of locale conventions. */
@Composable
fun DaySelector(
    selected: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit,
    enabled: Boolean,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DayOfWeek.entries.forEach { day ->
            FilterChip(
                selected = day in selected,
                onClick = { onToggle(day) },
                enabled = enabled,
                label = { Text(DurationFormat.dayInitial(day, locale)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Picks the method that will be required to lift the rule.
 *
 * Offered as a sheet over the edit screen so the draft stays untouched. When the user has no
 * method yet the sheet says so plainly rather than offering an empty list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockMethodSheet(
    methods: List<UnlockMethod>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Text(
            text = stringResource(R.string.rule_unlock_label),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        if (methods.isEmpty()) {
            Text(
                text = stringResource(R.string.rule_unlock_required),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)) {
                items(methods, key = { it.id }) { method ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = method.id == selectedId,
                                onClick = {
                                    onSelect(method.id)
                                    onDismiss()
                                },
                            )
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        RadioButton(selected = method.id == selectedId, onClick = null)
                        Column {
                            Text(text = method.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = method.type.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A Material 3 time picker in a sheet, reporting the chosen minute of day on dismissal. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerSheet(
    initialMinuteOfDay: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state: TimePickerState = rememberTimePickerState(
        initialHour = initialMinuteOfDay / 60,
        initialMinute = initialMinuteOfDay % 60,
        is24Hour = true,
    )

    ModalBottomSheet(
        onDismissRequest = {
            onConfirm(state.hour * 60 + state.minute)
            onDismiss()
        },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TimePicker(state = state)
        }
    }
}
