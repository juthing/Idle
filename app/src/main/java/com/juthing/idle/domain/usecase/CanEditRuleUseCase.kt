package com.juthing.idle.domain.usecase

import com.juthing.idle.core.time.IdleClock
import com.juthing.idle.domain.model.Rule
import com.juthing.idle.domain.repository.UsageRepository
import javax.inject.Inject

/**
 * Decides whether a rule can be changed without going through its unlock method.
 *
 * A rule that is doing its job right now is locked: without this, the whole app could be defeated
 * by opening it and switching the rule off, which is precisely the impulse the rules exist to
 * resist. Creating rules is always free, and everything stays readable while locked.
 */
class CanEditRuleUseCase @Inject constructor(
    private val usageRepository: UsageRepository,
    private val scheduleEvaluator: ScheduleEvaluator,
    private val timerEvaluator: TimerEvaluator,
    private val clock: IdleClock,
) {

    /**
     * Whether [rule] may currently be edited, disabled or deleted freely.
     *
     * A disabled rule is never active, so it can always be edited: turning a rule off is itself
     * guarded, which is what keeps that from being a way around the lock.
     */
    suspend operator fun invoke(rule: Rule): Boolean {
        if (!rule.enabled) return true
        return when (rule) {
            is Rule.Period -> !scheduleEvaluator.isActiveAt(rule, clock.nowDateTime())
            is Rule.Timer -> {
                val used = usageRepository.totalUsageMillis(clock.today(), rule.packageNames)
                !timerEvaluator.isExhausted(rule, used)
            }
        }
    }
}
