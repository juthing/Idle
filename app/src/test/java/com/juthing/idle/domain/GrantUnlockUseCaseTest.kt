package com.juthing.idle.domain

import com.google.common.truth.Truth.assertThat
import com.juthing.idle.domain.usecase.GrantUnlockUseCase
import com.juthing.idle.fake.FakeClock
import com.juthing.idle.fake.FakeSettingsRepository
import com.juthing.idle.fake.FakeUnlockGrantRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDateTime

class GrantUnlockUseCaseTest {

    private val app = "com.example.social"
    private val clock = FakeClock(LocalDateTime.of(2026, 9, 21, 23, 0))
    private val grants = FakeUnlockGrantRepository(clock)

    @Test
    fun `an unlock lasts fifteen minutes by default`() = runTest {
        val useCase = GrantUnlockUseCase(grants, FakeSettingsRepository())

        val granted = useCase(ruleId = 1, packageName = app)

        assertThat(granted).isEqualTo(15 * 60_000L)
    }

    @Test
    fun `an unlock follows the duration set in the settings`() = runTest {
        val useCase = GrantUnlockUseCase(grants, FakeSettingsRepository(unlockMinutes = 30))

        val granted = useCase(ruleId = 1, packageName = app)

        assertThat(granted).isEqualTo(30 * 60_000L)
    }

    @Test
    fun `an unlock records which rule it lifted and for which app`() = runTest {
        val useCase = GrantUnlockUseCase(grants, FakeSettingsRepository())

        useCase(ruleId = 7, packageName = app)

        val grant = grants.activeGrantFor(app)
        assertThat(grant?.ruleId).isEqualTo(7)
        assertThat(grant?.packageName).isEqualTo(app)
    }

    @Test
    fun `an unlock leaves other apps under the same rule blocked`() = runTest {
        val useCase = GrantUnlockUseCase(grants, FakeSettingsRepository())

        useCase(ruleId = 1, packageName = app)

        assertThat(grants.activeGrantFor("com.example.other")).isNull()
    }
}
