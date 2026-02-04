package com.panicmode

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles expiration of a safety-check response window.
 * Escalates by notifying the trusted contact and schedules the next
 * verification cycle, while guarding against duplicate or stale triggers.
 */
class DmsTimeoutReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Hard guards prevent ghost timeouts after disable or reschedule
        if (intent.action != DmsManager.ACTION_DMS_TIMEOUT) return
        if (!DmsPreferences.isDmsEnabled(context)) return

        // Use async execution to safely perform I/O beyond receiver lifetime
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Miss count is incremented only after all validation passes
                DmsPreferences.incrementMissed(context)

                val contact = PanicPreferences.getContact(context)
                if (contact.isNotBlank()) {
                    val location = DmsHelper.getLocationSafely(context)
                    val msg = DmsHelper.buildEscalationMessage(context, location)

                    AgentLog.log(
                        context,
                        AgentLog.Type.SAFETY,
                        "Timeout reached → escalating"
                    )

                    SmsSender.send(context, msg, contact)

                    AgentLog.log(
                        context,
                        AgentLog.Type.SAFETY,
                        "Escalation sent to $contact"
                    )
                }

                // Continue the DMS cycle regardless of escalation outcome
                DmsManager.scheduleNextCheckFromNow(context)

            } catch (e: Exception) {
                AgentLog.log(
                    context,
                    AgentLog.Type.SAFETY,
                    "Error in timeout: ${e.message}"
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
