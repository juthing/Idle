package com.juthing.idle.di

import android.content.Context
import androidx.room.Room
import com.juthing.idle.data.local.IdleDatabase
import com.juthing.idle.data.local.dao.DailyUsageDao
import com.juthing.idle.data.local.dao.RuleDao
import com.juthing.idle.data.local.dao.UnlockGrantDao
import com.juthing.idle.data.local.dao.UnlockMethodDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the database and its DAOs. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): IdleDatabase =
        Room.databaseBuilder(context, IdleDatabase::class.java, IdleDatabase.NAME)
            // Foreign keys are what keep a rule from outliving the unlock method guarding it.
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()

    @Provides
    fun provideRuleDao(database: IdleDatabase): RuleDao = database.ruleDao()

    @Provides
    fun provideUnlockMethodDao(database: IdleDatabase): UnlockMethodDao = database.unlockMethodDao()

    @Provides
    fun provideUnlockGrantDao(database: IdleDatabase): UnlockGrantDao = database.unlockGrantDao()

    @Provides
    fun provideDailyUsageDao(database: IdleDatabase): DailyUsageDao = database.dailyUsageDao()
}
