package com.panicmode

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.WorkManager

/**
 * SMS command entry point for the Panic Agent.
 *
 * Listens for activation and control commands from a trusted contact,
 * performs strict sender validation, and triggers agent lifecycle changes.
 */
class SmsReceiver : BroadcastReceiver() {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {

        // Agent must be explicitly armed to accept any SMS commands
        if (!PanicPreferences.isAgentArmed(context)) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val triggerPhrase = PanicPreferences.getTrigger(context)
        val trustedContact = PanicPreferences.getContact(context)

        for (msg in messages) {
            val body = msg.messageBody ?: continue
            val sender = msg.originatingAddress ?: ""

            // -----------------------------
            // 🔐 SENDER AUTHORIZATION
            // -----------------------------
            // Normalize numbers to digits-only to handle country codes
            val cleanSender = sender.replace(Regex("[^0-9]"), "")
            var cleanTrusted = trustedContact.replace(Regex("[^0-9]"), "")

            // Handle leading-zero national formats (e.g. 07xxxx → 447xxxx)
            if (cleanTrusted.startsWith("0") && cleanTrusted.length >= 7) {
                cleanTrusted = cleanTrusted.substring(1)
            }

            // Prevent weak / partial matches
            val isLengthSafe =
                cleanTrusted.length >= 7 &&
                        cleanSender.length >= cleanTrusted.length

            // Core rule: sender must END WITH trusted number
            val isAuthorized =
                isLengthSafe &&
                        cleanSender.endsWith(cleanTrusted)

            if (!isAuthorized) {
                if (body.contains(triggerPhrase, ignoreCase = true)) {
                    Log.w(
                        "PanicMode",
                        "⛔ Unauthorized SMS from $sender (trusted=$cleanTrusted)"
                    )
                }
                continue
            }

            // -----------------------------
            // ✅ AUTHORIZED COMMANDS
            // -----------------------------
            when {
                body.equals(triggerPhrase, ignoreCase = true) -> {
                    // Full panic activation
                    PanicPreferences.setPanicActive(context, true)
                    PanicPreferences.setSuspended(context, false)

                    AgentLog.log(context, AgentLog.Type.AGENT, "Activated via SMS")

                    val serviceIntent =
                        Intent(context, PanicService::class.java)
                    ContextCompat.startForegroundService(context, serviceIntent)

                    Log.i("PanicMode", "🚨 Panic mode activated via SMS")
                }

                body.equals("$triggerPhrase-STOP", ignoreCase = true) -> {
                    // Suspend heartbeats while keeping panic active
                    PanicPreferences.setSuspended(context, true)

                    WorkManager.getInstance(context)
                        .cancelUniqueWork("panic_heartbeat")

                    AgentLog.log(context, AgentLog.Type.AGENT, "Suspended via SMS")

                    val suspendIntent =
                        Intent(context, PanicService::class.java).apply {
                            action = PanicService.ACTION_SUSPEND
                        }

                    ContextCompat.startForegroundService(context, suspendIntent)

                    Log.i("PanicMode", "⏸️ Panic heartbeats suspended via SMS")
                }
            }
        }
    }
}
