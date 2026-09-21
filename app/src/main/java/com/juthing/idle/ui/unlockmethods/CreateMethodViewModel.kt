package com.juthing.idle.ui.unlockmethods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juthing.idle.core.crypto.SecretHasher
import com.juthing.idle.data.system.LocationProvider
import com.juthing.idle.data.system.NfcAvailability
import com.juthing.idle.data.system.NfcTagReader
import com.juthing.idle.domain.model.UnlockMethod
import com.juthing.idle.domain.model.UnlockMethodType
import com.juthing.idle.domain.repository.UnlockMethodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The default radius of a registered place, wide enough to cover a home or an office floor. */
private const val DEFAULT_RADIUS_METERS = 100

/**
 * The method being created.
 *
 * @property capturedSecret the hash of what was scanned or tapped. Only the hash is ever held,
 *   even in memory, so the payload cannot leak through a state dump.
 * @property saved flipped once the method is written, which tells the screen to close.
 */
data class CreateMethodUiState(
    val type: UnlockMethodType? = null,
    val name: String = "",
    val capturedSecret: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Int = DEFAULT_RADIUS_METERS,
    val locating: Boolean = false,
    val nfcAvailability: NfcAvailability = NfcAvailability.UNSUPPORTED,
    val saved: Boolean = false,
) {
    /** A method is complete once it has a name and something to check an attempt against. */
    val canSave: Boolean
        get() = name.isNotBlank() && when (type) {
            UnlockMethodType.QR, UnlockMethodType.NFC -> capturedSecret != null
            UnlockMethodType.LOCATION -> latitude != null && longitude != null
            null -> false
        }
}

/** Drives the screen that registers a new unlock method. */
@HiltViewModel
class CreateMethodViewModel @Inject constructor(
    private val repository: UnlockMethodRepository,
    private val locationProvider: LocationProvider,
    private val nfcTagReader: NfcTagReader,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateMethodUiState())
    val uiState: StateFlow<CreateMethodUiState> = _uiState.asStateFlow()

    fun selectType(type: UnlockMethodType) = _uiState.update {
        it.copy(type = type, nfcAvailability = nfcTagReader.availability())
    }

    fun setName(name: String) = _uiState.update { it.copy(name = name) }

    fun setRadius(meters: Int) = _uiState.update { it.copy(radiusMeters = meters) }

    /** Records a scanned code or a tapped tag, hashed immediately. */
    fun capture(payload: String) = _uiState.update {
        it.copy(capturedSecret = SecretHasher.hash(payload))
    }

    /** Reads the device position once and keeps it as the centre of the area. */
    fun captureCurrentPlace() {
        _uiState.update { it.copy(locating = true) }
        viewModelScope.launch {
            val fix = locationProvider.currentPosition()
            _uiState.update { state ->
                state.copy(
                    locating = false,
                    latitude = fix?.latitude ?: state.latitude,
                    longitude = fix?.longitude ?: state.longitude,
                )
            }
        }
    }

    fun save() {
        val state = _uiState.value
        val type = state.type
        if (!state.canSave || type == null) return

        viewModelScope.launch {
            repository.save(
                UnlockMethod(
                    name = state.name.trim(),
                    type = type,
                    secretHash = state.capturedSecret,
                    latitude = state.latitude,
                    longitude = state.longitude,
                    radiusMeters = state.radiusMeters.takeIf { type == UnlockMethodType.LOCATION },
                ),
            )
            _uiState.update { it.copy(saved = true) }
        }
    }
}
