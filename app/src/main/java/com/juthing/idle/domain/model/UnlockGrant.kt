package com.juthing.idle.domain.model

/**
 * A temporary permission to use one app despite a rule that would otherwise block it.
 *
 * A grant covers a single package rather than the whole rule: unlocking Instagram does not also
 * unlock TikTok, even when one rule covers both.
 *
 * @property ruleId the rule that was lifted, or `null` for a grant taken from the emergency quota.
 * @property expiresAt epoch milliseconds after which the app is blocked again.
 */
data class UnlockGrant(
    val id: Long = 0,
    val ruleId: Long?,
    val packageName: String,
    val grantedAt: Long,
    val expiresAt: Long,
) {
    /** Whether this grant still holds at [nowMillis]. */
    fun isActiveAt(nowMillis: Long): Boolean = nowMillis < expiresAt
}
