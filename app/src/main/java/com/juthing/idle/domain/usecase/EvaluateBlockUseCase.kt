package com.juthing.idle.domain.usecase

import com.juthing.idle.core.time.IdleClock
import com.juthing.idle.domain.model.BlockDecision
import com.juthing.idle.domain.model.BlockReason
import com.juthing.idle.domain.model.Rule
import com.juthing.idle.domain.repository.RuleRepository
import com.juthing.idle.domain.repository.UnlockGrantRepository
import com.juthing.idle.domain.repository.UsageRepository
import javax.inject.Inject

/**
 * Decides whether the app currently in the foreground should be blocked.
 *
 * This runs on every app switch, so the checks are ordered from cheapest to most expensive and
 * the first blocking rule found wins: the user only has to deal with one reason at a time.
 */
class EvaluateBlockUseCase @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val grantRepository: UnlockGrantRepository,
    private val usageRepository: UsageRepository,
    private val scheduleEvaluator: ScheduleEvaluator,
    private val timerEvaluator: TimerEvaluator,
    private val clock: IdleClock,
) {

    suspend operator fun invoke(packageName: String): BlockDecision {
        // An unlock in effect wins over every rule, including one that started since.
        if (grantRepository.activeGrantFor(packageName) != null) return BlockDecision.Allowed

        val rules = ruleRepository.getEnabledRules().filter { packageName in it.packageNames }
        if (rules.isEmpty()) return BlockDecision.Allowed

        val now = clock.nowDateTime()

        // Periods first: they are decided from the clock alone, with no database read.
        rules.filterIsInstance<Rule.Period>()
            .firstOrNull { scheduleEvaluator.isActiveAt(it, now) }
            ?.let { period ->
                return BlockDecision.Blocked(
                    BlockReason.DuringPeriod(period, scheduleEvaluator.endMinuteOf(period)),
                )
            }

        val today = clock.today()
        rules.filterIsInstance<Rule.Timer>().forEach { timer ->
            val used = usageRepository.totalUsageMillis(today, timer.packageNames)
            if (timerEvaluator.isExhausted(timer, used)) {
                return BlockDecision.Blocked(
                    BlockReason.TimerExhausted(timer, timer.dailyLimitMinutes),
                )
            }
        }

        return BlockDecision.Allowed
    }
}
