package com.panicmode

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.BatteryManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class LocationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        Log.i("PanicMode", "📍 WORKER EXECUTION STARTED")

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return Result.success()

        val fusedClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        val cancellationToken = CancellationTokenSource()
        var finalLocation: Location? = null
        var locationSource = "Searching..."

        // Strategy 1: Fresh Location
        try {
            val locationTask = fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationToken.token)
            finalLocation = Tasks.await(locationTask, 5, TimeUnit.SECONDS)
            if(finalLocation != null) locationSource = "GPS/Network (Live)"
        } catch (_: Exception) {}

        // Strategy 2: Last Known
        if (finalLocation == null) {
            try { finalLocation = Tasks.await(fusedClient.lastLocation, 2, TimeUnit.SECONDS)
                if(finalLocation != null) locationSource = "Last Known (Cached)" } catch (_: Exception) {}
        }

        val trustedContact = PanicPreferences.getContact(applicationContext)

        if (finalLocation != null) {
            val message = buildPanicMessage(finalLocation, locationSource)
            SmsSender.send(applicationContext, message, trustedContact)
        } else {
            SmsSender.send(applicationContext, "🚨 PANIC ACTIVE\nSearching for signal...", trustedContact)
        }

        return Result.success()
    }

    private fun buildPanicMessage(location: Location, source: String): String {
        val time = SimpleDateFormat("HH:mm z", Locale.getDefault()).format(Date())
        val battery = getBatteryLevel()
        val mapLink = "https://maps.google.com/?q=${location.latitude},${location.longitude}"

        // mAh Calculation
        val capacity = PanicPreferences.getCapacity(applicationContext)
        val remainingMah = (battery / 100.0) * capacity

        val rawIntent = PanicPreferences.getUserIntent(applicationContext)

        // Logic Re-Check for Display
        val isSurvivalContext = rawIntent == "TRAVELING" || rawIntent == "SAVE_BATTERY" || rawIntent == "AGGRESSIVE" || battery < 20


        val strategyDisplay = if (isSurvivalContext) "SURVIVAL" else "VISIBILITY"
        val intervalDisplay = if (isSurvivalContext) "60 min" else "15 min"

        return """
🚨 PANIC MODE ACTIVE
🕒 $time | 🔋 $battery%
⚡ Est. Power: ${remainingMah.toInt()} mAh

📍 $mapLink

🧠 Agent State:
Intent: $rawIntent
Mode: $strategyDisplay
Interval: $intervalDisplay

🛰️ Source: $source
""".trimIndent()
    }

    private fun getBatteryLevel(): Int {
        val status = applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = status?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = status?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 0
    }
}