package com.intentlock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Accessibility services are restarted by the system after a reboot, so there is
 * nothing to actively restart here; this receiver exists so the app has a hook
 * if boot-time setup is ever needed, and to keep protection state warm.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Prefs already persist; nothing required. Touch Prefs to ensure init.
            Prefs(context).protectionEnabled
        }
    }
}
