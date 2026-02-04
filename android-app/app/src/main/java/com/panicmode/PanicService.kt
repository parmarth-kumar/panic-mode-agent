package com.panicmode

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log

/**
 * Foreground service that represents the active Panic Agent lifecycle.
 *
 * Responsible for:
 * - Maintaining foreground priority while panic mode is active
 * - Applying system-level survival policies
 * - Exposing real-time agent state via persistent notification
 */
class PanicService : Service() {

    companion object {
        const val CHANNEL_ID = "panic_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_SUSPEND = "panic.ACTION_SUSPEND"
    }

    override fun onCreate() {
        super.onCreate()
        // Channel is created once; reused for all agent notifications
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // -----------------------------
        // ⏸️ SUSPEND HANDLING
        // -----------------------------
        if (intent?.action == ACTION_SUSPEND) {
            updateForegroundNotification("SUSPENDED", 0)
            AgentLog.log(this, AgentLog.Type.AGENT, "Suspended")
            return START_STICKY
        }

        // -----------------------------
        // 🚨 PANIC ACTIVATION
        // -----------------------------
        PanicPreferences.setPanicActive(this, true)
        Log.i("PanicService", "🟢 PANIC_ACTIVE flag set to true")

        val notification = buildNotification(
            title = "🛡️ Panic Agent Active",
            text = "Initializing survival strategy…"
        )

        // -----------------------------
        // ⭐ FOREGROUND START (API-SAFE)
        // -----------------------------
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(
                NOTIFICATION_ID,
                notification
            )
        }

        // -----------------------------
        // 🔆 ONE-TIME POLICY APPLICATION
        // -----------------------------
        // Applied once per activation to conserve power and stabilize runtime
        PolicyManager.reduceBrightness(this)
        val policy = PolicyManager.applyPolicy(this)

        updateForegroundNotification(
            policy.mode,
            policy.intervalMinutes
        )

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // -----------------------------
        // 🔴 CLEAN SHUTDOWN
        // -----------------------------
        PanicPreferences.setPanicActive(this, false)
        Log.i("PanicService", "🔴 PANIC_ACTIVE flag set to false")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // -----------------------------
    // 🔔 FOREGROUND NOTIFICATION
    // -----------------------------
    fun updateForegroundNotification(mode: String, intervalMinutes: Long) {
        val (title, text) = if (mode == "SUSPENDED") {
            "⏸️ Panic Agent Suspended" to
                    "Heartbeats paused • Awaiting re-activation"
        } else {
            "🛡️ Panic Agent Active" to
                    "Mode: $mode • Update every $intervalMinutes min"
        }

        val notification = buildNotification(title, text)
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(title: String, text: String): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Panic Agent",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
