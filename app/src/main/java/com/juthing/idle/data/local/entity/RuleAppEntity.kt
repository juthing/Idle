package com.juthing.idle.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** One app targeted by a rule. Deleting the rule removes its targets. */
@Entity(
    tableName = "rule_apps",
    primaryKeys = ["rule_id", "package_name"],
    foreignKeys = [
        ForeignKey(
            entity = RuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["rule_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("package_name")],
)
data class RuleAppEntity(
    @ColumnInfo(name = "rule_id") val ruleId: Long,
    @ColumnInfo(name = "package_name") val packageName: String,
)
