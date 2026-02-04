package com.panicmode

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/**
 * Minimal foreground service used solely to keep the DMS alarm system alive.
 * Holds process priority during long idle periods so scheduled safety checks
 * are not suppressed by the OS.
 */
class DmsKeepAliveService : Service() {

    override fun onCreate() {
        super.onCreate()

        // Foreground notification channel (low importance, no user noise)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "dms_keepalive",
                "SAFETY Monitor",
                NotificationManager.IMPORTANCE_MIN
            )
            getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }

        val builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                Notification.Builder(this, "dms_keepalive")
            else
                Notification.Builder(this)

        val notif = builder
            .setContentTitle("Safety checks active")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .build()

        // Android 10+ requires explicit foreground service type
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                2,
                notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(2, notif)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
