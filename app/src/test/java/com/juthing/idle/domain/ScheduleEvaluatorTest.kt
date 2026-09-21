package com.juthing.idle.domain

import com.google.common.truth.Truth.assertThat
import com.juthing.idle.domain.model.Rule
import com.juthing.idle.domain.usecase.ScheduleEvaluator
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

class ScheduleEvaluatorTest {

    private val evaluator = ScheduleEvaluator()

    private fun period(
        days: Set<DayOfWeek>,
        startMinute: Int,
        endMinute: Int,
    ) = Rule.Period(
        id = 1,
        name = "Test",
        packageNames = setOf("com.example.app"),
        unlockMethodId = 1,
        daysOfWeek = days,
        startMinute = startMinute,
        endMinute = endMinute,
    )

    private val weekdays = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
    )

    // 2026-09-21 is a Monday; every date below is derived from that week.
    private fun monday(hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 9, 21, hour, minute)
    private fun tuesday(hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 9, 22, hour, minute)
    private fun saturday(hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 9, 26, hour, minute)
    private fun sunday(hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 9, 27, hour, minute)

    @Test
    fun `a same-day range blocks inside its bounds`() {
        val lunchBreak = period(weekdays, startMinute = 12 * 60, endMinute = 14 * 60)

        assertThat(evaluator.isActiveAt(lunchBreak, monday(13, 0))).isTrue()
    }

    @Test
    fun `a same-day range starts on its first minute`() {
        val lunchBreak = period(weekdays, startMinute = 12 * 60, endMinute = 14 * 60)

        assertThat(evaluator.isActiveAt(lunchBreak, monday(12, 0))).isTrue()
    }

    @Test
    fun `a same-day range has already ended on its end minute`() {
        val lunchBreak = period(weekdays, startMinute = 12 * 60, endMinute = 14 * 60)

        assertThat(evaluator.isActiveAt(lunchBreak, monday(14, 0))).isFalse()
    }

    @Test
    fun `a same-day range ignores days that are not selected`() {
        val lunchBreak = period(weekdays, startMinute = 12 * 60, endMinute = 14 * 60)

        assertThat(evaluator.isActiveAt(lunchBreak, saturday(13, 0))).isFalse()
    }

    @Test
    fun `a range crossing midnight blocks on the evening of a selected day`() {
        val night = period(weekdays, startMinute = 22 * 60, endMinute = 7 * 60)

        assertThat(evaluator.isActiveAt(night, monday(23, 30))).isTrue()
    }

    @Test
    fun `a range crossing midnight still blocks the following morning`() {
        val night = period(weekdays, startMinute = 22 * 60, endMinute = 7 * 60)

        assertThat(evaluator.isActiveAt(night, tuesday(6, 30))).isTrue()
    }

    @Test
    fun `a range crossing midnight releases on its end minute`() {
        val night = period(weekdays, startMinute = 22 * 60, endMinute = 7 * 60)

        assertThat(evaluator.isActiveAt(night, tuesday(7, 0))).isFalse()
    }

    @Test
    fun `a range crossing midnight does not block the morning after an unselected day`() {
        // Saturday is not selected, so Sunday morning belongs to no period.
        val night = period(weekdays, startMinute = 22 * 60, endMinute = 7 * 60)

        assertThat(evaluator.isActiveAt(night, sunday(6, 30))).isFalse()
    }

    @Test
    fun `a range crossing midnight blocks Saturday morning after a selected Friday`() {
        val night = period(weekdays, startMinute = 22 * 60, endMinute = 7 * 60)

        assertThat(evaluator.isActiveAt(night, saturday(6, 30))).isTrue()
    }

    @Test
    fun `a range crossing midnight leaves the middle of the day alone`() {
        val night = period(weekdays, startMinute = 22 * 60, endMinute = 7 * 60)

        assertThat(evaluator.isActiveAt(night, monday(15, 0))).isFalse()
    }

    @Test
    fun `a range with equal bounds selected every day blocks around the clock`() {
        val allDay = period(DayOfWeek.entries.toSet(), startMinute = 9 * 60, endMinute = 9 * 60)

        assertThat(evaluator.isActiveAt(allDay, monday(3, 0))).isTrue()
        assertThat(evaluator.isActiveAt(allDay, monday(9, 0))).isTrue()
        assertThat(evaluator.isActiveAt(allDay, monday(20, 0))).isTrue()
    }

    @Test
    fun `an overnight range only carries into a morning whose previous day was selected`() {
        // Sunday night is not part of a weekday schedule, so Monday before 07:00 is free.
        val night = period(weekdays, startMinute = 22 * 60, endMinute = 7 * 60)

        assertThat(evaluator.isActiveAt(night, monday(6, 30))).isFalse()
    }

    @Test
    fun `end minute is reported as a minute of day`() {
        val night = period(weekdays, startMinute = 22 * 60, endMinute = 7 * 60)

        assertThat(evaluator.endMinuteOf(night)).isEqualTo(7 * 60)
    }
}
