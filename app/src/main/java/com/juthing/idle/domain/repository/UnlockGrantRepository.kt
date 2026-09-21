package com.juthing.idle.domain.repository

import com.juthing.idle.domain.model.UnlockGrant
import kotlinx.coroutines.flow.Flow

/** Stores the unlocks currently in effect. */
interface UnlockGrantRepository {

    /** The grant keeping [packageName] usable right now, or `null` if it is blocked. */
    suspend fun activeGrantFor(packageName: String): UnlockGrant?

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
}
