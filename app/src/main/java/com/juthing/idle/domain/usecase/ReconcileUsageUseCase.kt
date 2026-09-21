package com.juthing.idle.domain.usecase

import com.juthing.idle.core.time.IdleClock
import com.juthing.idle.domain.repository.SystemUsageSource
import com.juthing.idle.domain.repository.UsageRepository
import javax.inject.Inject

/**
 * Brings today's counters back in line with what the system observed.
 *
 * Idle's own counting stops whenever the process is killed or the phone reboots, which would let
 * a spent quota look fresh again. The system's own figures cover those gaps.
 *
 * Each package keeps the larger of the two numbers. Under-counting is the failure that matters
 * here: it hands back time the user had already spent, which is exactly what the quota exists to
 * prevent. Over-counting only ends a session slightly early.
 */
class ReconcileUsageUseCase @Inject constructor(
    private val systemUsage: SystemUsageSource,
    private val usageRepository: UsageRepository,
    private val clock: IdleClock,
) {

    suspend operator fun invoke() {
        if (!systemUsage.hasPermission()) return

        val observed = systemUsage.usageFor(clock.today())
        if (observed.isEmpty()) return

        val stored = usageRepository.todayUsage()
        observed.forEach { (packageName, systemMillis) ->
            val ownMillis = stored[packageName] ?: 0
            if (systemMillis > ownMillis) {
                usageRepository.setUsage(packageName, systemMillis)
            }
        }
    }
}
