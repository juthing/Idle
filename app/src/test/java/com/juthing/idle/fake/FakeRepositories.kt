package com.juthing.idle.fake

import com.juthing.idle.domain.model.Rule
import com.juthing.idle.domain.model.UnlockGrant
import com.juthing.idle.domain.repository.RuleRepository
import com.juthing.idle.domain.repository.SettingsRepository
import com.juthing.idle.domain.repository.SystemUsageSource
import com.juthing.idle.domain.repository.ThemeMode
import com.juthing.idle.domain.repository.UnlockGrantRepository
import com.juthing.idle.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * In-memory doubles for the repositories the use cases depend on.
 *
 * Hand-written rather than mocked: the tests below are about behaviour under a given state, and a
 * fake that actually stores things reads far closer to the scenario being described.
 */
class FakeRuleRepository(rules: List<Rule> = emptyList()) : RuleRepository {

    private val state = MutableStateFlow(rules)

    override fun observePeriods(): Flow<List<Rule.Period>> =
        state.map { list -> list.filterIsInstance<Rule.Period>() }

    override fun observeTimers(): Flow<List<Rule.Timer>> =
        state.map { list -> list.filterIsInstance<Rule.Timer>() }

    override fun observeRule(id: Long): Flow<Rule?> =
        state.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getRule(id: Long): Rule? = state.value.firstOrNull { it.id == id }

    override suspend fun getEnabledRules(): List<Rule> = state.value.filter { it.enabled }

    override fun observeEnabledRules(): Flow<List<Rule>> =
        state.map { list -> list.filter { it.enabled } }

    override suspend fun save(rule: Rule): Long {
        state.value = state.value.filterNot { it.id == rule.id } + rule
        return rule.id
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) {
        state.value = state.value.map { rule ->
            when {
                rule.id != id -> rule
                rule is Rule.Period -> rule.copy(enabled = enabled)
                rule is Rule.Timer -> rule.copy(enabled = enabled)
                else -> rule
            }
        }
    }

    override suspend fun delete(id: Long) {
        state.value = state.value.filterNot { it.id == id }
    }

    override suspend fun countRulesUsing(unlockMethodId: Long): Int =
        state.value.count { it.unlockMethodId == unlockMethodId }
}

class FakeUnlockGrantRepository(private val clock: FakeClock) : UnlockGrantRepository {

    val grants = mutableListOf<UnlockGrant>()

    override suspend fun activeGrantFor(packageName: String): UnlockGrant? =
        grants.firstOrNull { it.packageName == packageName && it.isActiveAt(clock.nowMillis()) }

    override fun observeActiveGrants(): Flow<List<UnlockGrant>> =
        flowOf(grants.filter { it.isActiveAt(clock.nowMillis()) })

    override suspend fun grant(ruleId: Long?, packageName: String, durationMillis: Long): Long {
        val now = clock.nowMillis()
        val grant = UnlockGrant(
            id = grants.size + 1L,
            ruleId = ruleId,
            packageName = packageName,
            grantedAt = now,
            expiresAt = now + durationMillis,
        )
        grants += grant
        return grant.id
    }

    override suspend fun revoke(packageName: String) {
        val now = clock.nowMillis()
        grants.replaceAll { grant ->
            if (grant.packageName == packageName) grant.copy(expiresAt = now) else grant
        }
    }

    override suspend fun purgeExpiredBefore(cutoffMillis: Long) {
        grants.removeAll { it.expiresAt < cutoffMillis }
    }
}

class FakeUsageRepository(
    private val usage: MutableMap<String, Long> = mutableMapOf(),
) : UsageRepository {

    override suspend fun addUsage(packageName: String, deltaMillis: Long) {
        usage[packageName] = (usage[packageName] ?: 0) + deltaMillis
    }

    override suspend fun totalUsageMillis(
        date: LocalDate,
        packageNames: Collection<String>,
    ): Long = packageNames.sumOf { usage[it] ?: 0 }

    override fun observeTodayUsage(): Flow<Map<String, Long>> = flowOf(usage.toMap())

    override suspend fun todayUsage(): Map<String, Long> = usage.toMap()

    override suspend fun setUsage(packageName: String, totalMillis: Long) {
        usage[packageName] = totalMillis
    }

    override suspend fun purgeBefore(cutoff: LocalDate) = Unit
}

class FakeSettingsRepository(
    unlockMinutes: Int = SettingsRepository.DEFAULT_UNLOCK_DURATION_MINUTES,
) : SettingsRepository {

    override val themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    override val onboardingCompleted = MutableStateFlow(false)
    override val unlockDurationMinutes = MutableStateFlow(unlockMinutes)

    private var emergencyDate: LocalDate? = null
    private var emergencyUsed = 0

    override suspend fun setThemeMode(mode: ThemeMode) { themeMode.value = mode }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        onboardingCompleted.value = completed
    }

    override suspend fun setUnlockDurationMinutes(minutes: Int) {
        unlockDurationMinutes.value = minutes
    }

    override suspend fun emergencySecondsUsed(date: LocalDate): Int =
        if (emergencyDate == date) emergencyUsed else 0

    override suspend fun addEmergencySeconds(date: LocalDate, seconds: Int) {
        if (emergencyDate != date) {
            emergencyDate = date
            emergencyUsed = 0
        }
        emergencyUsed += seconds
    }
}

/** A stand-in for the platform's usage figures, with whatever the test needs it to have seen. */
class FakeSystemUsageSource(
    private val permitted: Boolean = true,
    private val usage: Map<String, Long> = emptyMap(),
) : SystemUsageSource {
    override fun hasPermission(): Boolean = permitted
    override suspend fun usageFor(date: LocalDate): Map<String, Long> = usage
}
