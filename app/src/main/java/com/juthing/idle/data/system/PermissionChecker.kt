package com.juthing.idle.data.system

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.juthing.idle.blocking.IdleAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reports which of the things Idle needs are currently granted.
 *
 * None of these can be requested with a runtime dialog: usage access and the accessibility
 * service both live in Android settings, so the app can only check them and send the user there.
 */
@Singleton
class PermissionChecker @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val usageStatsDataSource: UsageStatsDataSource,
) {

    /** Whether Idle can read per-app usage totals. */
    fun hasUsageAccess(): Boolean = usageStatsDataSource.hasPermission()

    /**
     * Whether the accessibility service is switched on.
     *
     * Read from the system setting rather than from the service itself: the service is not
     * running when it is off, so it cannot report its own absence.
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        val expected = ComponentName(context, IdleAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()

        return enabled.split(':').any { entry ->
            ComponentName.unflattenFromString(entry) == expected
        }
    }

    /** Whether Idle may post the notification its counting service requires. */
    fun hasNotificationPermission(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** The Android settings screen where usage access is granted. */
    fun usageAccessIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    /** The Android settings screen where the accessibility service is switched on. */
    fun accessibilitySettingsIntent(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
}
