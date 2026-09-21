package com.juthing.idle.domain.usecase

import com.juthing.idle.core.time.IdleClock
import com.juthing.idle.domain.model.Rule
import com.juthing.idle.domain.repository.RuleRepository
import com.juthing.idle.domain.repository.UsageRepository
import javax.inject.Inject

/**
 * Whether any rule is doing its job right now.
 *
 * Used to freeze the global unlock duration. A setting the user could raise from fifteen minutes
 * to eight hours mid-period would be the shortest way around every rule in the app, so it is only
 * editable while nothing is being enforced.
 */
class AnyRuleActiveUseCase @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val usageRepository: UsageRepository,
    private val scheduleEvaluator: ScheduleEvaluator,
    private val timerEvaluator: TimerEvaluator,
    private val clock: IdleClock,
) {

    suspend operator fun invoke(): Boolean {
        val now = clock.nowDateTime()
        val today = clock.today()

        return ruleRepository.getEnabledRules().any { rule ->
            when (rule) {
                is Rule.Period -> scheduleEvaluator.isActiveAt(rule, now)
                is Rule.Timer -> timerEvaluator.isExhausted(
                    rule,
                    usageRepository.totalUsageMillis(today, rule.packageNames),
                )
            }
        }
    }
}
