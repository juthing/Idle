package com.juthing.idle.blocking

import android.content.Context
import android.content.Intent
import com.juthing.idle.domain.model.BlockDecision
import com.juthing.idle.domain.repository.RuleRepository
import com.juthing.idle.domain.usecase.EvaluateBlockUseCase
import com.juthing.idle.domain.usecase.ReconcileUsageUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single place that reacts to the app in the foreground changing.
 *
 * Everything that observes the foreground — the accessibility service today — funnels through
 * here, so the decision to block, the decision to start counting, and the decision to stop are
 * always taken from the same view of the world.
 */
@Singleton
class BlockingCoordinator @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val evaluateBlock: EvaluateBlockUseCase,
    private val ruleRepository: RuleRepository,
    private val reconcileUsage: ReconcileUsageUseCase,
) {

    private val scope = CoroutineScope(SupervisorJob())
    private val mutex = Mutex()

    /**
     * The package the block screen is currently showing for.
     *
     * Without it, every window change while the block screen is up would be read as the blocked
     * app coming forward again and would relaunch the screen on top of itself.
     */
    private var blockedPackage: String? = null

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
                    reconcileUsage()
                }
                evaluate(packageName)
            }
        }
    }

    private suspend fun evaluate(packageName: String) {
        // Idle's own screens are never blocked: the block screen is where a block gets lifted.
        if (packageName == context.packageName) {
            blockedPackage = null
            return
        }

        when (evaluateBlock(packageName)) {
            is BlockDecision.Allowed -> {
                blockedPackage = null
                updateCounting(packageName)
            }

            is BlockDecision.Blocked -> {
                if (blockedPackage == packageName) return
                blockedPackage = packageName
                TimerCountingService.stop(context)
                // The reason is not carried along: the block screen recomputes it, so a period
                // that ended in between is not insisted on.
                context.startActivity(
                    BlockActivity.intentFor(context, packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                )
            }
        }
    }

    /**
     * Starts or stops counting depending on whether the foreground app is under a timer.
     *
     * The counting service only runs while it has something to count, so a phone that is not
     * using any timed app carries no ongoing notification and no background work.
     */
    private suspend fun updateCounting(packageName: String) {
        val underTimer = ruleRepository.getEnabledRules()
            .filterIsInstance<com.juthing.idle.domain.model.Rule.Timer>()
            .any { packageName in it.packageNames }

        if (underTimer) {
            TimerCountingService.start(context, packageName)
        } else {
            TimerCountingService.stop(context)
        }
    }

    /** Called when the screen turns off or the launcher comes forward: nothing is in use. */
    fun onForegroundCleared() {
        blockedPackage = null
        TimerCountingService.stop(context)
    }
}
