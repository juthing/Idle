package com.juthing.idle.domain

import com.google.common.truth.Truth.assertThat
import com.juthing.idle.domain.repository.SettingsRepository
import com.juthing.idle.domain.usecase.EmergencyUnlockUseCase
import com.juthing.idle.fake.FakeClock
import com.juthing.idle.fake.FakeSettingsRepository
import com.juthing.idle.fake.FakeUnlockGrantRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDateTime

class EmergencyUnlockUseCaseTest {

    private val app = "com.example.social"
    private val clock = FakeClock(LocalDateTime.of(2026, 9, 21, 23, 0))
    private val grants = FakeUnlockGrantRepository(clock)
    private val settings = FakeSettingsRepository()
    private val useCase = EmergencyUnlockUseCase(grants, settings, clock)

    @Test
    fun `a full budget is five minutes`() = runTest {
        assertThat(useCase.remainingSeconds())
            .isEqualTo(SettingsRepository.EMERGENCY_DAILY_SECONDS)
    }

    @Test
    fun `using the budget grants the whole remaining time`() = runTest {
        val granted = useCase(app)

        assertThat(granted).isEqualTo(SettingsRepository.EMERGENCY_DAILY_SECONDS * 1_000L)
        assertThat(grants.activeGrantFor(app)).isNotNull()
    }

    @Test
    fun `an emergency grant is not tied to any rule`() = runTest {
        useCase(app)

        assertThat(grants.activeGrantFor(app)?.ruleId).isNull()
    }

    @Test
    fun `the budget is spent after one use`() = runTest {
        useCase(app)

        assertThat(useCase.remainingSeconds()).isEqualTo(0)
    }

    @Test
    fun `a second attempt on the same day grants nothing`() = runTest {
        useCase(app)

        assertThat(useCase("com.example.other")).isEqualTo(0)
    }

    @Test
    fun `the budget is shared by every app rather than per app`() = runTest {
        useCase(app)

        assertThat(useCase("com.example.tube")).isEqualTo(0)
    }

    @Test
    fun `a partly used budget grants only what is left`() = runTest {
        settings.addEmergencySeconds(clock.today(), seconds = 120)

        val granted = useCase(app)

        assertThat(granted).isEqualTo((SettingsRepository.EMERGENCY_DAILY_SECONDS - 120) * 1_000L)
    }

    @Test
    fun `the budget comes back the next day`() = runTest {
        useCase(app)

        clock.now = clock.now.plusDays(1)

        assertThat(useCase.remainingSeconds())
            .isEqualTo(SettingsRepository.EMERGENCY_DAILY_SECONDS)
    }
}
