package com.juthing.idle.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/** The quota part of a timer rule. */
@Entity(
    tableName = "rule_timers",
    foreignKeys = [
        ForeignKey(
            entity = RuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["rule_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class TimerDetailEntity(
    @ColumnInfo(name = "rule_id") @PrimaryKey val ruleId: Long,
    @ColumnInfo(name = "daily_limit_minutes") val dailyLimitMinutes: Int,
)
