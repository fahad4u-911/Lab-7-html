package com.intentlock

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Watches which app is in the foreground. When a guarded app appears, it asks
 * [LockOverlayController] to raise the pause screen; when the user moves away,
 * it tells the controller to reset. All social-blocking logic lives in the
 * controller — this class only turns window-change events into enter/leave.
 */
class AppMonitorService : AccessibilityService() {

    private lateinit var controller: LockOverlayController
    private lateinit var prefs: Prefs
    private var lastPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        controller = LockOverlayController(this)
        prefs = Prefs(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        // Ignore our own overlay and system chrome so the shade / keyboard don't
        // count as "leaving" the guarded app.
        if (pkg in TargetApps.NEUTRAL) return
        if (pkg == lastPackage) return
        lastPackage = pkg

        if (!prefs.protectionEnabled) {
            controller.onLeaveTarget()
            return
        }

        val target = TargetApps.match(pkg)
        if (target != null) {
            controller.onEnterTarget(target)
        } else {
            controller.onLeaveTarget()
        }
    }

    override fun onInterrupt() { /* no-op */ }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        if (this::controller.isInitialized) controller.destroy()
        return super.onUnbind(intent)
    }
}
