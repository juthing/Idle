package com.juthing.idle.data.repository

import com.juthing.idle.core.time.IdleClock
import com.juthing.idle.data.local.dao.UnlockMethodDao
import com.juthing.idle.data.mapper.toDomain
import com.juthing.idle.data.mapper.toEntity
import com.juthing.idle.domain.model.UnlockMethod
import com.juthing.idle.domain.repository.UnlockMethodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Room-backed [UnlockMethodRepository]. */
class UnlockMethodRepositoryImpl @Inject constructor(
    private val dao: UnlockMethodDao,
    private val clock: IdleClock,
) : UnlockMethodRepository {

    override fun observeAll(): Flow<List<UnlockMethod>> =
        dao.observeAll().map { methods -> methods.map { it.toDomain() } }

    override suspend fun get(id: Long): UnlockMethod? = dao.getById(id)?.toDomain()

    override suspend fun save(method: UnlockMethod): Long {
        val entity = method.toEntity(createdAt = clock.nowMillis())
        return if (method.id == 0L) {
            dao.insert(entity)
        } else {
            dao.update(entity)
            method.id
        }
    }

    override suspend fun delete(id: Long) = dao.delete(id)
}
