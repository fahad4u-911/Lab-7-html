package com.intentlock

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.CountDownTimer
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView

/**
 * Draws (and tears down) the full-screen countdown that blocks a guarded app.
 *
 * Behaviour, driven entirely by [AppMonitorService]:
 *  - onEnterTarget:  show the overlay and start the pause countdown for that app.
 *  - onLeaveTarget:  the user bailed -> cancel + remove -> next visit starts fresh.
 *  - countdown hits 0: remove the overlay; the app is now usable until they leave.
 *
 * The overlay is a touchable window layered on top of everything, so the app
 * underneath cannot be interacted with while the timer runs. There is no "skip".
 */
class LockOverlayController(private val context: Context) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var overlayView: View? = null
    private var timer: CountDownTimer? = null
    private var activeKey: String? = null

    val isShowing: Boolean get() = overlayView != null

    fun onEnterTarget(app: TargetApp) {
        // Already counting down for this same app -> leave it running.
        if (activeKey == app.key && isShowing) return
        // Switched from another guarded app directly -> restart clean.
        teardown()
        if (!canDrawOverlay()) return
        activeKey = app.key
        showOverlay(app)
    }

    fun onLeaveTarget() {
        // Any exit from a guarded app resets the pause.
        teardown()
        activeKey = null
    }

    fun destroy() = teardown()

    private fun showOverlay(app: TargetApp) {
        val view = LayoutInflater.from(context).inflate(R.layout.overlay_lock, null)
        val title = view.findViewById<TextView>(R.id.lockAppName)
        val count = view.findViewById<TextView>(R.id.lockCountdown)

        title.text = context.getString(R.string.lock_title, app.label)
        view.findViewById<View>(R.id.lockGoHome).setOnClickListener { goHome() }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }

        windowManager.addView(view, params)
        overlayView = view

        val totalMs = app.minutesFor(context).toLong() * 60_000L
        count.text = format(totalMs)
        timer = object : CountDownTimer(totalMs, 1_000L) {
            override fun onTick(msLeft: Long) {
                count.text = format(msLeft)
            }
            override fun onFinish() {
                // Pause served -> let them through until they leave the app.
                teardown()
            }
        }.also { it.start() }
    }

    private fun teardown() {
        timer?.cancel()
        timer = null
        overlayView?.let {
            runCatching { windowManager.removeView(it) }
        }
        overlayView = null
    }

    private fun goHome() {
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(home) }
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ERROR

    private fun canDrawOverlay(): Boolean = Settings.canDrawOverlays(context)

    private fun TargetApp.minutesFor(ctx: Context): Int = Prefs(ctx).minutesFor(this)

    private fun format(ms: Long): String {
        val totalSec = (ms + 999L) / 1000L // round up so it starts at full minutes
        val m = totalSec / 60
        val s = totalSec % 60
        return "%02d:%02d".format(m, s)
    }
}
