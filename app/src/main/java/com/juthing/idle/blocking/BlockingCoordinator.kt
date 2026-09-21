package com.juthing.idle.blocking

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.juthing.idle.domain.model.BlockDecision
import com.juthing.idle.domain.model.Rule
import com.juthing.idle.domain.repository.InstalledAppsRepository
import com.juthing.idle.domain.repository.RuleRepository
import com.juthing.idle.domain.repository.UsageRepository
import com.juthing.idle.domain.usecase.EvaluateBlockUseCase
import com.juthing.idle.domain.usecase.ReconcileUsageUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BlockingCoordinator"

/** How often counted time is written down. */
private const val TICK_MILLIS = 15_000L

/**
 * Something able to act on the foreground on Idle's behalf.
 *
 * Implemented by the accessibility service, which is the only component that can send the user
 * home without being an activity itself. The coordinator holds it as an interface so that it
 * keeps no reference to the service beyond the window where the service is actually connected.
 */
fun interface ForegroundController {
    /** Leaves the current app, used when the block screen itself cannot be shown. */
    fun goHome()
}

/**
 * The single place that reacts to the app in the foreground changing.
 *
 * Everything that observes the foreground — the accessibility service today — funnels through
 * here, so the decision to block, the decision to start counting, and the decision to stop are
 * always taken from the same view of the world.
 *
 * Counting lives here rather than inside a foreground service. Since Android 12 an app whose only
 * foreground presence is an accessibility service is often refused permission to *start* a
 * foreground service at all, which used to leave quotas silently not counting. The accessibility
 * service is bound by the system, so this process is already kept alive; a plain coroutine here
 * is both simpler and strictly more reliable than a service that may never start.
 */
@Singleton
class BlockingCoordinator @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val evaluateBlock: EvaluateBlockUseCase,
    private val ruleRepository: RuleRepository,
    private val usageRepository: UsageRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val reconcileUsage: ReconcileUsageUseCase,
    private val notifications: BlockingNotifications,
) {

    /**
     * A failure here must never take the process down with it.
     *
     * This scope runs on behalf of an accessibility service: an unhandled exception would crash
     * the whole app and take the blocking with it, which is a far worse outcome than one missed
     * decision.
     */
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, error ->
            Log.e(TAG, "Foreground evaluation failed", error)
        },
    )
    private val mutex = Mutex()

    /** Set while the accessibility service is connected. */
    @Volatile
    var foregroundController: ForegroundController? = null

    /**
     * The package the block screen is currently showing for.
     *
     * Without it, every window change while the block screen is up would be read as the blocked
     * app coming forward again and would relaunch the screen on top of itself.
     */
    private var blockedPackage: String? = null

    private var countingJob: Job? = null
    private var countedPackage: String? = null

    /**
     * Whether this process has already caught up with the system's usage figures.
     *
     * Done once per process, on the first decision: that covers a reboot and a kill, which are
     * exactly the cases where Idle's own counting has a gap.
     */
    private var reconciled = false

    /** Handles a new foreground package. Safe to call from the accessibility service's thread. */
    fun onForegroundApp(packageName: String) {
        scope.launch {
            mutex.withLock {
                if (!reconciled) {
                    reconciled = true
                    runCatching { reconcileUsage() }
                        .onFailure { Log.w(TAG, "Could not reconcile usage", it) }
                }
                evaluate(packageName)
            }
        }
    }

    /** Called when the screen turns off or the launcher comes forward: nothing is in use. */
    fun onForegroundCleared() {
        blockedPackage = null
        stopCounting()
    }

    private suspend fun evaluate(packageName: String) {
        // Idle's own screens are never blocked: the block screen is where a block gets lifted.
        if (packageName == context.packageName) {
            blockedPackage = null
            stopCounting()
            return
        }

        when (evaluateBlock(packageName)) {
            is BlockDecision.Allowed -> {
                blockedPackage = null
                updateCounting(packageName)
            }

            is BlockDecision.Blocked -> {
                stopCounting()
                if (blockedPackage == packageName) return
                blockedPackage = packageName
                showBlockScreen(packageName)
            }
        }
    }

    /**
     * Puts the block screen in front of the app the user just opened.
     *
     * Android only lets an app start an activity from the background under a short list of
     * exemptions, and an accessibility service is not one of them: the permission to draw over
     * other apps is. Without it the system drops the launch silently — no exception, no screen,
     * an app that simply looks broken — so the permission is checked first and its absence is
     * reported rather than swallowed.
     */
    private fun showBlockScreen(packageName: String) {
        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Cannot show the block screen: overlay permission missing")
            notifications.showOverlayPermissionMissing()
            foregroundController?.goHome()
            return
        }

        // The reason is not carried along: the block screen recomputes it, so a period that ended
        // in between is not insisted on.
        val intent = BlockActivity.intentFor(context, packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        runCatching { context.startActivity(intent) }
            .onFailure {
                Log.e(TAG, "Could not show the block screen", it)
                foregroundController?.goHome()
            }
    }

    /**
     * Starts or stops counting depending on whether the foreground app is under a timer.
     *
     * Counting only runs while it has something to count, so a phone that is not using any timed
     * app carries no ongoing notification and no background work.
     */
    private suspend fun updateCounting(packageName: String) {
        val underTimer = ruleRepository.getEnabledRules()
            .filterIsInstance<Rule.Timer>()
            .any { packageName in it.packageNames }

        if (underTimer) startCounting(packageName) else stopCounting()
    }

    /**
     * Adds a tick's worth of time every [TICK_MILLIS], then re-checks the app.
     *
     * Re-checking from the tick is what makes a quota bite while the user is still inside the
     * app, rather than only the next time they switch to it. Every tick is written as an
     * increment rather than a total, so a process killed between two ticks costs one tick instead
     * of the whole session.
     */
    private fun startCounting(packageName: String) {
        if (countedPackage == packageName && countingJob?.isActive == true) return

        stopCounting()
        countedPackage = packageName
        countingJob = scope.launch {
            notifications.showCounting(installedAppsRepository.labelFor(packageName))
            while (isActive) {
                delay(TICK_MILLIS)
                usageRepository.addUsage(packageName, TICK_MILLIS)
                // Re-entering the lock is safe: the only caller holding it is a foreground event,
                // and this tick simply waits for it to finish before asking again.
                mutex.withLock { evaluate(packageName) }
            }
        }
    }

    private fun stopCounting() {
        countingJob?.cancel()
        countingJob = null
        countedPackage = null
        notifications.hideCounting()
    }
}
