package com.juthing.idle.di

import com.juthing.idle.core.time.IdleClock
import com.juthing.idle.core.time.SystemIdleClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides app-wide primitives that are not tied to storage. */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideClock(): IdleClock = SystemIdleClock()
}
