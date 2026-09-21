package com.juthing.idle.domain

import com.google.common.truth.Truth.assertThat
import com.juthing.idle.domain.model.Rule
import com.juthing.idle.domain.repository.UnlockGrantRepository
import com.juthing.idle.domain.usecase.CanEditRuleUseCase
import com.juthing.idle.domain.usecase.ScheduleEvaluator
import com.juthing.idle.domain.usecase.TimerEvaluator
import com.juthing.idle.fake.FakeClock
import com.juthing.idle.fake.FakeUnlockGrantRepository
import com.juthing.idle.fake.FakeUsageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

class CanEditRuleUseCaseTest {

    private val social = "com.example.social"
    private val tube = "com.example.tube"
    private val mondayNight = LocalDateTime.of(2026, 9, 21, 23, 0)
    private val mondayNoon = LocalDateTime.of(2026, 9, 21, 12, 0)

    private val nightPeriod = Rule.Period(
        id = 1,
        name = "Night",
        packageNames = setOf(social),
        unlockMethodId = 1,
        daysOfWeek = setOf(DayOfWeek.MONDAY),
        startMinute = 22 * 60,
        endMinute = 7 * 60,
    )

    private val videoTimer = Rule.Timer(
        id = 2,
        name = "Video",
        packageNames = setOf(tube),
        unlockMethodId = 1,
        dailyLimitMinutes = 120,
    )

    private fun useCase(
        clock: FakeClock,
        usage: FakeUsageRepository = FakeUsageRepository(),
        grants: FakeUnlockGrantRepository = FakeUnlockGrantRepository(clock),
    ) = CanEditRuleUseCase(usage, grants, ScheduleEvaluator(), TimerEvaluator(), clock)

    @Test
    fun `a running period can be edited once it has been unlocked from inside the app`() = runTest {
        val clock = FakeClock(mondayNight)
        val grants = FakeUnlockGrantRepository(clock)
        grants.grant(
            ruleId = nightPeriod.id,
            packageName = UnlockGrantRepository.EDIT_SCOPE,
            durationMillis = 15 * 60_000L,
        )

        assertThat(useCase(clock, grants = grants)(nightPeriod)).isTrue()
    }

    @Test
    fun `unlocking one app under a rule does not open the rule itself`() = runTest {
        val clock = FakeClock(mondayNight)
        val grants = FakeUnlockGrantRepository(clock)
        grants.grant(ruleId = nightPeriod.id, packageName = social, durationMillis = 15 * 60_000L)

        assertThat(useCase(clock, grants = grants)(nightPeriod)).isFalse()
    }

    @Test
    fun `a period that is not running can be edited`() = runTest {
        assertThat(useCase(FakeClock(mondayNoon))(nightPeriod)).isTrue()
    }

    @Test
    fun `a period that is running is locked`() = runTest {
        assertThat(useCase(FakeClock(mondayNight))(nightPeriod)).isFalse()
    }

    @Test
    fun `a timer with budget left can be edited`() = runTest {
        val usage = FakeUsageRepository(mutableMapOf(tube to 30 * 60_000L))

        assertThat(useCase(FakeClock(mondayNoon), usage)(videoTimer)).isTrue()
    }

    @Test
    fun `a spent timer is locked`() = runTest {
        val usage = FakeUsageRepository(mutableMapOf(tube to 120 * 60_000L))

        assertThat(useCase(FakeClock(mondayNoon), usage)(videoTimer)).isFalse()
    }

    @Test
    fun `an already disabled rule can always be edited`() = runTest {
        // Turning a rule off is itself guarded, so a disabled rule is never a way around the lock.
        val disabled = nightPeriod.copy(enabled = false)

        assertThat(useCase(FakeClock(mondayNight))(disabled)).isTrue()
    }
}
