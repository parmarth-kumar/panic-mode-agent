package com.panicmode

import android.content.Context
import android.telephony.SmsManager
import android.util.Log

object SmsSender {

    // Now accept 'targetNumber' as an argument
    fun send(context: Context, message: String, targetNumber: String) {
        Log.i("PanicMode", "📤 SENDING SMS to $targetNumber")

        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            val parts = smsManager.divideMessage(message)

            smsManager.sendMultipartTextMessage(
                targetNumber, null, parts, null, null
            )
            Log.i("PanicMode", "✅ SMS SENT")

        } catch (e: Exception) {
            Log.e("PanicMode", "❌ SMS FAILED", e)
        }
    }
}