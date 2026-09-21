package com.juthing.idle.domain.repository

import java.time.LocalDate

/**
 * The platform's own view of how long each app has been used.
 *
 * Declared here so that the reconciliation logic stays in the domain layer, with no idea that the
 * figures come from Android's UsageStatsManager.
 */
interface SystemUsageSource {

    /** Whether the user has granted the access this source needs. */
    fun hasPermission(): Boolean

    /**
     * Foreground milliseconds per package for [date].
     *
     * @return an empty map when access has not been granted, so callers fall back to their own
     *   counting rather than reading missing data as zero usage.
     */
    suspend fun usageFor(date: LocalDate): Map<String, Long>
}
