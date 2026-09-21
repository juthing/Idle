package com.juthing.idle.ui.unlockmethods

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juthing.idle.R
import com.juthing.idle.core.ui.components.EmptyState
import com.juthing.idle.domain.model.UnlockMethodType

/** Lists the unlock methods the user has registered, and lets them add or remove one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockMethodsScreen(
    onBack: () -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UnlockMethodsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.unlock_methods_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreate,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.unlock_methods_add)) },
            )
        },
    ) { innerPadding ->
        if (uiState.items.isEmpty() && !uiState.loading) {
            EmptyState(
                icon = Icons.Outlined.Key,
                title = stringResource(R.string.unlock_methods_empty_title),
                body = stringResource(R.string.unlock_methods_empty_body),
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.items, key = { it.method.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.method.name,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = stringResource(item.method.type.labelRes()),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (item.usedByRules > 0) {
                                    Text(
                                        text = stringResource(R.string.unlock_method_in_use),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                            }
                            // A method guarding a rule stays: deleting it would disarm the rule.
                            if (item.usedByRules == 0) {
                                IconButton(onClick = { viewModel.delete(item) }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = stringResource(R.string.action_delete),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The localised label of a method type. */
internal fun UnlockMethodType.labelRes(): Int = when (this) {
    UnlockMethodType.QR -> R.string.unlock_type_qr
    UnlockMethodType.NFC -> R.string.unlock_type_nfc
    UnlockMethodType.LOCATION -> R.string.unlock_type_location
}

/** The one-line explanation of a method type, shown when choosing one. */
internal fun UnlockMethodType.summaryRes(): Int = when (this) {
    UnlockMethodType.QR -> R.string.unlock_type_qr_summary
    UnlockMethodType.NFC -> R.string.unlock_type_nfc_summary
    UnlockMethodType.LOCATION -> R.string.unlock_type_location_summary
}
