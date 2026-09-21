package com.juthing.idle.domain.usecase

import com.juthing.idle.core.crypto.SecretHasher
import com.juthing.idle.domain.model.UnlockMethod
import com.juthing.idle.domain.model.UnlockMethodType
import javax.inject.Inject
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** What the user presented in order to lift a block. */
sealed interface UnlockAttempt {

    /** The payload decoded from a scanned QR code or barcode. */
    data class Scan(val payload: String) : UnlockAttempt

    /** The identifier read from an NFC tag. */
    data class Tag(val tagId: String) : UnlockAttempt

    /** The device's current position, with the accuracy reported by the location provider. */
    data class Position(
        val latitude: Double,
        val longitude: Double,
        val accuracyMeters: Float,
    ) : UnlockAttempt
}

/** Why an attempt was refused, so the UI can say something more useful than "wrong". */
enum class UnlockFailure {
    /** The scan or tag does not match the registered one. */
    MISMATCH,

    /** The device is outside the registered area. */
    OUT_OF_AREA,

    /** The attempt is of a different kind than the method expects. */
    WRONG_KIND,

    /** The method is missing the data it needs, which means the database is inconsistent. */
    METHOD_INCOMPLETE,
}

/** The outcome of an unlock attempt. */
sealed interface UnlockResult {
    data object Success : UnlockResult
    data class Failure(val reason: UnlockFailure) : UnlockResult
}

/** Earth's mean radius in metres, used by the great-circle distance below. */
private const val EARTH_RADIUS_METERS = 6_371_000.0

/**
 * Checks an unlock attempt against the method that guards a rule.
 *
 * This is the one place that decides whether a block is lifted, so it stays free of Android types
 * and is covered directly by unit tests.
 */
class ValidateUnlockUseCase @Inject constructor() {

    operator fun invoke(method: UnlockMethod, attempt: UnlockAttempt): UnlockResult =
        when (method.type) {
            UnlockMethodType.QR -> validateSecret(method, (attempt as? UnlockAttempt.Scan)?.payload)
            UnlockMethodType.NFC -> validateSecret(method, (attempt as? UnlockAttempt.Tag)?.tagId)
            UnlockMethodType.LOCATION -> validatePosition(method, attempt as? UnlockAttempt.Position)
        }

    private fun validateSecret(method: UnlockMethod, presented: String?): UnlockResult {
        if (presented == null) return UnlockResult.Failure(UnlockFailure.WRONG_KIND)
        val expected = method.secretHash
            ?: return UnlockResult.Failure(UnlockFailure.METHOD_INCOMPLETE)
        return if (SecretHasher.matches(presented, expected)) {
            UnlockResult.Success
        } else {
            UnlockResult.Failure(UnlockFailure.MISMATCH)
        }
    }

    /**
     * Accepts a position when it is inside the registered circle.
     *
     * The reported accuracy is added to the radius rather than ignored: refusing someone standing
     * in their own kitchen because GPS is 30 m off would make the method unusable indoors, which
     * is exactly where people put their unlock spots.
     */
    private fun validatePosition(
        method: UnlockMethod,
        attempt: UnlockAttempt.Position?,
    ): UnlockResult {
        if (attempt == null) return UnlockResult.Failure(UnlockFailure.WRONG_KIND)
        val lat = method.latitude
        val lon = method.longitude
        val radius = method.radiusMeters
        if (lat == null || lon == null || radius == null) {
            return UnlockResult.Failure(UnlockFailure.METHOD_INCOMPLETE)
        }

        val distance = distanceMeters(lat, lon, attempt.latitude, attempt.longitude)
        return if (distance <= radius + attempt.accuracyMeters) {
            UnlockResult.Success
        } else {
            UnlockResult.Failure(UnlockFailure.OUT_OF_AREA)
        }
    }

    /** Great-circle distance between two points, in metres. */
    internal fun distanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(a).coerceAtMost(1.0))
    }
}
