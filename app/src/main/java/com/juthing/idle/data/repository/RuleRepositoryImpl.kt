package com.juthing.idle.data.repository

import androidx.room.withTransaction
import com.juthing.idle.core.time.IdleClock
import com.juthing.idle.data.local.IdleDatabase
import com.juthing.idle.data.local.dao.RuleDao
import com.juthing.idle.data.local.entity.RuleAppEntity
import com.juthing.idle.data.local.entity.RuleType
import com.juthing.idle.data.mapper.toDomain
import com.juthing.idle.data.mapper.toEntities
import com.juthing.idle.domain.model.Rule
import com.juthing.idle.domain.repository.RuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Room-backed [RuleRepository]. */
class RuleRepositoryImpl @Inject constructor(
    private val database: IdleDatabase,
    private val ruleDao: RuleDao,
    private val clock: IdleClock,
) : RuleRepository {

    override fun observePeriods(): Flow<List<Rule.Period>> =
        ruleDao.observeByType(RuleType.PERIOD).map { rows ->
            rows.map { it.toDomain() }.filterIsInstance<Rule.Period>()
        }

    override fun observeTimers(): Flow<List<Rule.Timer>> =
        ruleDao.observeByType(RuleType.TIMER).map { rows ->
            rows.map { it.toDomain() }.filterIsInstance<Rule.Timer>()
        }

    override fun observeRule(id: Long): Flow<Rule?> =
        ruleDao.observeById(id).map { it?.toDomain() }

    override suspend fun getRule(id: Long): Rule? = ruleDao.getById(id)?.toDomain()

    override suspend fun getEnabledRules(): List<Rule> =
        ruleDao.getEnabled().map { it.toDomain() }

    override fun observeEnabledRules(): Flow<List<Rule>> =
        ruleDao.observeEnabled().map { rows -> rows.map { it.toDomain() } }

    /**
     * Writes the rule, its detail row and its target apps in one transaction, so that a rule can
     * never end up stored without the details the blocking engine needs to evaluate it.
     */
    override suspend fun save(rule: Rule): Long = database.withTransaction {
        val rows = rule.toEntities(createdAt = clock.nowMillis())
        val id = if (rule.id == 0L) {
            ruleDao.insertRule(rows.rule)
        } else {
            ruleDao.updateRule(rows.rule)
            ruleDao.deleteApps(rule.id)
            rule.id
        }

        rows.period?.let { ruleDao.upsertPeriod(it.copy(ruleId = id)) }
        rows.timer?.let { ruleDao.upsertTimer(it.copy(ruleId = id)) }
        ruleDao.insertApps(rule.packageNames.map { RuleAppEntity(ruleId = id, packageName = it) })
        id
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) = ruleDao.setEnabled(id, enabled)

    override suspend fun delete(id: Long) = ruleDao.delete(id)

    override suspend fun countRulesUsing(unlockMethodId: Long): Int =
        ruleDao.countUsing(unlockMethodId)
}
