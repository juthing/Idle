package com.juthing.idle.data.system

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
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

    /**
     * Whether Idle is allowed to put its block screen in front of another app.
     *
     * Android refuses to start an activity from the background unless the app is on a short list
     * of exemptions, and an accessibility service is not on it — "display over other apps" is.
     * Idle never draws an overlay window; it holds the permission purely for that exemption.
     */
    fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(context)

    /** Whether Idle may post the notice it shows while a timed app is open. */
    fun hasNotificationPermission(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** The Android settings screen where usage access is granted. */
    fun usageAccessIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    /** The Android settings screen where the accessibility service is switched on. */
    fun accessibilitySettingsIntent(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    /** The Android settings screen where drawing over other apps is allowed, scoped to Idle. */
    fun overlaySettingsIntent(): Intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        "package:${context.packageName}".toUri(),
    )

    /**
     * Whether Android has agreed to stop putting Idle to sleep.
     *
     * Not a permission so much as a truce. Several manufacturers kill background processes
     * aggressively enough to take the accessibility service down with them, and a service that is
     * not running notices nothing. Idle never asks for this with a dialog — that is a Play policy
     * problem — it only says whether it holds and opens the list where the user can grant it.
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        val manager = context.getSystemService<PowerManager>() ?: return true
        return manager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Android's list of apps exempted from battery optimisation. */
    fun batteryOptimizationIntent(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    /** Idle's own entry in Android's app settings, where a refused permission can be given back. */
    fun appSettingsIntent(): Intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        "package:${context.packageName}".toUri(),
    )
}
