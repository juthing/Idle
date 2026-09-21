package com.juthing.idle.domain.model

/** How a user proves they really mean to unblock an app. */
enum class UnlockMethodType {
    /** A QR code or barcode the user has to scan again. */
    QR,

    /** An NFC tag the user has to tap again. */
    NFC,

    /** A circular area the user has to physically be inside of. */
    LOCATION,
}

/**
 * A way to lift a block, created and named by the user ("QR on the fridge").
 *
 * For [UnlockMethodType.QR] and [UnlockMethodType.NFC] only [secretHash] is stored: a SHA-256 of
 * the scanned payload. Reading the database therefore never reveals the code itself.
 * For [UnlockMethodType.LOCATION] the area is stored in the clear, since it is the area itself
 * that has to be reached.
 */
data class UnlockMethod(
    val id: Long = 0,
    val name: String,
    val type: UnlockMethodType,
    val secretHash: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Int? = null,
    val createdAt: Long = 0,
)
