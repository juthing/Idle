package com.juthing.idle.fake

import com.juthing.idle.core.time.IdleClock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/** A clock the tests move by hand, so schedule and quota logic never depends on the real time. */
class FakeClock(var now: LocalDateTime = LocalDateTime.of(2026, 9, 21, 12, 0)) : IdleClock {
    override fun nowMillis(): Long = now.toInstant(ZoneOffset.UTC).toEpochMilli()
    override fun today(): LocalDate = now.toLocalDate()
    override fun nowDateTime(): LocalDateTime = now
}
