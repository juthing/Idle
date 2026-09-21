package com.juthing.idle.domain.model

/**
 * A blocking rule, in one of the two shapes the app offers.
 *
 * Both shapes share a name, a set of target apps and a mandatory unlock method, which is why they
 * are modelled as one sealed hierarchy rather than two unrelated types.
 *
 * @property id the database identifier, `0` until the rule is first saved.
 * @property name the user-facing label of the rule.
 * @property packageNames the apps this rule applies to; never empty for a saved rule.
 * @property unlockMethodId the method required to lift this rule; mandatory by design.
 * @property enabled whether the rule currently takes part in blocking decisions.
 */
sealed interface Rule {
    val id: Long
    val name: String
    val packageNames: Set<String>
    val unlockMethodId: Long
    val enabled: Boolean
    val createdAt: Long

    /**
     * Blocks the target apps during a recurring time range on selected days.
     *
     * @property daysOfWeek the days the range applies to, as [java.time.DayOfWeek] values.
     * @property startMinute minutes since midnight at which blocking starts, inclusive.
     * @property endMinute minutes since midnight at which blocking ends, exclusive. When it is
     *   less than or equal to [startMinute] the range crosses midnight (22:00 → 07:00).
     */
    data class Period(
        override val id: Long = 0,
        override val name: String,
        override val packageNames: Set<String>,
        override val unlockMethodId: Long,
        override val enabled: Boolean = true,
        override val createdAt: Long = 0,
        val daysOfWeek: Set<java.time.DayOfWeek>,
        val startMinute: Int,
        val endMinute: Int,
    ) : Rule

    /**
     * Blocks the target apps once they have been used for [dailyLimitMinutes] in a day.
     *
     * The quota is shared by every app in [packageNames]: two apps under one timer draw from the
     * same budget, which is what makes "2 hours of video a day" mean what the user expects.
     */
    data class Timer(
        override val id: Long = 0,
        override val name: String,
        override val packageNames: Set<String>,
        override val unlockMethodId: Long,
        override val enabled: Boolean = true,
        override val createdAt: Long = 0,
        val dailyLimitMinutes: Int,
    ) : Rule
}
