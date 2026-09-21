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
 * @property anchorLatitude where the device actually is, kept apart from the chosen point so the
 *   picker can draw one relative to the other.
 * @property savedMethodId set once the method is written; the screen hands it back to whoever
 *   opened it and closes.
 */
data class CreateMethodUiState(
    val type: UnlockMethodType? = null,
    val name: String = "",
    val capturedSecret: String? = null,
    val anchorLatitude: Double? = null,
    val anchorLongitude: Double? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Int = DEFAULT_RADIUS_METERS,
    val locating: Boolean = false,
    val locationFailed: Boolean = false,
    val nfcAvailability: NfcAvailability = NfcAvailability.UNSUPPORTED,
    val savedMethodId: Long? = null,
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

    /** Goes back to the list of kinds, so a wrong turn costs one tap rather than a whole screen. */
    fun clearType() = _uiState.update { CreateMethodUiState() }

    fun setName(name: String) = _uiState.update { it.copy(name = name) }

    fun setRadius(meters: Int) = _uiState.update { it.copy(radiusMeters = meters) }

    /** Records a scanned code or a tapped tag, hashed immediately. */
    fun capture(payload: String) = _uiState.update {
        if (it.capturedSecret != null) it else it.copy(capturedSecret = SecretHasher.hash(payload))
    }

    /** Clears a capture so the user can present something else without starting over. */
    fun recapture() = _uiState.update { it.copy(capturedSecret = null) }

    /**
     * Reads the device position once and uses it as both the reference point and the first guess.
     *
     * The reference never moves afterwards: it is where the phone was standing, and the picker
     * draws every chosen point as a distance and a bearing from it.
     */
    fun captureCurrentPlace() {
        _uiState.update { it.copy(locating = true, locationFailed = false) }
        viewModelScope.launch {
            val fix = locationProvider.currentPosition()
            _uiState.update { state ->
                if (fix == null) {
                    state.copy(locating = false, locationFailed = true)
                } else {
                    state.copy(
                        locating = false,
                        locationFailed = false,
                        anchorLatitude = fix.latitude,
                        anchorLongitude = fix.longitude,
                        latitude = state.latitude ?: fix.latitude,
                        longitude = state.longitude ?: fix.longitude,
                    )
                }
            }
        }
    }

    /** Moves the centre of the area to a point the user picked by hand. */
    fun moveTo(latitude: Double, longitude: Double) = _uiState.update {
        it.copy(latitude = latitude, longitude = longitude)
    }

    /** Puts the chosen point back on the device's own position. */
    fun recentre() = _uiState.update {
        it.copy(latitude = it.anchorLatitude, longitude = it.anchorLongitude)
    }

    fun save() {
        val state = _uiState.value
        val type = state.type
        if (!state.canSave || type == null || state.savedMethodId != null) return

        viewModelScope.launch {
            val id = repository.save(
                UnlockMethod(
                    name = state.name.trim(),
                    type = type,
                    secretHash = state.capturedSecret,
                    latitude = state.latitude,
                    longitude = state.longitude,
                    radiusMeters = state.radiusMeters.takeIf { type == UnlockMethodType.LOCATION },
                ),
            )
            _uiState.update { it.copy(savedMethodId = id) }
        }
    }
}
