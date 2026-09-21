package com.juthing.idle.domain.repository

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** The user's choice of appearance, kept in sync with the system by default. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Simple preferences. Anything with structure or history belongs in the database instead. */
interface SettingsRepository {

    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)

    val onboardingCompleted: Flow<Boolean>

    suspend fun setOnboardingCompleted(completed: Boolean)

    /**
     * How long an unlock lasts, in minutes.
     *
     * Editable only while no rule is active: a setting that could be raised on impulse would be
     * the easiest way around every rule in the app.
     */
    val unlockDurationMinutes: Flow<Int>

    suspend fun setUnlockDurationMinutes(minutes: Int)

    /** Emergency seconds already spent on [date]; `0` once the day rolls over. */
    suspend fun emergencySecondsUsed(date: LocalDate): Int

    /** Adds to the emergency time spent on [date], resetting the counter if the day changed. */
    suspend fun addEmergencySeconds(date: LocalDate, seconds: Int)

    companion object {
        /** Default unlock window, in minutes. */
        const val DEFAULT_UNLOCK_DURATION_MINUTES = 15

        /** The daily emergency budget, shared by every blocked app. Deliberately not configurable. */
        const val EMERGENCY_DAILY_SECONDS = 5 * 60
    }
}
