package com.juthing.idle.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

/**
 * The schedule part of a period rule.
 *
 * Times are stored as minutes since midnight rather than as clock strings so that comparisons stay
 * plain integer arithmetic, including for ranges that cross midnight.
 *
 * @property daysOfWeek a 7-bit mask, bit 0 being Monday, matching [java.time.DayOfWeek] ordinals.
 */
@Entity(
    tableName = "rule_periods",
    foreignKeys = [
        ForeignKey(
            entity = RuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["rule_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PeriodDetailEntity(
    @ColumnInfo(name = "rule_id") @androidx.room.PrimaryKey val ruleId: Long,
    @ColumnInfo(name = "days_of_week") val daysOfWeek: Int,
    @ColumnInfo(name = "start_minute") val startMinute: Int,
    @ColumnInfo(name = "end_minute") val endMinute: Int,
)
