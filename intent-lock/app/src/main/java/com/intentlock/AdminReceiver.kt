package com.intentlock

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Being an active Device Admin means the app cannot be uninstalled until the
 * user first deactivates admin in Settings — that extra, deliberate step is the
 * friction that stops an impulsive delete. It is NOT a permanent lock: the user
 * always remains the owner of their own device.
 */
class AdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, R.string.admin_enabled_toast, Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return context.getString(R.string.admin_disable_warning)
    }
}
