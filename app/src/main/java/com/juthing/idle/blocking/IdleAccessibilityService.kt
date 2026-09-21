package com.juthing.idle.blocking

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == lastPackage) return

        lastPackage = packageName
        coordinator.onForegroundApp(packageName)
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        // The user turned the service off: stop counting rather than leaving a stale notification.
        lastPackage = null
        coordinator.onForegroundCleared()
        return super.onUnbind(intent)
    }
}
