package com.juthing.idle.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.juthing.idle.domain.model.UnlockMethodType

/** Room representation of a user-created unlock method. */
@Entity(tableName = "unlock_methods")
data class UnlockMethodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: UnlockMethodType,
    /** SHA-256 of the scanned payload for QR and NFC methods; `null` for location methods. */
    @ColumnInfo(name = "secret_hash") val secretHash: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @ColumnInfo(name = "radius_meters") val radiusMeters: Int? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
