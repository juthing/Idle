package com.juthing.idle.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Discriminates the two rule shapes stored in the shared `rules` table. */
enum class RuleType { PERIOD, TIMER }

/**
 * The part every rule has in common.
 *
 * The foreign key to `unlock_methods` uses [ForeignKey.RESTRICT] on purpose: a method that still
 * guards a rule must not be deletable, otherwise deleting it would silently disarm the rule.
 */
@Entity(
    tableName = "rules",
    foreignKeys = [
        ForeignKey(
            entity = UnlockMethodEntity::class,
            parentColumns = ["id"],
            childColumns = ["unlock_method_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("unlock_method_id")],
)
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: RuleType,
    @ColumnInfo(name = "unlock_method_id") val unlockMethodId: Long,
    val enabled: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
