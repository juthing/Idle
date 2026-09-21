package com.juthing.idle.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An unlock currently in effect for one app.
 *
 * Expired rows are kept until the nightly cleanup so that the app can show a short history of
 * when blocks were lifted without keeping a separate log.
 *
 * @property ruleId `null` when the grant came from the daily emergency quota.
 */
@Entity(
    tableName = "unlock_grants",
    foreignKeys = [
        ForeignKey(
            entity = RuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["rule_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("package_name", "expires_at"), Index("rule_id")],
)
data class UnlockGrantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "rule_id") val ruleId: Long?,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "granted_at") val grantedAt: Long,
    @ColumnInfo(name = "expires_at") val expiresAt: Long,
)
