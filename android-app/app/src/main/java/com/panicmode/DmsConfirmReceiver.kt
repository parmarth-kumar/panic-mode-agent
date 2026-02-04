package com.panicmode

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Handles explicit user acknowledgment of a safety check from the notification.
 * Cancels any pending escalation and hands control back to DmsManager
 * to schedule the next verification cycle.
 */
class DmsConfirmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Accept only the explicit confirmation action
        if (intent.action != ACTION_CONFIRM_SAFETY) return

        Log.i("SAFETY", "✅ User confirmed safety via notification")
        AgentLog.log(context, AgentLog.Type.SAFETY, "User confirmed safety")

        // Clear active notification immediately to avoid duplicate interaction
        DmsPreferences.confirmCheck(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(DmsPreferences.DMS_NOTIFICATION_ID)

        // Delegate scheduling and state updates to the central DMS controller
        DmsManager.confirmUserSafety(context)
    }

    companion object {
        const val ACTION_CONFIRM_SAFETY = "com.panicmode.DMS_CONFIRM_SAFETY"
    }
}
