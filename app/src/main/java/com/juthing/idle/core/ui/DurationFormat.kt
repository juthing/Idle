package com.juthing.idle.core.ui

import android.content.res.Resources
import com.juthing.idle.R
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Formatting helpers shared by the rule screens.
 *
 * They live outside the Composables so that the same wording is used everywhere a duration, a
 * time or a set of days is shown, and so the locale rules stay in one place.
 *
 * They take [Resources] rather than a `Context`: resources follow configuration changes, so the
 * wording updates when the user switches language or font scale.
 */
object DurationFormat {

    /** A minute count as "2h 30" or "45 min", never as "0h 45". */
    fun duration(resources: Resources, totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours == 0 -> resources.getString(R.string.duration_minutes, minutes)
            minutes == 0 -> resources.getString(R.string.duration_hours, hours)
            else -> resources.getString(R.string.duration_hours_minutes, hours, minutes)
        }
    }

    /** A minute of day as a locale-aware clock time. */
    fun clock(minuteOfDay: Int): String =
        "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)

    /** A time range as "22:00 – 07:00". */
    fun range(resources: Resources, startMinute: Int, endMinute: Int): String =
        resources.getString(R.string.time_range, clock(startMinute), clock(endMinute))

    private val WEEKDAYS = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
    )
    private val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    /**
     * A set of days as the shortest phrase that still says exactly which days are meant.
     *
     * "Every day" and "Weekdays" read better than a list of seven abbreviations, but anything
     * else is spelled out rather than approximated.
     */
    fun days(resources: Resources, days: Set<DayOfWeek>, locale: Locale): String = when {
        days.size == 7 -> resources.getString(R.string.every_day)
        days == WEEKDAYS -> resources.getString(R.string.weekdays)
        days == WEEKEND -> resources.getString(R.string.weekends)
        else -> DayOfWeek.entries
            .filter { it in days }
            .joinToString(separator = " ") { it.getDisplayName(TextStyle.SHORT, locale) }
    }

    /** The one-letter label used on the day toggles. */
    fun dayInitial(day: DayOfWeek, locale: Locale): String =
        day.getDisplayName(TextStyle.NARROW, locale).uppercase(locale)
}
