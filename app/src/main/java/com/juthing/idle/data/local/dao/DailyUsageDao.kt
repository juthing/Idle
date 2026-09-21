package com.juthing.idle.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.juthing.idle.data.local.entity.DailyUsageEntity
import kotlinx.coroutines.flow.Flow

/** Reads and writes the per-day, per-app foreground totals that timers are checked against. */
@Dao
interface DailyUsageDao {

    @Query("SELECT * FROM daily_usage WHERE date = :date AND package_name IN (:packageNames)")
    suspend fun getForDate(date: String, packageNames: Collection<String>): List<DailyUsageEntity>

    @Query("SELECT * FROM daily_usage WHERE date = :date")
    fun observeForDate(date: String): Flow<List<DailyUsageEntity>>

    /**
     * Adds [deltaMillis] to an app's total for [date], creating the row if needed.
     *
     * Written as a single upsert statement so that concurrent ticks from the counting service
     * cannot lose an increment through a read-modify-write race.
     */
    @Query(
        """
        INSERT INTO daily_usage (date, package_name, foreground_millis)
        VALUES (:date, :packageName, :deltaMillis)
        ON CONFLICT(date, package_name)
        DO UPDATE SET foreground_millis = foreground_millis + :deltaMillis
        """,
    )
    suspend fun addUsage(date: String, packageName: String, deltaMillis: Long)

    @Upsert
    suspend fun upsert(usage: DailyUsageEntity)

    @Query("DELETE FROM daily_usage WHERE date < :cutoffDate")
    suspend fun deleteBefore(cutoffDate: String)
}
