package com.juthing.idle.domain.repository

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Tracks how long each app has been used, per day. */
interface UsageRepository {

    /** Adds foreground time to today's total for [packageName]. */
    suspend fun addUsage(packageName: String, deltaMillis: Long)

    /** Total foreground time on [date] across [packageNames], which share a timer's quota. */
    suspend fun totalUsageMillis(date: LocalDate, packageNames: Collection<String>): Long

    /** Today's totals per package, for the progress shown on the Timers screen. */
    fun observeTodayUsage(): Flow<Map<String, Long>>

    /** Today's totals per package, read once. */
    suspend fun todayUsage(): Map<String, Long>

    /** Overwrites today's total for [packageName], used when reconciling with UsageStatsManager. */
    suspend fun setUsage(packageName: String, totalMillis: Long)

    /** Drops usage rows older than [cutoff]. */
    suspend fun purgeBefore(cutoff: LocalDate)
}
