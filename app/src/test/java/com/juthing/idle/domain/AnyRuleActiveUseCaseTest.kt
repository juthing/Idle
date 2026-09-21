package com.juthing.idle.domain

import com.google.common.truth.Truth.assertThat
import com.juthing.idle.domain.model.Rule
import com.juthing.idle.domain.usecase.AnyRuleActiveUseCase
import com.juthing.idle.domain.usecase.ScheduleEvaluator
import com.juthing.idle.domain.usecase.TimerEvaluator
import com.juthing.idle.fake.FakeClock
import com.juthing.idle.fake.FakeRuleRepository
import com.juthing.idle.fake.FakeUsageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

class AnyRuleActiveUseCaseTest {

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
        rules: List<Rule>,
        clock: FakeClock,
        usage: FakeUsageRepository = FakeUsageRepository(),
    ) = AnyRuleActiveUseCase(
        ruleRepository = FakeRuleRepository(rules),
        usageRepository = usage,
        scheduleEvaluator = ScheduleEvaluator(),
        timerEvaluator = TimerEvaluator(),
        clock = clock,
    )

    @Test
    fun `nothing is active when there are no rules`() = runTest {
        assertThat(useCase(emptyList(), FakeClock(mondayNight))()).isFalse()
    }

    @Test
    fun `a running period counts as active`() = runTest {
        assertThat(useCase(listOf(nightPeriod), FakeClock(mondayNight))()).isTrue()
    }

    @Test
    fun `a period outside its hours does not count`() = runTest {
        assertThat(useCase(listOf(nightPeriod), FakeClock(mondayNoon))()).isFalse()
    }

    @Test
    fun `a spent timer counts as active`() = runTest {
        val usage = FakeUsageRepository(mutableMapOf(tube to 120 * 60_000L))

        assertThat(useCase(listOf(videoTimer), FakeClock(mondayNoon), usage)()).isTrue()
    }

    @Test
    fun `a timer with budget left does not count`() = runTest {
        val usage = FakeUsageRepository(mutableMapOf(tube to 10 * 60_000L))

        assertThat(useCase(listOf(videoTimer), FakeClock(mondayNoon), usage)()).isFalse()
    }

    @Test
    fun `a disabled rule never counts however it looks`() = runTest {
        val disabled = nightPeriod.copy(enabled = false)

        assertThat(useCase(listOf(disabled), FakeClock(mondayNight))()).isFalse()
    }
}
