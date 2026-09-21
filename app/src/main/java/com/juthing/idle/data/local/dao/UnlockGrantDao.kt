package com.juthing.idle.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.juthing.idle.data.local.entity.UnlockGrantEntity
import kotlinx.coroutines.flow.Flow

/** Reads and writes the unlocks currently in effect. */
@Dao
interface UnlockGrantDao {

    @Query("SELECT * FROM unlock_grants WHERE package_name = :packageName AND expires_at > :nowMillis LIMIT 1")
    suspend fun getActiveFor(packageName: String, nowMillis: Long): UnlockGrantEntity?

    @Query("SELECT * FROM unlock_grants WHERE expires_at > :nowMillis")
    fun observeActive(nowMillis: Long): Flow<List<UnlockGrantEntity>>

    @Insert
    suspend fun insert(grant: UnlockGrantEntity): Long

    /** Ends a grant early, used when the user chooses to re-lock an app themselves. */
    @Query("UPDATE unlock_grants SET expires_at = :nowMillis WHERE package_name = :packageName AND expires_at > :nowMillis")
    suspend fun revokeFor(packageName: String, nowMillis: Long)

    @Query("DELETE FROM unlock_grants WHERE expires_at < :cutoffMillis")
    suspend fun deleteExpiredBefore(cutoffMillis: Long)
}
