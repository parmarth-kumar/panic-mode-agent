package com.panicmode

import android.content.Context
import android.telephony.SmsManager
import android.util.Log

/**
 * Low-level SMS delivery utility used by the Panic Agent.
 * Handles message chunking and best-effort delivery without
 * blocking agent execution paths.
 */
object SmsSender {

    fun send(context: Context, message: String, targetNumber: String) {
        Log.i("PanicMode", "📤 Sending SMS to $targetNumber")

        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            val parts = smsManager.divideMessage(message)

            smsManager.sendMultipartTextMessage(
                targetNumber,
                null,
                parts,
                null,
                null
            )

            Log.i("PanicMode", "✅ SMS sent")

        } catch (e: Exception) {
            // Fail silently beyond logging; agent flow must continue
            Log.e("PanicMode", "❌ SMS failed", e)
        }
    }
}
