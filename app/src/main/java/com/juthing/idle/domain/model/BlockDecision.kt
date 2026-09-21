package com.juthing.idle.domain.model

/** Why an app is being blocked, and what the block screen has to tell the user. */
sealed interface BlockReason {

    /** The rule whose unlock method lifts this block. */
    val rule: Rule

    /**
     * A period is under way.
     *
     * @property endsAtMinute minutes since midnight at which the period ends, so the screen can
     *   say when the app becomes available again on its own.
     */
    data class DuringPeriod(
        override val rule: Rule.Period,
        val endsAtMinute: Int,
    ) : BlockReason

    /**
     * A timer's daily quota is spent.
     *
     * @property limitMinutes the quota the user set, shown to remind them of their own choice.
     */
    data class TimerExhausted(
        override val rule: Rule.Timer,
        val limitMinutes: Int,
    ) : BlockReason
}

/** The outcome of checking one app against every rule. */
sealed interface BlockDecision {

    /** The app may be used, either because no rule covers it or because an unlock is in effect. */
    data object Allowed : BlockDecision

    /** The app must be blocked, for [reason]. */
    data class Blocked(val reason: BlockReason) : BlockDecision
}
