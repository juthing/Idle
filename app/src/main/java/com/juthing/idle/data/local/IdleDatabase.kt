package com.juthing.idle.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.juthing.idle.data.local.converter.Converters
import com.juthing.idle.data.local.dao.DailyUsageDao
import com.juthing.idle.data.local.dao.RuleDao
import com.juthing.idle.data.local.dao.UnlockGrantDao
import com.juthing.idle.data.local.dao.UnlockMethodDao
import com.juthing.idle.data.local.entity.DailyUsageEntity
import com.juthing.idle.data.local.entity.PeriodDetailEntity
import com.juthing.idle.data.local.entity.RuleAppEntity
import com.juthing.idle.data.local.entity.RuleEntity
import com.juthing.idle.data.local.entity.TimerDetailEntity
import com.juthing.idle.data.local.entity.UnlockGrantEntity
import com.juthing.idle.data.local.entity.UnlockMethodEntity

/**
 * The single local database.
 *
 * Schemas are exported to `app/schemas` and committed, so that every future migration can be
 * written and tested against the exact shape that shipped.
 */
@Database(
    entities = [
        RuleEntity::class,
        RuleAppEntity::class,
        PeriodDetailEntity::class,
        TimerDetailEntity::class,
        UnlockMethodEntity::class,
        UnlockGrantEntity::class,
        DailyUsageEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class IdleDatabase : RoomDatabase() {

    abstract fun ruleDao(): RuleDao

    abstract fun unlockMethodDao(): UnlockMethodDao

    abstract fun unlockGrantDao(): UnlockGrantDao

    abstract fun dailyUsageDao(): DailyUsageDao

    companion object {
        const val NAME = "idle.db"
    }
}
