package com.juthing.idle.blocking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import com.juthing.idle.R
import com.juthing.idle.domain.repository.UsageRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/** How often the counted time is written down. */
private const val TICK_MILLIS = 15_000L

/**
 * Counts the time spent in an app that a timer covers.
 *
 * A foreground service rather than WorkManager: WorkManager makes no promise about when it runs,
 * and a quota that is noticed ten minutes late is a quota that does not work. The service only
 * runs while a timed app is actually in the foreground, so a phone that is not using one carries
 * no ongoing notification.
 *
 * Every tick is written to the database as an increment rather than a total, so a kill between
 * ticks costs at most one tick instead of the whole session.
 */
@AndroidEntryPoint
class TimerCountingService : Service() {

    @Inject
    lateinit var usageRepository: UsageRepository

    @Inject
    lateinit var coordinator: BlockingCoordinator

    private val scope = CoroutineScope(SupervisorJob())
    private var countingJob: Job? = null
    private var countedPackage: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val packageName = intent?.getStringExtra(EXTRA_PACKAGE)
        if (packageName == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (packageName == countedPackage) return START_STICKY

        startForeground()
        countedPackage = packageName
        countingJob?.cancel()
        countingJob = scope.launch { count(packageName) }
        return START_STICKY
    }

    /**
     * Adds a tick's worth of time every [TICK_MILLIS], then asks the coordinator to re-check.
     *
     * Re-checking from here is what makes a quota bite while the user is still inside the app,
     * rather than only the next time they switch to it.
     */
    private suspend fun count(packageName: String) {
        while (scope.isActive) {
            delay(TICK_MILLIS)
            usageRepository.addUsage(packageName, TICK_MILLIS)
            coordinator.onForegroundApp(packageName)
        }
    }

    private fun startForeground() {
        val manager = getSystemService<NotificationManager>() ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_timers),
                // Low importance: the notification is a legal and technical requirement, not news.
                NotificationManager.IMPORTANCE_LOW,
            ).apply { setShowBadge(false) },
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_counting_title))
            .setContentText(getString(R.string.notification_counting_body))
            .setOngoing(true)
            .setSilent(true)
            .build()

        // Service types only exist from API 34; below that the platform wants a plain 0.
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)
    }

    override fun onDestroy() {
        scope.cancel()
        countedPackage = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "idle_timers"
        private const val NOTIFICATION_ID = 1
        private const val EXTRA_PACKAGE = "package"

        /** Starts counting [packageName], or switches an already running count to it. */
        fun start(context: Context, packageName: String) {
            context.startForegroundService(
                Intent(context, TimerCountingService::class.java)
                    .putExtra(EXTRA_PACKAGE, packageName),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TimerCountingService::class.java))
        }
    }
}
