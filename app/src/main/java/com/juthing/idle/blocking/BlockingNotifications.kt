package com.juthing.idle.blocking

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.juthing.idle.MainActivity
import com.juthing.idle.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** The channel carrying the quiet "a timed app is open" notice. */
private const val CHANNEL_COUNTING = "idle_timers"

/** The channel carrying the one notice the user has to act on. */
private const val CHANNEL_SETUP = "idle_setup"

private const val ID_COUNTING = 1
private const val ID_SETUP = 2

/**
 * The two things Idle ever says outside its own screens.
 *
 * Both are posted from here rather than from whichever class happens to notice, so that the app
 * can never end up with two competing notifications about the same state, and so that a phone
 * with notifications refused degrades to silence instead of crashing.
 */
@Singleton
class BlockingNotifications @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val manager: NotificationManager? get() = context.getSystemService()

    /**
     * Shows that a timed app is open and its budget is running down.
     *
     * Low importance and silent: this is a disclosure, not news. It is posted while counting so
     * that time is never taken from the user without them being able to see that it is happening.
     */
    fun showCounting(appLabel: String) {
        val manager = manager ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_COUNTING,
                context.getString(R.string.notification_channel_timers),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { setShowBadge(false) },
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_COUNTING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_counting_title))
            .setContentText(context.getString(R.string.notification_counting_body, appLabel))
            .setContentIntent(openIdle())
            .setOngoing(true)
            .setSilent(true)
            .build()

        post(ID_COUNTING, notification)
    }

    /** Takes the counting notice down; safe to call when nothing is showing. */
    fun hideCounting() {
        runCatching { manager?.cancel(ID_COUNTING) }
    }

    /**
     * Says that a block could not be shown because Idle is not allowed to draw over other apps.
     *
     * Without that permission the block screen is silently dropped by Android, which from the
     * user's side looks exactly like an app that does not work. Saying so at the moment it
     * happens, with a way straight to the setting, is the only honest option.
     */
    fun showOverlayPermissionMissing() {
        val manager = manager ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SETUP,
                context.getString(R.string.notification_channel_setup),
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SETUP)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_overlay_title))
            .setContentText(context.getString(R.string.notification_overlay_body))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.notification_overlay_body)),
            )
            .setContentIntent(openIdle())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .build()

        post(ID_SETUP, notification)
    }

    private fun openIdle(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Posting is best-effort: a user who refused notifications keeps a working app. */
    private fun post(id: Int, notification: android.app.Notification) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        runCatching { manager?.notify(id, notification) }
    }
}
