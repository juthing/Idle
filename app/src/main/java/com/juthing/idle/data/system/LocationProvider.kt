package com.juthing.idle.data.system

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** A position fix, with the accuracy the provider reported for it. */
data class PositionFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
)

/**
 * Reads the device position through the framework's [LocationManager].
 *
 * The platform API rather than the Play Services fused provider, for the same reason as ZXing:
 * Idle stays usable on a phone without Google services.
 *
 * Positions are only ever read on demand — when registering a place, and when unlocking. Idle
 * never subscribes to location updates and never registers a geofence, so it cannot follow the
 * user around in the background.
 */
@Singleton
class LocationProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val locationManager: LocationManager? get() = context.getSystemService()

    /** Whether the user has granted a location permission precise enough to be useful. */
    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Asks for one fresh position.
     *
     * @return the fix, or `null` when no provider could produce one before the caller gave up.
     */
    @SuppressLint("MissingPermission")
    suspend fun currentPosition(): PositionFix? {
        if (!hasPermission()) return null
        val manager = locationManager ?: return null
        val provider = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstOrNull(manager::isProviderEnabled)
            ?: return null

        return suspendCancellableCoroutine { continuation ->
            val cancellationSignal = CancellationSignal()
            continuation.invokeOnCancellation { cancellationSignal.cancel() }

            manager.getCurrentLocation(
                provider,
                cancellationSignal,
                context.mainExecutor,
            ) { location: Location? ->
                continuation.resume(
                    location?.let {
                        PositionFix(
                            latitude = it.latitude,
                            longitude = it.longitude,
                            accuracyMeters = it.accuracy,
                        )
                    },
                )
            }
        }
    }
}
