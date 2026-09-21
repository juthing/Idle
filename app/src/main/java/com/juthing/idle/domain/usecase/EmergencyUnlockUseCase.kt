package com.juthing.idle.domain.usecase

import com.juthing.idle.core.time.IdleClock
import com.juthing.idle.domain.repository.SettingsRepository
import com.juthing.idle.domain.repository.UnlockGrantRepository
import javax.inject.Inject

/**
 * The daily escape hatch: a few minutes that need no scan, no tag and no trip.
 *
 * The budget is deliberately small, shared by every blocked app, and not configurable anywhere in
 * the app. A quota the user could raise would be the simplest way around every rule they set, so
 * the only way to get more is to wait for tomorrow.
 */
class EmergencyUnlockUseCase @Inject constructor(
    private val grantRepository: UnlockGrantRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: IdleClock,
) {

    /** How many seconds of emergency time are left today. */
    suspend fun remainingSeconds(): Int {
        val used = settingsRepository.emergencySecondsUsed(clock.today())
        return (SettingsRepository.EMERGENCY_DAILY_SECONDS - used).coerceAtLeast(0)
    }

    /**
     * Spends the remaining emergency time on [packageName].
     *
     * The whole remaining budget is spent at once rather than in fixed slices: splitting it would
     * only invite the user to come back for the next slice a minute later.
     *
     * @return the granted duration in milliseconds, or `0` when the budget is already spent.
     */
    suspend operator fun invoke(packageName: String): Long {
        val remaining = remainingSeconds()
        if (remaining <= 0) return 0

        settingsRepository.addEmergencySeconds(clock.today(), remaining)
        val durationMillis = remaining * 1_000L
        grantRepository.grant(ruleId = null, packageName = packageName, durationMillis = durationMillis)
        return durationMillis
    }
}
