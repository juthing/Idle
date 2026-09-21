package com.juthing.idle.blocking

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Watches which app is in the foreground.
 *
 * This is the only Android API that reports an app coming forward in time to block it.
 * [android.app.usage.UsageStatsManager] measures durations but reacts far too late: the user
 * would already be scrolling before the block appeared.
 *
 * Idle is not an accessibility tool and must never declare `isAccessibilityTool`. The service
 * reads one thing — the package name of the window that just opened — and that never leaves the
 * device. The user is told exactly this, and consents to it, before ever reaching the Android
 * settings screen that enables the service.
 */
@AndroidEntryPoint
class IdleAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var coordinator: BlockingCoordinator

    /**
     * The last package acted on.
     *
     * Window state changes arrive in bursts as an app draws its first screens; forwarding each
     * one would re-evaluate the same app several times a second.
     */
    private var lastPackage: String? = null

    /**
     * Stops the clock when the screen goes dark.
     *
     * No accessibility event is sent when the display turns off, so without this a timed app left
     * open would keep burning its quota in the user's pocket.
     */
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_SCREEN_OFF) return
            lastPackage = null
            coordinator.onForegroundCleared()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Sending the user home is the only thing Idle can still do when Android refuses to let
        // the block screen open, and only this service is allowed to do it.
        coordinator.foregroundController = ForegroundController { performGlobalAction(GLOBAL_ACTION_HOME) }
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == lastPackage) return

        lastPackage = packageName
        coordinator.onForegroundApp(packageName)
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        // The user turned the service off: stop counting rather than leaving a stale notification.
        lastPackage = null
        coordinator.foregroundController = null
        coordinator.onForegroundCleared()
        runCatching { unregisterReceiver(screenReceiver) }
        return super.onUnbind(intent)
    }
}
