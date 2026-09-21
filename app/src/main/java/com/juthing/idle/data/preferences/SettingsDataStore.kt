package com.juthing.idle.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.juthing.idle.domain.repository.SettingsRepository
import com.juthing.idle.domain.repository.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * DataStore-backed implementation of [SettingsRepository].
 *
 * The emergency counter lives here rather than in the database because it is a single scalar the
 * blocking screen reads on every open; keeping it out of Room avoids a query on that hot path.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SettingsRepository {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_completed")
        val UNLOCK_MINUTES = intPreferencesKey("unlock_duration_minutes")
        val EMERGENCY_DATE = stringPreferencesKey("emergency_date")
        val EMERGENCY_SECONDS = intPreferencesKey("emergency_seconds_used")
    }

    override val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME]?.let { stored ->
            ThemeMode.entries.firstOrNull { it.name == stored }
        } ?: ThemeMode.SYSTEM
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.name }
    }

    override val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_DONE] ?: false
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = completed }
    }

    override val unlockDurationMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.UNLOCK_MINUTES] ?: SettingsRepository.DEFAULT_UNLOCK_DURATION_MINUTES
    }

    override suspend fun setUnlockDurationMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.UNLOCK_MINUTES] = minutes }
    }

    override suspend fun emergencySecondsUsed(date: LocalDate): Int {
        val prefs = context.dataStore.data.first()
        val storedDate = prefs[Keys.EMERGENCY_DATE]
        return if (storedDate == date.toString()) prefs[Keys.EMERGENCY_SECONDS] ?: 0 else 0
    }

    override suspend fun addEmergencySeconds(date: LocalDate, seconds: Int) {
        context.dataStore.edit { prefs ->
            val sameDay = prefs[Keys.EMERGENCY_DATE] == date.toString()
            val used = if (sameDay) prefs[Keys.EMERGENCY_SECONDS] ?: 0 else 0
            prefs[Keys.EMERGENCY_DATE] = date.toString()
            prefs[Keys.EMERGENCY_SECONDS] = used + seconds
        }
    }
}
