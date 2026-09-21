package com.juthing.idle.blocking.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.juthing.idle.core.time.IdleClock
import com.juthing.idle.domain.repository.UnlockGrantRepository
import com.juthing.idle.domain.repository.UsageRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/** How much usage history is worth keeping, in days. */
private const val HISTORY_DAYS = 30L

/**
 * Housekeeping: drops usage rows and unlock grants that are too old to matter.
 *
 * There is deliberately no midnight reset here. Usage rows are keyed by date and the emergency
 * counter carries the day it belongs to, so a new day already starts empty; a worker that had to
 * fire at midnight for the app to behave correctly would be a worker the app could not rely on.
 *
 * WorkManager is right for this and wrong for blocking: it makes no promise about when it runs,
 * which is fine for deleting old rows and unacceptable for enforcing a limit.
 */
@HiltWorker
class MaintenanceWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val usageRepository: UsageRepository,
    private val grantRepository: UnlockGrantRepository,
    private val clock: IdleClock,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        usageRepository.purgeBefore(clock.today().minusDays(HISTORY_DAYS))
        // Expired grants are kept briefly so the app can show when a block was last lifted.
        grantRepository.purgeExpiredBefore(
            clock.nowMillis() - TimeUnit.DAYS.toMillis(HISTORY_DAYS),
        )
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "idle_maintenance"

        /**
         * Schedules the daily run, replacing nothing if it is already scheduled.
         *
         * Constrained to a charging, idle phone: none of this is urgent, and housekeeping should
         * never cost the user battery.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MaintenanceWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(true)
                        .build(),
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
