package com.panicmode

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat

class SmsReceiver : BroadcastReceiver() {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val triggerPhrase = PanicPreferences.getTrigger(context)
        val trustedContact = PanicPreferences.getContact(context)

        for (msg in messages) {
            val body = msg.messageBody ?: continue
            val sender = msg.originatingAddress ?: ""

            // 🛑 SENDER VALIDATION
            // We strip non-digits to compare numbers safely (avoids +91 issues)
            val cleanSender = sender.replace(Regex("[^0-9]"), "")
            val cleanTrusted = trustedContact.replace(Regex("[^0-9]"), "")

            // Allow if sender contains trusted
            val isAuthorized = cleanTrusted.isNotEmpty() && cleanSender.contains(cleanTrusted.takeLast(10))

            if (isAuthorized && body.contains(triggerPhrase, ignoreCase = true)) {
                Log.i("PanicMode", "🚨 AUTHORIZED TRIGGER DETECTED")
                val serviceIntent = Intent(context, PanicService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
            } else if (!isAuthorized && body.contains(triggerPhrase, ignoreCase = true)) {
                Log.w("PanicMode", "⛔ UNAUTHORIZED TRIGGER ATTEMPT from $sender")
            }
        }
    }
}