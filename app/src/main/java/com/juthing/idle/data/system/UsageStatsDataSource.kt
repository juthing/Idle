package com.juthing.idle.data.system

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import androidx.core.content.getSystemService
import com.juthing.idle.domain.repository.SystemUsageSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads per-app foreground time from the system.
 *
 * This is the source of truth after a reboot or after Idle was killed, when the foreground
 * service's own counting has a hole in it. During normal use the service counts instead, because
 * [UsageStatsManager] aggregates too coarsely to catch a quota the moment it runs out.
 */
@Singleton
class UsageStatsDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SystemUsageSource {

    /** Whether the user has granted usage access in Android settings. */
    override fun hasPermission(): Boolean {
        val appOps = context.getSystemService<AppOpsManager>() ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Foreground time per package for [date], in milliseconds.
     *
     * @return an empty map when usage access has not been granted, so callers fall back to their
     *   own counting rather than treating missing data as zero usage.
     */
    override suspend fun usageFor(date: LocalDate): Map<String, Long> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyMap()
        val manager = context.getSystemService<UsageStatsManager>() ?: return@withContext emptyMap()

        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
            .orEmpty()
            .groupBy { it.packageName }
            .mapValues { (_, stats) -> stats.sumOf { it.totalTimeInForeground } }
            .filterValues { it > 0 }
    }
}
