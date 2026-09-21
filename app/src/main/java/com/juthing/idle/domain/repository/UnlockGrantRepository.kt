package com.juthing.idle.domain.repository

import com.juthing.idle.domain.model.UnlockGrant
import kotlinx.coroutines.flow.Flow

/** Stores the unlocks currently in effect. */
interface UnlockGrantRepository {

    /** The grant keeping [packageName] usable right now, or `null` if it is blocked. */
    suspend fun activeGrantFor(packageName: String): UnlockGrant?

    /**
     * The grant currently allowing [ruleId] to be edited, or `null` if the rule is still sealed.
     *
     * Deliberately separate from [activeGrantFor]: lifting a block on one app is permission to
     * use that app for a quarter of an hour, not permission to rewrite the rule that blocked it.
     * Changing a running rule costs its own unlock.
     */
    suspend fun activeEditGrantFor(ruleId: Long): UnlockGrant?

    fun observeActiveGrants(): Flow<List<UnlockGrant>>

    /**
     * Records an unlock for one app.
     *
     * @param ruleId the rule being lifted, or `null` for a grant drawn from the emergency quota.
     * @param durationMillis how long the app stays usable.
     * @return the identifier of the stored grant.
     */
    suspend fun grant(ruleId: Long?, packageName: String, durationMillis: Long): Long

    /** Ends any grant on [packageName] immediately. */
    suspend fun revoke(packageName: String)

    /** Drops grants that expired before [cutoffMillis]. */
    suspend fun purgeExpiredBefore(cutoffMillis: Long)

    companion object {
        /**
         * The package name a grant carries when what was unlocked is a rule, not an app.
         *
         * Grants are stored per app, and an edit unlock belongs to no app at all. Rather than
         * making the column nullable and having every app lookup learn to skip nulls, edit grants
         * are filed under a name no package can have.
         */
        const val EDIT_SCOPE = "idle:rule-edit"
    }
}
