package com.juthing.idle.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.juthing.idle.data.local.entity.UnlockMethodEntity
import kotlinx.coroutines.flow.Flow

/** Reads and writes the unlock methods the user has registered. */
@Dao
interface UnlockMethodDao {

    @Query("SELECT * FROM unlock_methods ORDER BY created_at DESC")
    fun observeAll(): Flow<List<UnlockMethodEntity>>

    @Query("SELECT * FROM unlock_methods WHERE id = :id")
    suspend fun getById(id: Long): UnlockMethodEntity?

    @Insert
    suspend fun insert(method: UnlockMethodEntity): Long

    @Update
    suspend fun update(method: UnlockMethodEntity)

    @Query("DELETE FROM unlock_methods WHERE id = :id")
    suspend fun delete(id: Long)
}
