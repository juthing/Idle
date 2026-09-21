package com.juthing.idle.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.juthing.idle.data.local.entity.PeriodDetailEntity
import com.juthing.idle.data.local.entity.RuleAppEntity
import com.juthing.idle.data.local.entity.RuleEntity
import com.juthing.idle.data.local.entity.RuleType
import com.juthing.idle.data.local.entity.TimerDetailEntity
import kotlinx.coroutines.flow.Flow

/** Reads and writes rules together with their type-specific details and target apps. */
@Dao
interface RuleDao {

    @Transaction
    @Query("SELECT * FROM rules WHERE type = :type ORDER BY created_at DESC")
    fun observeByType(type: RuleType): Flow<List<RuleWithDetails>>

    @Transaction
    @Query("SELECT * FROM rules WHERE id = :id")
    fun observeById(id: Long): Flow<RuleWithDetails?>

    @Transaction
    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun getById(id: Long): RuleWithDetails?

    /** Every enabled rule, used by the blocking engine to decide on a foreground app. */
    @Transaction
    @Query("SELECT * FROM rules WHERE enabled = 1")
    suspend fun getEnabled(): List<RuleWithDetails>

    @Transaction
    @Query("SELECT * FROM rules WHERE enabled = 1")
    fun observeEnabled(): Flow<List<RuleWithDetails>>

    /** How many enabled rules use [unlockMethodId]; used to refuse deleting a method in use. */
    @Query("SELECT COUNT(*) FROM rules WHERE unlock_method_id = :unlockMethodId")
    suspend fun countUsing(unlockMethodId: Long): Int

    @Query("UPDATE rules SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert
    suspend fun insertRule(rule: RuleEntity): Long

    @Update
    suspend fun updateRule(rule: RuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPeriod(detail: PeriodDetailEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTimer(detail: TimerDetailEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertApps(apps: List<RuleAppEntity>)

    @Query("DELETE FROM rule_apps WHERE rule_id = :ruleId")
    suspend fun deleteApps(ruleId: Long)
}
