package com.juthing.idle.domain.usecase

import com.juthing.idle.domain.model.Rule
import javax.inject.Inject

/** Decides whether a timer's daily quota has run out. */
class TimerEvaluator @Inject constructor() {

    /**
     * Whether [timer] is out of budget given [usedMillis].
     *
     * [usedMillis] is the total across every app the timer covers: apps grouped under one timer
     * share a single budget, which is what makes "2 hours of video a day" mean what it says.
     */
    fun isExhausted(timer: Rule.Timer, usedMillis: Long): Boolean =
        usedMillis >= timer.dailyLimitMinutes * 60_000L

    /** How much of the quota is left, never negative. */
    fun remainingMillis(timer: Rule.Timer, usedMillis: Long): Long =
        (timer.dailyLimitMinutes * 60_000L - usedMillis).coerceAtLeast(0)
}
