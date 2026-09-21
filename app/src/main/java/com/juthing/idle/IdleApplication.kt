package com.juthing.idle

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.juthing.idle.blocking.worker.MaintenanceWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point.
 *
 * Installs the Hilt graph, teaches WorkManager how to build injected workers, and schedules the
 * one piece of background housekeeping. Nothing else happens at startup, so cold launch stays
 * immediate.
 */
@HiltAndroidApp
class IdleApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        MaintenanceWorker.schedule(this)
    }
}
