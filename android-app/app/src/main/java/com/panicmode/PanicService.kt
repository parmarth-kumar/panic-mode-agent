package com.panicmode

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder

class PanicService : Service() {

    companion object {
        const val CHANNEL_ID = "panic_channel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Initial foreground notification (minimal)
        startForeground(
            NOTIFICATION_ID,
            buildNotification(
                title = "🛡️ Panic Agent Active",
                text = "Initializing survival strategy…"
            )
        )

        // Apply agent logic AFTER foreground promotion
        PolicyManager.applyPolicy(this)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // 🔥 THIS is the ONLY correct way to update a foreground notification
    fun updateForegroundNotification(mode: String, intervalMinutes: Long) {
        val notification = buildNotification(
            title = "🛡️ Panic Agent Active",
            text = "Mode: $mode • Update every $intervalMinutes min"
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
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
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Panic Agent",
            NotificationManager.IMPORTANCE_DEFAULT // DO NOT use LOW
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
