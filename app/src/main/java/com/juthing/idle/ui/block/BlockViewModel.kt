package com.juthing.idle.ui.block

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juthing.idle.data.system.LocationProvider
import com.juthing.idle.data.system.NfcAvailability
import com.juthing.idle.data.system.NfcTagReader
import com.juthing.idle.domain.model.BlockDecision
import com.juthing.idle.domain.model.BlockReason
import com.juthing.idle.domain.model.UnlockMethod
import com.juthing.idle.domain.repository.InstalledAppsRepository
import com.juthing.idle.domain.repository.UnlockMethodRepository
import com.juthing.idle.domain.usecase.EmergencyUnlockUseCase
import com.juthing.idle.domain.usecase.EvaluateBlockUseCase
import com.juthing.idle.domain.usecase.GrantUnlockUseCase
import com.juthing.idle.domain.usecase.UnlockAttempt
import com.juthing.idle.domain.usecase.UnlockFailure
import com.juthing.idle.domain.usecase.UnlockResult
import com.juthing.idle.domain.usecase.ValidateUnlockUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * What the block screen shows.
 *
 * @property reason `null` while loading, or once the block no longer applies.
 * @property failure the last refused attempt, so the screen can say what went wrong instead of
 *   silently doing nothing.
 * @property dismissed set once the app may be used, which closes the screen.
 * @property unlocked set only when the block was actually lifted by the user, as opposed to
 *   having quietly expired. The screen lingers on a confirmation for the first and not the
 *   second: there is nothing to celebrate about a period that simply ended.
 */
data class BlockUiState(
    val packageName: String = "",
    val appLabel: String = "",
    val reason: BlockReason? = null,
    val method: UnlockMethod? = null,
    val nfcAvailability: NfcAvailability = NfcAvailability.UNSUPPORTED,
    val emergencySecondsLeft: Int = 0,
    val failure: UnlockFailure? = null,
    val checkingPosition: Boolean = false,
    val dismissed: Boolean = false,
    val unlocked: Boolean = false,
    val loading: Boolean = true,
)

/**
 * Drives the block screen and the unlock attempt made from it.
 *
 * The reason is recomputed here rather than carried in the launching intent: a period can end, or
 * a grant can arrive, between the service deciding to block and this screen appearing, and the
 * screen should never insist on a block that no longer holds.
 */
@HiltViewModel
class BlockViewModel @Inject constructor(
    private val evaluateBlock: EvaluateBlockUseCase,
    private val unlockMethodRepository: UnlockMethodRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val validateUnlock: ValidateUnlockUseCase,
    private val grantUnlock: GrantUnlockUseCase,
    private val emergencyUnlock: EmergencyUnlockUseCase,
    private val locationProvider: LocationProvider,
    private val nfcTagReader: NfcTagReader,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlockUiState())
    val uiState: StateFlow<BlockUiState> = _uiState.asStateFlow()

    /**
     * Loads everything the screen needs for [packageName].
     *
     * Ignored once the block has been lifted: the activity re-checks on every resume, and a
     * reload at that moment would wipe the confirmation the user has just earned.
     */
    fun start(packageName: String) {
        if (_uiState.value.unlocked) return
        viewModelScope.launch {
            val decision = evaluateBlock(packageName)
            val reason = (decision as? BlockDecision.Blocked)?.reason
            val method = reason?.let { unlockMethodRepository.get(it.rule.unlockMethodId) }

            _uiState.value = BlockUiState(
                packageName = packageName,
                appLabel = installedAppsRepository.labelFor(packageName),
                reason = reason,
                method = method,
                nfcAvailability = nfcTagReader.availability(),
                emergencySecondsLeft = emergencyUnlock.remainingSeconds(),
                dismissed = reason == null,
                loading = false,
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
        val state = _uiState.value
        if (state.unlocked) return
        val method = state.method ?: return
        val rule = state.reason?.rule ?: return

        viewModelScope.launch {
            when (val result = validateUnlock(method, attempt)) {
                is UnlockResult.Success -> {
                    grantUnlock(ruleId = rule.id, packageName = state.packageName)
                    _uiState.update { it.copy(failure = null, dismissed = true, unlocked = true) }
                }

                is UnlockResult.Failure -> _uiState.update { it.copy(failure = result.reason) }
            }
        }
    }

    /**
     * Spends the daily emergency budget on this app.
     *
     * No method is presented and none is checked: that is the whole point of the escape hatch.
     * What keeps it from being a way around every rule is that it is small, shared by all apps,
     * and cannot be topped up before tomorrow.
     */
    fun useEmergency() {
        val packageName = _uiState.value.packageName
        viewModelScope.launch {
            val granted = emergencyUnlock(packageName)
            if (granted > 0) _uiState.update { it.copy(dismissed = true, unlocked = true) }
        }
    }
}
