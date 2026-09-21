package com.juthing.idle.domain.usecase

import com.juthing.idle.domain.repository.SettingsRepository
import com.juthing.idle.domain.repository.UnlockGrantRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Records a successful unlock for one app.
 *
 * The grant covers the app the user was trying to open, not the whole rule: unlocking one app
 * under a rule leaves its siblings blocked, so every detour costs its own scan.
 */
class GrantUnlockUseCase @Inject constructor(
    private val grantRepository: UnlockGrantRepository,
    private val settingsRepository: SettingsRepository,
) {

    /**
     * @param ruleId the rule being lifted.
     * @param packageName the app that triggered the block screen.
     * @return how long the app stays usable, in milliseconds.
     */
    suspend operator fun invoke(ruleId: Long, packageName: String): Long {
        val durationMillis = unlockWindowMillis()
        grantRepository.grant(ruleId = ruleId, packageName = packageName, durationMillis = durationMillis)
        return durationMillis
    }

    /**
     * Opens a rule for editing after its method was presented from inside Idle.
     *
     * Filed against the rule rather than against any app: this unlock buys the right to change
     * the rule, not the right to use the apps it covers. Someone who wants both pays twice, which
     * is the point.
     *
     * @return how long the rule stays editable, in milliseconds.
     */
    suspend fun forEditing(ruleId: Long): Long {
        val durationMillis = unlockWindowMillis()
        grantRepository.grant(
            ruleId = ruleId,
            packageName = UnlockGrantRepository.EDIT_SCOPE,
            durationMillis = durationMillis,
        )
        return durationMillis
    }

    private suspend fun unlockWindowMillis(): Long =
        settingsRepository.unlockDurationMinutes.first() * 60_000L
}
