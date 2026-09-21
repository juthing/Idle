package com.juthing.idle.di

import com.juthing.idle.data.preferences.SettingsDataStore
import com.juthing.idle.data.repository.RuleRepositoryImpl
import com.juthing.idle.data.repository.UnlockGrantRepositoryImpl
import com.juthing.idle.data.repository.UnlockMethodRepositoryImpl
import com.juthing.idle.data.repository.UsageRepositoryImpl
import com.juthing.idle.domain.repository.RuleRepository
import com.juthing.idle.domain.repository.SettingsRepository
import com.juthing.idle.domain.repository.UnlockGrantRepository
import com.juthing.idle.domain.repository.UnlockMethodRepository
import com.juthing.idle.domain.repository.UsageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds each domain repository interface to its data-layer implementation. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRuleRepository(impl: RuleRepositoryImpl): RuleRepository

    @Binds
    @Singleton
    abstract fun bindUnlockMethodRepository(impl: UnlockMethodRepositoryImpl): UnlockMethodRepository

    @Binds
    @Singleton
    abstract fun bindUnlockGrantRepository(impl: UnlockGrantRepositoryImpl): UnlockGrantRepository

    @Binds
    @Singleton
    abstract fun bindUsageRepository(impl: UsageRepositoryImpl): UsageRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsDataStore): SettingsRepository
}
