package com.juthing.idle.data.repository

import com.juthing.idle.core.time.IdleClock
import com.juthing.idle.data.local.dao.UnlockGrantDao
import com.juthing.idle.data.local.entity.UnlockGrantEntity
import com.juthing.idle.data.mapper.toDomain
import com.juthing.idle.domain.model.UnlockGrant
import com.juthing.idle.domain.repository.UnlockGrantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Room-backed [UnlockGrantRepository]. */
class UnlockGrantRepositoryImpl @Inject constructor(
    private val dao: UnlockGrantDao,
    private val clock: IdleClock,
) : UnlockGrantRepository {

    override suspend fun activeGrantFor(packageName: String): UnlockGrant? =
        dao.getActiveFor(packageName, clock.nowMillis())?.toDomain()

    override suspend fun activeEditGrantFor(ruleId: Long): UnlockGrant? = dao.getActiveForRule(
        ruleId = ruleId,
        scope = UnlockGrantRepository.EDIT_SCOPE,
        nowMillis = clock.nowMillis(),
    )?.toDomain()

    override fun observeActiveGrants(): Flow<List<UnlockGrant>> =
        dao.observeActive(clock.nowMillis()).map { grants -> grants.map { it.toDomain() } }

    override suspend fun grant(ruleId: Long?, packageName: String, durationMillis: Long): Long {
        val now = clock.nowMillis()
        return dao.insert(
            UnlockGrantEntity(
                ruleId = ruleId,
                packageName = packageName,
                grantedAt = now,
                expiresAt = now + durationMillis,
            ),
        )
    }

    override suspend fun revoke(packageName: String) = dao.revokeFor(packageName, clock.nowMillis())

    override suspend fun purgeExpiredBefore(cutoffMillis: Long) =
        dao.deleteExpiredBefore(cutoffMillis)
}
