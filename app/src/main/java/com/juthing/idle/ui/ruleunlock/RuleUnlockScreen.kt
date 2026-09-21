package com.juthing.idle.ui.ruleunlock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juthing.idle.R
import com.juthing.idle.core.ui.components.BackButton
import com.juthing.idle.core.ui.components.CaptureState
import com.juthing.idle.core.ui.components.UnlockFailureMessage
import com.juthing.idle.core.ui.components.UnlockPanel
import com.juthing.idle.core.ui.messageRes
import com.juthing.idle.data.system.NfcTagReader
import kotlinx.coroutines.delay

/**
 * Lifts the lock on a running rule so it can be changed.
 *
 * The rule's own method is what opens it — the friction the user chose stays exactly where it
 * was. What this screen removes is the dead end: being told a rule is locked and having no way
 * from there to unlock it, only the instruction to go and open one of its apps first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleUnlockScreen(
    nfcTagReader: NfcTagReader,
    onDone: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RuleUnlockViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // A short pause before leaving: the confirmation is the reward for the gesture, and closing
    // the screen the instant it appears would take it away again.
    LaunchedEffect(uiState.unlocked) {
        if (uiState.unlocked) {
            delay(900)
            onDone()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rule_unlock_to_edit)) },
                navigationIcon = { BackButton(onClose) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = uiState.ruleName,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )

            val method = uiState.method
            when {
                uiState.loading -> Unit

                method == null -> Text(
                    text = stringResource(R.string.block_method_broken),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )

                else -> {
                    UnlockPanel(
                        method = method,
                        nfcTagReader = nfcTagReader,
                        nfcAvailability = uiState.nfcAvailability,
                        state = if (uiState.unlocked) {
                            CaptureState.DONE
                        } else if (uiState.failure != null) {
                            CaptureState.FAILED
                        } else {
                            CaptureState.WAITING
                        },
                        checkingPosition = uiState.checkingPosition,
                        onScan = viewModel::submitScan,
                        onTag = viewModel::submitTag,
                        onCheckPosition = viewModel::submitPosition,
                    )

                    uiState.failure?.let { failure ->
                        UnlockFailureMessage(stringResource(failure.messageRes()))
                    }

                    if (uiState.unlocked) {
                        Text(
                            text = stringResource(
                                R.string.rule_unlocked_for,
                                uiState.unlockedMinutes,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
