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

/**
 * Periodic heartbeat worker executed while panic mode is active.
 * Collects best-effort context (location, battery, intent), sends a status SMS,
 * and records agent activity without blocking or hard-failing.
 */
class LocationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {

        // Heartbeats are paused while the agent is explicitly suspended
        if (PanicPreferences.isSuspended(applicationContext)) {
            return Result.success()
        }

        Log.i("PanicMode", "📍 LOCATION WORKER STARTED")

        // Location is optional; skip gracefully if permission is missing
        if (
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("PanicMode", "❌ Location permission missing")
            return Result.success()
        }

        val fusedClient =
            LocationServices.getFusedLocationProviderClient(applicationContext)

        val cancellationToken = CancellationTokenSource()

        var finalLocation: Location? = null
        var locationSource = "Searching…"

        // -----------------------------
        // 📡 LIVE LOCATION
        // -----------------------------
        try {
            val task = fusedClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationToken.token
            )
            finalLocation = Tasks.await(task, 5, TimeUnit.SECONDS)
            if (finalLocation != null) {
                locationSource = "GPS / Network (Live)"
            }
        } catch (_: Exception) {}

        // -----------------------------
        // 🗂️ LAST KNOWN
        // -----------------------------
        if (finalLocation == null) {
            try {
                finalLocation =
                    Tasks.await(fusedClient.lastLocation, 2, TimeUnit.SECONDS)
                if (finalLocation != null) {
                    locationSource = "Last Known (Cached)"
                }
            } catch (_: Exception) {}
        }

        val battery = getBatteryLevel()
        val trustedContact = PanicPreferences.getContact(applicationContext)

        // -----------------------------
        // 🧊 COLD START DETECTION
        // -----------------------------
        val lastHeartbeat = HeartbeatPrefs.getLastSent(applicationContext)
        val isColdStart = lastHeartbeat == 0L

        HeartbeatPrefs.setLastSent(
            applicationContext,
            System.currentTimeMillis()
        )

        // -----------------------------
        // 📊 CONFIDENCE (DIAGNOSTIC ONLY)
        // -----------------------------
        val confidence = ConfidenceModel.calculate(
            hasLocation = finalLocation != null,
            isLive = locationSource.contains("Live"),
            batteryPct = battery,
            isColdStart = isColdStart
        )

        AgentLog.log(
            applicationContext,
            AgentLog.Type.AGENT,
            "Heartbeat sent ($locationSource, confidence=${confidence.score}%)"
        )

        // -----------------------------
        // 📩 STATUS SMS
        // -----------------------------
        val message =
            if (finalLocation != null) {
                buildPanicMessage(finalLocation, locationSource)
            } else {
                buildPanicMessageWithoutLocation(locationSource)
            }

        SmsSender.send(applicationContext, message, trustedContact)

        return Result.success()
    }

    // -----------------------------
    // 📍 WITH LOCATION
    // -----------------------------
    private fun buildPanicMessage(
        location: Location,
        source: String
    ): String {

        val time =
            SimpleDateFormat("HH:mm z", Locale.getDefault()).format(Date())

        val battery = getBatteryLevel()
        val capacity = PanicPreferences.getCapacity(applicationContext)
        val remainingMah = (battery / 100.0) * capacity

        val mapLink =
            "https://maps.google.com/?q=${location.latitude},${location.longitude}"

        val rawIntent =
            PanicPreferences.getUserIntent(applicationContext)

        val isSurvivalContext =
            rawIntent == "TRAVELING" ||
                    rawIntent == "SAVE_BATTERY" ||
                    rawIntent == "AGGRESSIVE" ||
                    battery < 20

        val strategyDisplay =
            if (isSurvivalContext) "SURVIVAL" else "VISIBILITY"

        val intervalDisplay =
            if (isSurvivalContext) "60 min" else "15 min"

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

    // -----------------------------
    // 🚫 NO LOCATION
    // -----------------------------
    private fun buildPanicMessageWithoutLocation(
        source: String
    ): String {

        val time =
            SimpleDateFormat("HH:mm z", Locale.getDefault()).format(Date())

        val battery = getBatteryLevel()
        val capacity = PanicPreferences.getCapacity(applicationContext)
        val remainingMah = (battery / 100.0) * capacity

        val rawIntent =
            PanicPreferences.getUserIntent(applicationContext)

        val isSurvivalContext =
            rawIntent == "TRAVELING" ||
                    rawIntent == "SAVE_BATTERY" ||
                    rawIntent == "AGGRESSIVE" ||
                    battery < 20

        val strategyDisplay =
            if (isSurvivalContext) "SURVIVAL" else "VISIBILITY"

        val intervalDisplay =
            if (isSurvivalContext) "60 min" else "15 min"

        return """
🚨 PANIC MODE ACTIVE
🕒 $time | 🔋 $battery%
⚡ Est. Power: ${remainingMah.toInt()} mAh

📍 Location unavailable (searching…)

🧠 Agent State:
Intent: $rawIntent
Mode: $strategyDisplay
Interval: $intervalDisplay

🛰️ Source: $source
""".trimIndent()
    }

    // -----------------------------
    // 🔋 BATTERY
    // -----------------------------
    private fun getBatteryLevel(): Int {
        val status = applicationContext.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val level =
            status?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale =
            status?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        return if (level != -1 && scale != -1) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            0
        }
    }
}
