package com.panicmode

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Central controller for the Dead Man’s Switch (DMS) system.
 * Owns all alarm scheduling, cancellation symmetry, and lifecycle coordination
 * to ensure predictable behavior across OS versions and idle states.
 */
object DmsManager {

    private const val REQ_CHECK = 8000
    private const val REQ_TIMEOUT = 8001

    // Explicit action strings ensure stable PendingIntent identity and safe cancellation
    const val ACTION_DMS_CHECK = "com.panicmode.ACTION_DMS_CHECK"
    const val ACTION_DMS_TIMEOUT = "com.panicmode.ACTION_DMS_TIMEOUT"

    private fun alarm(c: Context) =
        c.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // PendingIntent must match exactly (action + requestCode) for reliable cancel()
    private fun pending(c: Context, action: String, receiver: Class<*>, req: Int): PendingIntent =
        PendingIntent.getBroadcast(
            c,
            req,
            Intent(c, receiver).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun setExact(c: Context, pi: PendingIntent, at: Long) {
        val am = alarm(c)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                // Graceful degradation when exact alarm permission is missing
                am.setWindow(AlarmManager.RTC_WAKEUP, at, 600_000, pi)
                AgentLog.log(c, AgentLog.Type.SAFETY, "Exact alarm permission missing, using inexact")
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun ensureChannel(c: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = c.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                PanicService.CHANNEL_ID,
                "Panic Agent",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm?.createNotificationChannel(channel)
        }
    }

    fun enableDms(c: Context) {
        ensureChannel(c)

        // Foreground service prevents OS from suppressing long idle alarms
        ContextCompat.startForegroundService(
            c,
            Intent(c, DmsKeepAliveService::class.java)
        )

        DmsPreferences.enableDms(c)
        AgentLog.log(c, AgentLog.Type.SAFETY, "Enabled → first check scheduled")

        val now = System.currentTimeMillis()
        DmsPreferences.setNextCheckAt(c, now)

        val pi = pending(c, ACTION_DMS_CHECK, DmsCheckReceiver::class.java, REQ_CHECK)
        setExact(c, pi, now)
    }

    fun disableDms(c: Context) {
        // Cancellation must mirror scheduling exactly to avoid orphaned alarms
        alarm(c).cancel(pending(c, ACTION_DMS_CHECK, DmsCheckReceiver::class.java, REQ_CHECK))
        alarm(c).cancel(pending(c, ACTION_DMS_TIMEOUT, DmsTimeoutReceiver::class.java, REQ_TIMEOUT))

        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(DmsPreferences.DMS_NOTIFICATION_ID)

        c.stopService(Intent(c, DmsKeepAliveService::class.java))

        DmsPreferences.clearRuntimeState(c)
        AgentLog.log(c, AgentLog.Type.SAFETY, "Disabled → alarms cleared")
    }

    fun confirmUserSafety(c: Context) {
        // Cancel any armed timeout before scheduling the next cycle
        alarm(c).cancel(
            pending(c, ACTION_DMS_TIMEOUT, DmsTimeoutReceiver::class.java, REQ_TIMEOUT)
        )

        DmsPreferences.confirmCheck(c)
        scheduleNextCheckAbsolute(c)

        AgentLog.log(c, AgentLog.Type.SAFETY, "Safety confirmed → timeout canceled")
    }

    fun scheduleNextCheckAbsolute(c: Context) {
        val last = DmsPreferences.getLastConfirmedTime(c)
        val interval = DmsPreferences.getCheckIntervalMinutes(c) * 60 * 1000
        val next = last + interval

        DmsPreferences.setNextCheckAt(c, next)
        DmsPreferences.setNextTimeoutAt(c, 0L)

        val pi = pending(c, ACTION_DMS_CHECK, DmsCheckReceiver::class.java, REQ_CHECK)
        setExact(c, pi, next)

        AgentLog.log(c, AgentLog.Type.SAFETY, "Next check scheduled")
    }

    fun scheduleNextCheckFromNow(c: Context) {
        val interval = DmsPreferences.getCheckIntervalMinutes(c) * 60 * 1000
        val next = System.currentTimeMillis() + interval

        DmsPreferences.setNextCheckAt(c, next)
        DmsPreferences.setNextTimeoutAt(c, 0L)

        val pi = pending(c, ACTION_DMS_CHECK, DmsCheckReceiver::class.java, REQ_CHECK)
        setExact(c, pi, next)

        AgentLog.log(c, AgentLog.Type.SAFETY, "Next check scheduled")
    }

    fun scheduleTimeout(c: Context) {
        val timeout = DmsPreferences.getTimeoutSeconds(c) * 1000
        val at = System.currentTimeMillis() + timeout

        DmsPreferences.setNextTimeoutAt(c, at)

        val pi = pending(c, ACTION_DMS_TIMEOUT, DmsTimeoutReceiver::class.java, REQ_TIMEOUT)
        setExact(c, pi, at)

        AgentLog.log(c, AgentLog.Type.SAFETY, "Timeout armed")
    }

    fun hardKillAll(c: Context) {
        disableDms(c)
        AgentLog.log(c, AgentLog.Type.SAFETY, "Force stopped")
    }
}
