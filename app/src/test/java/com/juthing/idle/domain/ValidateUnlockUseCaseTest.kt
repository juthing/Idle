package com.juthing.idle.domain

import com.google.common.truth.Truth.assertThat
import com.juthing.idle.core.crypto.SecretHasher
import com.juthing.idle.domain.model.UnlockMethod
import com.juthing.idle.domain.model.UnlockMethodType
import com.juthing.idle.domain.usecase.UnlockAttempt
import com.juthing.idle.domain.usecase.UnlockFailure
import com.juthing.idle.domain.usecase.UnlockResult
import com.juthing.idle.domain.usecase.ValidateUnlockUseCase
import org.junit.Test

class ValidateUnlockUseCaseTest {

    private val validate = ValidateUnlockUseCase()

    private val fridgeQr = UnlockMethod(
        id = 1,
        name = "QR on the fridge",
        type = UnlockMethodType.QR,
        secretHash = SecretHasher.hash("idle-fridge-7f3a"),
    )

    private val deskTag = UnlockMethod(
        id = 2,
        name = "Desk tag",
        type = UnlockMethodType.NFC,
        secretHash = SecretHasher.hash("04:A2:19:B7"),
    )

    // The office, with a 100 m radius.
    private val office = UnlockMethod(
        id = 3,
        name = "Office",
        type = UnlockMethodType.LOCATION,
        latitude = 48.8566,
        longitude = 2.3522,
        radiusMeters = 100,
    )

    @Test
    fun `scanning the registered code unlocks`() {
        val result = validate(fridgeQr, UnlockAttempt.Scan("idle-fridge-7f3a"))

        assertThat(result).isEqualTo(UnlockResult.Success)
    }

    @Test
    fun `scanning a different code is refused`() {
        val result = validate(fridgeQr, UnlockAttempt.Scan("idle-fridge-7f3b"))

        assertThat(result).isEqualTo(UnlockResult.Failure(UnlockFailure.MISMATCH))
    }

    @Test
    fun `presenting a tag to a QR method is the wrong kind of attempt`() {
        val result = validate(fridgeQr, UnlockAttempt.Tag("04:A2:19:B7"))

        assertThat(result).isEqualTo(UnlockResult.Failure(UnlockFailure.WRONG_KIND))
    }

    @Test
    fun `tapping the registered tag unlocks`() {
        val result = validate(deskTag, UnlockAttempt.Tag("04:A2:19:B7"))

        assertThat(result).isEqualTo(UnlockResult.Success)
    }

    @Test
    fun `a method missing its secret is reported as incomplete rather than matching`() {
        val broken = fridgeQr.copy(secretHash = null)

        val result = validate(broken, UnlockAttempt.Scan("anything"))

        assertThat(result).isEqualTo(UnlockResult.Failure(UnlockFailure.METHOD_INCOMPLETE))
    }

    @Test
    fun `standing at the registered spot unlocks`() {
        val result = validate(
            office,
            UnlockAttempt.Position(latitude = 48.8566, longitude = 2.3522, accuracyMeters = 5f),
        )

        assertThat(result).isEqualTo(UnlockResult.Success)
    }

    @Test
    fun `standing well outside the area is refused`() {
        // Roughly 5 km away.
        val result = validate(
            office,
            UnlockAttempt.Position(latitude = 48.9016, longitude = 2.3522, accuracyMeters = 5f),
        )

        assertThat(result).isEqualTo(UnlockResult.Failure(UnlockFailure.OUT_OF_AREA))
    }

    @Test
    fun `a poor location fix widens the area instead of locking the user out`() {
        // 150 m from the centre of a 100 m area, with a 100 m accuracy reading.
        val result = validate(
            office,
            UnlockAttempt.Position(latitude = 48.85795, longitude = 2.3522, accuracyMeters = 100f),
        )

        assertThat(result).isEqualTo(UnlockResult.Success)
    }

    @Test
    fun `a location method without coordinates is reported as incomplete`() {
        val broken = office.copy(latitude = null)

        val result = validate(
            broken,
            UnlockAttempt.Position(latitude = 48.8566, longitude = 2.3522, accuracyMeters = 5f),
        )

        assertThat(result).isEqualTo(UnlockResult.Failure(UnlockFailure.METHOD_INCOMPLETE))
    }

    @Test
    fun `distance between two known points is right to within a metre`() {
        // One degree of latitude is about 111 195 m on a sphere of radius 6 371 km.
        val distance = validate.distanceMeters(0.0, 0.0, 1.0, 0.0)

        assertThat(distance).isWithin(1.0).of(111_194.9)
    }
}
