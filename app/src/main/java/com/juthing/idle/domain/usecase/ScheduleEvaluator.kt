package com.juthing.idle.domain.usecase

import com.juthing.idle.domain.model.Rule
import java.time.LocalDateTime
import javax.inject.Inject

/** Minutes in a day, used to wrap ranges that cross midnight. */
private const val MINUTES_PER_DAY = 24 * 60

/**
 * Decides whether a period is under way at a given moment.
 *
 * A period that crosses midnight belongs to the day it *starts* on: "weekdays, 22:00 to 07:00"
 * runs from Monday evening to Tuesday morning, and the last one starts on Friday evening. Reading
 * it the other way would block Monday morning off a Sunday the user never selected.
 */
class ScheduleEvaluator @Inject constructor() {

    /** Whether [period] is blocking at [moment]. */
    fun isActiveAt(period: Rule.Period, moment: LocalDateTime): Boolean {
        val minuteOfDay = moment.hour * 60 + moment.minute
        val crossesMidnight = period.endMinute <= period.startMinute

        return if (!crossesMidnight) {
            period.daysOfWeek.contains(moment.dayOfWeek) &&
                minuteOfDay >= period.startMinute &&
                minuteOfDay < period.endMinute
        } else {
            // Either the evening leg of a period starting today, or the morning leg of one that
            // started yesterday.
            val eveningLeg = period.daysOfWeek.contains(moment.dayOfWeek) &&
                minuteOfDay >= period.startMinute
            val morningLeg = period.daysOfWeek.contains(moment.minusDays(1).dayOfWeek) &&
                minuteOfDay < period.endMinute
            eveningLeg || morningLeg
        }
    }

    /**
     * Minutes since midnight at which the currently running [period] ends.
     *
     * Returns the end as a minute of day, wrapped into `0..1439`, so the block screen can display
     * a plain clock time.
     */
    fun endMinuteOf(period: Rule.Period): Int = period.endMinute % MINUTES_PER_DAY
}
