package com.intentlock

import android.content.Context

/**
 * Thin wrapper over SharedPreferences for the handful of values we persist:
 * per-app pause durations, the master on/off flag, and the "disable protection"
 * cooldown timestamp used as a deliberate high-friction off-ramp.
 */
class Prefs(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("intentlock", Context.MODE_PRIVATE)

    var protectionEnabled: Boolean
        get() = sp.getBoolean(KEY_ENABLED, true)
        set(value) = sp.edit().putBoolean(KEY_ENABLED, value).apply()

    fun minutesFor(app: TargetApp): Int =
        sp.getInt("minutes_${app.key}", app.defaultMinutes)

    fun setMinutesFor(app: TargetApp, minutes: Int) =
        sp.edit().putInt("minutes_${app.key}", minutes.coerceIn(1, 180)).apply()

    /** 0 means no disable request is pending. */
    var disableRequestedAt: Long
        get() = sp.getLong(KEY_DISABLE_AT, 0L)
        set(value) = sp.edit().putLong(KEY_DISABLE_AT, value).apply()

    companion object {
        private const val KEY_ENABLED = "protection_enabled"
        private const val KEY_DISABLE_AT = "disable_requested_at"

        /** How long the user must wait after requesting to turn protection off. */
        const val DISABLE_COOLDOWN_MS = 24L * 60L * 60L * 1000L
    }
}
