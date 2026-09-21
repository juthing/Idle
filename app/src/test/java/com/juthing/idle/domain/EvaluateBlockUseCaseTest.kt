package com.juthing.idle.domain

import com.google.common.truth.Truth.assertThat
import com.juthing.idle.domain.model.BlockDecision
import com.juthing.idle.domain.model.BlockReason
import com.juthing.idle.domain.model.Rule
import com.juthing.idle.domain.usecase.EvaluateBlockUseCase
import com.juthing.idle.domain.usecase.ScheduleEvaluator
import com.juthing.idle.domain.usecase.TimerEvaluator
import com.juthing.idle.fake.FakeClock
import com.juthing.idle.fake.FakeRuleRepository
import com.juthing.idle.fake.FakeUnlockGrantRepository
import com.juthing.idle.fake.FakeUsageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

class EvaluateBlockUseCaseTest {

    private val social = "com.example.social"
    private val tube = "com.example.tube"

    // 2026-09-21 is a Monday.
    private val mondayNight = LocalDateTime.of(2026, 9, 21, 23, 0)
    private val mondayNoon = LocalDateTime.of(2026, 9, 21, 12, 0)

    private val weekdays = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
    )

    private val nightPeriod = Rule.Period(
        id = 1,
        name = "Night",
        packageNames = setOf(social),
        unlockMethodId = 1,
        daysOfWeek = weekdays,
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
        rules: List<Rule>,
        clock: FakeClock,
        grants: FakeUnlockGrantRepository = FakeUnlockGrantRepository(clock),
        usage: FakeUsageRepository = FakeUsageRepository(),
    ) = EvaluateBlockUseCase(
        ruleRepository = FakeRuleRepository(rules),
        grantRepository = grants,
        usageRepository = usage,
        scheduleEvaluator = ScheduleEvaluator(),
        timerEvaluator = TimerEvaluator(),
        clock = clock,
    )

    @Test
    fun `an app no rule covers is allowed`() = runTest {
        val clock = FakeClock(mondayNight)

        val decision = useCase(listOf(nightPeriod), clock)("com.example.maps")

        assertThat(decision).isEqualTo(BlockDecision.Allowed)
    }

    @Test
    fun `an app inside an active period is blocked with that period as the reason`() = runTest {
        val clock = FakeClock(mondayNight)

        val decision = useCase(listOf(nightPeriod), clock)(social)

        assertThat(decision).isInstanceOf(BlockDecision.Blocked::class.java)
        val reason = (decision as BlockDecision.Blocked).reason
        assertThat(reason).isInstanceOf(BlockReason.DuringPeriod::class.java)
        assertThat((reason as BlockReason.DuringPeriod).endsAtMinute).isEqualTo(7 * 60)
    }

    @Test
    fun `an app outside its period is allowed`() = runTest {
        val clock = FakeClock(mondayNoon)

        val decision = useCase(listOf(nightPeriod), clock)(social)

        assertThat(decision).isEqualTo(BlockDecision.Allowed)
    }

    @Test
    fun `a disabled rule does not block`() = runTest {
        val clock = FakeClock(mondayNight)

        val decision = useCase(listOf(nightPeriod.copy(enabled = false)), clock)(social)

        assertThat(decision).isEqualTo(BlockDecision.Allowed)
    }

    @Test
    fun `an unlock in effect beats an active period`() = runTest {
        val clock = FakeClock(mondayNight)
        val grants = FakeUnlockGrantRepository(clock)
        grants.grant(ruleId = 1, packageName = social, durationMillis = 15 * 60_000L)

        val decision = useCase(listOf(nightPeriod), clock, grants)(social)

        assertThat(decision).isEqualTo(BlockDecision.Allowed)
    }

    @Test
    fun `an unlock stops holding once it expires`() = runTest {
        val clock = FakeClock(mondayNight)
        val grants = FakeUnlockGrantRepository(clock)
        grants.grant(ruleId = 1, packageName = social, durationMillis = 15 * 60_000L)

        clock.now = mondayNight.plusMinutes(16)
        val decision = useCase(listOf(nightPeriod), clock, grants)(social)

        assertThat(decision).isInstanceOf(BlockDecision.Blocked::class.java)
    }

    @Test
    fun `an unlock only covers the app it was granted for`() = runTest {
        val clock = FakeClock(mondayNight)
        val other = "com.example.other"
        val twoApps = nightPeriod.copy(packageNames = setOf(social, other))
        val grants = FakeUnlockGrantRepository(clock)
        grants.grant(ruleId = 1, packageName = social, durationMillis = 15 * 60_000L)

        assertThat(useCase(listOf(twoApps), clock, grants)(social)).isEqualTo(BlockDecision.Allowed)
        assertThat(useCase(listOf(twoApps), clock, grants)(other))
            .isInstanceOf(BlockDecision.Blocked::class.java)
    }

    @Test
    fun `a timer with budget left allows the app`() = runTest {
        val clock = FakeClock(mondayNoon)
        val usage = FakeUsageRepository(mutableMapOf(tube to 60 * 60_000L))

        val decision = useCase(listOf(videoTimer), clock, usage = usage)(tube)

        assertThat(decision).isEqualTo(BlockDecision.Allowed)
    }

    @Test
    fun `a spent timer blocks with the quota as the reason`() = runTest {
        val clock = FakeClock(mondayNoon)
        val usage = FakeUsageRepository(mutableMapOf(tube to 120 * 60_000L))

        val decision = useCase(listOf(videoTimer), clock, usage = usage)(tube)

        val reason = (decision as BlockDecision.Blocked).reason
        assertThat(reason).isInstanceOf(BlockReason.TimerExhausted::class.java)
        assertThat((reason as BlockReason.TimerExhausted).limitMinutes).isEqualTo(120)
    }

    @Test
    fun `apps under one timer share a single budget`() = runTest {
        val clock = FakeClock(mondayNoon)
        val tok = "com.example.tok"
        val sharedTimer = videoTimer.copy(packageNames = setOf(tube, tok))
        // Neither app reaches the limit alone; together they do.
        val usage = FakeUsageRepository(mutableMapOf(tube to 70 * 60_000L, tok to 60 * 60_000L))

        val decision = useCase(listOf(sharedTimer), clock, usage = usage)(tube)

        assertThat(decision).isInstanceOf(BlockDecision.Blocked::class.java)
    }

    @Test
    fun `an active period wins over a timer that still has budget`() = runTest {
        val clock = FakeClock(mondayNight)
        val bothRules = listOf(
            nightPeriod.copy(packageNames = setOf(tube)),
            videoTimer,
        )
        val usage = FakeUsageRepository(mutableMapOf(tube to 10 * 60_000L))

        val decision = useCase(bothRules, clock, usage = usage)(tube)

        assertThat((decision as BlockDecision.Blocked).reason)
            .isInstanceOf(BlockReason.DuringPeriod::class.java)
    }
}
