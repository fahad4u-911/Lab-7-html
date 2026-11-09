package com.intentlock

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Setup / dashboard screen. Walks the user through the three permissions the
 * guard needs, shows guarded apps, and exposes the deliberately slow
 * "turn protection off" off-ramp.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private val adminComponent by lazy { ComponentName(this, AdminReceiver::class.java) }
    private val dpm by lazy { getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)

        findViewById<TextView>(R.id.guardedApps).text = buildString {
            TargetApps.ALL.forEach { app ->
                append("• ${app.label}  —  ${prefs.minutesFor(app)} min pause\n")
            }
        }.trimEnd()

        findViewById<Button>(R.id.btnOverlay).setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
        findViewById<Button>(R.id.btnAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            toast(getString(R.string.hint_enable_accessibility))
        }
        findViewById<Button>(R.id.btnAdmin).setOnClickListener {
            if (dpm.isAdminActive(adminComponent)) {
                toast(getString(R.string.admin_already_on))
            } else {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                    putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        getString(R.string.admin_explanation)
                    )
                }
                startActivity(intent)
            }
        }
        findViewById<Button>(R.id.btnDisable).setOnClickListener { onDisableClicked() }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        bindRow(
            R.id.statusOverlay, R.id.btnOverlay,
            Settings.canDrawOverlays(this),
            getString(R.string.status_overlay_on),
            getString(R.string.status_overlay_off)
        )
        bindRow(
            R.id.statusAccessibility, R.id.btnAccessibility,
            isAccessibilityEnabled(),
            getString(R.string.status_accessibility_on),
            getString(R.string.status_accessibility_off)
        )
        bindRow(
            R.id.statusAdmin, R.id.btnAdmin,
            dpm.isAdminActive(adminComponent),
            getString(R.string.status_admin_on),
            getString(R.string.status_admin_off)
        )

        val allOn = Settings.canDrawOverlays(this) &&
            isAccessibilityEnabled() &&
            prefs.protectionEnabled
        val banner = findViewById<TextView>(R.id.banner)
        if (allOn) {
            banner.text = getString(R.string.banner_active)
            banner.setBackgroundColor(ContextCompat.getColor(this, R.color.ok))
        } else {
            banner.text = getString(R.string.banner_inactive)
            banner.setBackgroundColor(ContextCompat.getColor(this, R.color.warn))
        }
    }

    private fun bindRow(statusId: Int, buttonId: Int, on: Boolean, onText: String, offText: String) {
        val status = findViewById<TextView>(statusId)
        status.text = if (on) onText else offText
        status.setTextColor(
            ContextCompat.getColor(this, if (on) R.color.ok else R.color.warn)
        )
        findViewById<Button>(buttonId).isEnabled = !on || buttonId == R.id.btnAccessibility
    }

    private fun onDisableClicked() {
        val requestedAt = prefs.disableRequestedAt
        val now = System.currentTimeMillis()

        if (requestedAt == 0L) {
            AlertDialog.Builder(this)
                .setTitle(R.string.disable_title)
                .setMessage(R.string.disable_explain)
                .setPositiveButton(R.string.disable_start) { _, _ ->
                    prefs.disableRequestedAt = now
                    refreshDisableLabel()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
            return
        }

        val elapsed = now - requestedAt
        if (elapsed < Prefs.DISABLE_COOLDOWN_MS) {
            val remaining = Prefs.DISABLE_COOLDOWN_MS - elapsed
            toast(
                getString(
                    R.string.disable_wait,
                    DateUtils.formatElapsedTime(remaining / 1000)
                )
            )
        } else {
            prefs.protectionEnabled = false
            prefs.disableRequestedAt = 0L
            if (dpm.isAdminActive(adminComponent)) dpm.removeActiveAdmin(adminComponent)
            toast(getString(R.string.disable_done))
            refreshStatus()
        }
        refreshDisableLabel()
    }

    private fun refreshDisableLabel() {
        val btn = findViewById<Button>(R.id.btnDisable)
        val requestedAt = prefs.disableRequestedAt
        btn.text = when {
            !prefs.protectionEnabled -> getString(R.string.disable_reenable)
            requestedAt == 0L -> getString(R.string.disable_button)
            else -> getString(R.string.disable_pending)
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val expected = ComponentName(this, AppMonitorService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
