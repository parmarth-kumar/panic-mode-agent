package com.panicmode

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Helper utilities for Dead Man’s Switch escalation.
 * Focuses on best-effort signal collection and human-readable message
 * construction without blocking or hard-failing.
 */
object DmsHelper {

    fun getLocationSafely(context: Context): Location? {
        // Location is optional during escalation; fail soft if permission is missing
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return null

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val cancellationToken = CancellationTokenSource()

        var finalLocation: Location? = null

        // Prefer a fresh, high-accuracy fix but enforce a strict timeout
        try {
            val task = fusedClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationToken.token
            )
            finalLocation = Tasks.await(task, 8, TimeUnit.SECONDS)
        } catch (_: Exception) {}

        // Fallback to last-known location if live acquisition fails
        if (finalLocation == null) {
            try {
                finalLocation = Tasks.await(fusedClient.lastLocation, 2, TimeUnit.SECONDS)
            } catch (_: Exception) {}
        }

        return finalLocation
    }

    fun buildEscalationMessage(context: Context, location: Location?): String {
        val time = SimpleDateFormat("HH:mm z", Locale.getDefault()).format(Date())

        val map = location?.let {
            "https://www.google.com/maps/search/?api=1&query=${it.latitude},${it.longitude}"
        } ?: "(location unavailable)"

        val trigger = PanicPreferences.getTrigger(context)

        val batteryPct = getBattery(context)
        val batteryLine = if (batteryPct >= 0) "🔋 $batteryPct%" else "🔋 Battery unknown"

        return """
⚠️ User missed safety checks.
Try contacting them.
(NOT necessarily emergency)

🕒 $time | $batteryLine
📍 $map

to activate live tracking send:
$trigger

to pause tracking send:
$trigger-STOP
""".trimIndent()
    }

    // Lightweight battery snapshot used only for context in escalation messages
    private fun getBattery(context: Context): Int {
        val status = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = status?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = status?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level > 0 && scale > 0) (level * 100 / scale.toFloat()).toInt() else -1
    }
}
