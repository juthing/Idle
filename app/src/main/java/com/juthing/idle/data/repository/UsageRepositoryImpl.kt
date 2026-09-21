package com.juthing.idle.data.repository

import com.juthing.idle.core.time.IdleClock
import com.juthing.idle.data.local.dao.DailyUsageDao
import com.juthing.idle.data.local.entity.DailyUsageEntity
import com.juthing.idle.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/** Room-backed [UsageRepository]. */
class UsageRepositoryImpl @Inject constructor(
    private val dao: DailyUsageDao,
    private val clock: IdleClock,
) : UsageRepository {

    override suspend fun addUsage(packageName: String, deltaMillis: Long) {
        dao.addUsage(clock.today().toString(), packageName, deltaMillis)
    }

    override suspend fun totalUsageMillis(
        date: LocalDate,
        packageNames: Collection<String>,
    ): Long {
        if (packageNames.isEmpty()) return 0
        return dao.getForDate(date.toString(), packageNames).sumOf { it.foregroundMillis }
    }

    override fun observeTodayUsage(): Flow<Map<String, Long>> =
        dao.observeForDate(clock.today().toString()).map { rows ->
            rows.associate { it.packageName to it.foregroundMillis }
        }

    override suspend fun todayUsage(): Map<String, Long> =
        dao.getAllForDate(clock.today().toString())
            .associate { it.packageName to it.foregroundMillis }

    override suspend fun setUsage(packageName: String, totalMillis: Long) {
        dao.upsert(
            DailyUsageEntity(
                date = clock.today().toString(),
                packageName = packageName,
                foregroundMillis = totalMillis,
            ),
        )
    }

    override suspend fun purgeBefore(cutoff: LocalDate) = dao.deleteBefore(cutoff.toString())
}
