package com.juthing.idle.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * How long one app has been in the foreground on one day.
 *
 * @property date the local day, as an ISO-8601 string (`2026-09-21`). Stored as text so that the
 *   row belongs to the day the user experienced, independently of time zone changes.
 * @property foregroundMillis accumulated foreground time for that day.
 */
@Entity(
    tableName = "daily_usage",
    primaryKeys = ["date", "package_name"],
    indices = [Index("date")],
)
data class DailyUsageEntity(
    val date: String,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "foreground_millis") val foregroundMillis: Long,
)
