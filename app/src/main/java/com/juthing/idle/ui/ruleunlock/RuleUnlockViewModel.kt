package com.juthing.idle.ui.ruleunlock

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.juthing.idle.data.system.LocationProvider
import com.juthing.idle.data.system.NfcAvailability
import com.juthing.idle.data.system.NfcTagReader
import com.juthing.idle.domain.model.Rule
import com.juthing.idle.domain.model.UnlockMethod
import com.juthing.idle.domain.repository.RuleRepository
import com.juthing.idle.domain.repository.UnlockMethodRepository
import com.juthing.idle.domain.usecase.GrantUnlockUseCase
import com.juthing.idle.domain.usecase.UnlockAttempt
import com.juthing.idle.domain.usecase.UnlockFailure
import com.juthing.idle.domain.usecase.UnlockResult
import com.juthing.idle.domain.usecase.ValidateUnlockUseCase
import com.juthing.idle.ui.navigation.RuleUnlockRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * What the in-app unlock screen shows.
 *
 * @property unlocked set once the rule is open for editing, which is what closes the screen.
 * @property unlockedMinutes how long the rule stays open, so the screen can say it out loud
 *   rather than leaving the user guessing how long they have.
 */
data class RuleUnlockUiState(
    val loading: Boolean = true,
    val ruleName: String = "",
    val method: UnlockMethod? = null,
    val nfcAvailability: NfcAvailability = NfcAvailability.UNSUPPORTED,
    val failure: UnlockFailure? = null,
    val checkingPosition: Boolean = false,
    val unlocked: Boolean = false,
    val unlockedMinutes: Int = 0,
)

/**
 * Drives lifting the lock on a rule that is currently running, from inside Idle.
 *
 * The same proof as blocking an app: the rule's own method, checked by the same use case. What
 * changes is only what the unlock buys — the right to edit that rule, not the right to use the
 * apps under it.
 */
@HiltViewModel
class RuleUnlockViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val ruleRepository: RuleRepository,
    private val unlockMethodRepository: UnlockMethodRepository,
    private val validateUnlock: ValidateUnlockUseCase,
    private val grantUnlock: GrantUnlockUseCase,
    private val locationProvider: LocationProvider,
    private val nfcTagReader: NfcTagReader,
) : ViewModel() {

    private val route: RuleUnlockRoute = savedStateHandle.toRoute()

    private val _uiState = MutableStateFlow(RuleUnlockUiState())
    val uiState: StateFlow<RuleUnlockUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val rule: Rule? = ruleRepository.getRule(route.ruleId)
            val method = rule?.let { unlockMethodRepository.get(it.unlockMethodId) }
            _uiState.value = RuleUnlockUiState(
                loading = false,
                ruleName = rule?.name.orEmpty(),
                method = method,
                nfcAvailability = nfcTagReader.availability(),
            )
        }
    }

    /** Checks a scanned code against the rule's method. */
    fun submitScan(payload: String) = attempt(UnlockAttempt.Scan(payload))

    /** Checks a tapped tag against the rule's method. */
    fun submitTag(tagId: String) = attempt(UnlockAttempt.Tag(tagId))

    /** Reads the current position and checks it against the rule's area. */
    fun submitPosition() {
        _uiState.update { it.copy(checkingPosition = true, failure = null) }
        viewModelScope.launch {
            val fix = locationProvider.currentPosition()
            if (fix == null) {
                _uiState.update {
                    it.copy(checkingPosition = false, failure = UnlockFailure.OUT_OF_AREA)
                }
                return@launch
            }
            _uiState.update { it.copy(checkingPosition = false) }
            attempt(
                UnlockAttempt.Position(
                    latitude = fix.latitude,
                    longitude = fix.longitude,
                    accuracyMeters = fix.accuracyMeters,
                ),
            )
        }
    }

    private fun attempt(attempt: UnlockAttempt) {
        val method = _uiState.value.method ?: return
        if (_uiState.value.unlocked) return

        viewModelScope.launch {
            when (val result = validateUnlock(method, attempt)) {
                is UnlockResult.Success -> {
                    val millis = grantUnlock.forEditing(route.ruleId)
                    _uiState.update {
                        it.copy(
                            failure = null,
                            unlocked = true,
                            unlockedMinutes = (millis / 60_000L).toInt(),
                        )
                    }
                }

                is UnlockResult.Failure -> _uiState.update { it.copy(failure = result.reason) }
            }
        }
    }
}
