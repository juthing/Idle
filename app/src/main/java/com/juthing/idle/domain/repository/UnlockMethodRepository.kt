package com.juthing.idle.domain.repository

import com.juthing.idle.domain.model.UnlockMethod
import kotlinx.coroutines.flow.Flow

/** Stores the unlock methods the user has registered. */
interface UnlockMethodRepository {

    fun observeAll(): Flow<List<UnlockMethod>>

    suspend fun get(id: Long): UnlockMethod?

    /** @return the identifier of the saved method. */
    suspend fun save(method: UnlockMethod): Long

    suspend fun delete(id: Long)
}
