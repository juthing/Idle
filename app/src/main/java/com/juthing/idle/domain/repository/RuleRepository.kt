package com.juthing.idle.domain.repository

import com.juthing.idle.domain.model.Rule
import kotlinx.coroutines.flow.Flow

/** Stores the periods and timers the user has defined. */
interface RuleRepository {

    fun observePeriods(): Flow<List<Rule.Period>>

    fun observeTimers(): Flow<List<Rule.Timer>>

    fun observeRule(id: Long): Flow<Rule?>

    suspend fun getRule(id: Long): Rule?

    /** Every rule currently taking part in blocking decisions. */
    suspend fun getEnabledRules(): List<Rule>

    fun observeEnabledRules(): Flow<List<Rule>>

    /**
     * Inserts or updates [rule] together with its details and target apps.
     *
     * @return the identifier of the saved rule.
     */
    suspend fun save(rule: Rule): Long

    suspend fun setEnabled(id: Long, enabled: Boolean)

    suspend fun delete(id: Long)

    /** How many rules depend on an unlock method; a method still in use must not be deleted. */
    suspend fun countRulesUsing(unlockMethodId: Long): Int
}
