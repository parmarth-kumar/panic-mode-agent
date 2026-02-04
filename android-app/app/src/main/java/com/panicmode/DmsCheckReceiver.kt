package com.panicmode

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

/**
 * Alarm-driven receiver that issues a safety check notification
 * and arms the corresponding response timeout.
 * Strict action validation prevents unintended or duplicate triggers.
 */
class DmsCheckReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Hard guard: only accept the exact scheduled DMS check action
        if (intent.action != DmsManager.ACTION_DMS_CHECK) return

        // Safety system may be disabled between scheduling and delivery
        if (!DmsPreferences.isDmsEnabled(context)) return

        showNotification(context)
        DmsManager.scheduleTimeout(context)
    }

    private fun showNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val missed = DmsPreferences.getMissed(context)

        val intent = Intent(context, DmsConfirmReceiver::class.java).apply {
            action = DmsConfirmReceiver.ACTION_CONFIRM_SAFETY
        }

        // Stable PendingIntent identity ensures reliable action handling
        val pending = PendingIntent.getBroadcast(
            context,
            9991,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (missed > 0)
            "⚠️ $missed safety checks missed"
        else
            "Quick Safety Check"

        val text = "Confirm 'I'm OK' within ${DmsPreferences.getTimeoutSeconds(context)}s"

        val notification = NotificationCompat.Builder(context, PanicService.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .addAction(
                android.R.drawable.ic_dialog_info,
                "I'm OK",
                pending
            )
            .build()

        nm.notify(DmsPreferences.DMS_NOTIFICATION_ID, notification)

        AgentLog.log(context, AgentLog.Type.SAFETY, "Check issued")
    }
}
