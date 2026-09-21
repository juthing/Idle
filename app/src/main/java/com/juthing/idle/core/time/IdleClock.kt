package com.juthing.idle.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The app's source of time.
 *
 * Every rule decision depends on "now", so time is injected rather than read from
 * [System.currentTimeMillis] directly: that is what makes schedules and quotas testable without
 * waiting for the clock.
 */
interface IdleClock {
    /** Milliseconds since the epoch. */
    fun nowMillis(): Long

    /** The current date in the device's time zone. */
    fun today(): LocalDate

    /** The current date and time in the device's time zone. */
    fun nowDateTime(): LocalDateTime
}

/** The real clock, reading the system time in the device's current zone. */
class SystemIdleClock(private val zone: ZoneId = ZoneId.systemDefault()) : IdleClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
    override fun today(): LocalDate = LocalDate.now(zone)
    override fun nowDateTime(): LocalDateTime = LocalDateTime.ofInstant(Instant.now(), zone)
}
