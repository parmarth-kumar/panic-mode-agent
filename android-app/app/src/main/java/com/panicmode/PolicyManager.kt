package com.panicmode

import android.content.Context
import android.content.IntentFilter
import android.os.BatteryManager
import android.provider.Settings
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit
import android.content.Intent

object PolicyManager {

    fun applyPolicy(context: Context) {
        Log.i("PanicMode", "🧠 AGENT BRAIN ACTIVE")

        val batteryPct = getBatteryLevel(context)
        val rawIntent = PanicPreferences.getUserIntent(context)

        val mode = normalizeIntent(rawIntent)
        val policy = generatePolicy(mode, batteryPct)

        Log.i(
            "PanicMode",
            "📜 POLICY → Raw=$rawIntent | Mode=${policy.mode} | Interval=${policy.intervalMinutes}"
        )

        reduceBrightness(context)

        // 🔥 CRITICAL: Update foreground notification via service
        (context as? PanicService)
            ?.updateForegroundNotification(policy.mode, policy.intervalMinutes)

        scheduleHeartbeat(context, policy)
        triggerImmediateUpdate(context)
    }

    // ---------- Intent Normalization ----------

    private fun normalizeIntent(raw: String): String {
        return when (raw) {
            "TRAVELING", "SAVE_BATTERY" -> "SURVIVAL"
            "CROWDED", "LOST" -> "VISIBILITY"
            "AGGRESSIVE" -> "AGGRESSIVE"
            else -> "ADAPTIVE"
        }
    }

    // ---------- Policy Decision Engine ----------

    private fun generatePolicy(mode: String, batteryPct: Int): AgentPolicy {
        if (batteryPct < 15) {
            return AgentPolicy("SURVIVAL (Critical Battery)", 60)
        }

        return when (mode) {
            "SURVIVAL" -> AgentPolicy("SURVIVAL", 60)
            "VISIBILITY" -> AgentPolicy("VISIBILITY", 15)
            "AGGRESSIVE" -> AgentPolicy("AGGRESSIVE", 15)
            else -> {
                if (batteryPct > 30)
                    AgentPolicy("ADAPTIVE (High Battery)", 15)
                else
                    AgentPolicy("ADAPTIVE (Low Battery)", 60)
            }
        }
    }

    // ---------- Work Scheduling ----------

    private fun scheduleHeartbeat(context: Context, policy: AgentPolicy) {
        val work = PeriodicWorkRequestBuilder<LocationWorker>(
            policy.intervalMinutes,
            TimeUnit.MINUTES
        )
            .setInitialDelay(policy.intervalMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "panic_heartbeat",
            ExistingPeriodicWorkPolicy.REPLACE,
            work
        )
    }

    private fun triggerImmediateUpdate(context: Context) {
        val work = OneTimeWorkRequestBuilder<LocationWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueue(work)
    }

    // ---------- System Controls ----------

    private fun reduceBrightness(context: Context) {
        if (Settings.System.canWrite(context)) {
            try {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    0
                )
            } catch (e: Exception) {
                Log.e("PanicMode", "Brightness control failed", e)
            }
        }
    }

    private fun getBatteryLevel(context: Context): Int {
        val status = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val level = status?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = status?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        return if (level > 0 && scale > 0) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            50
        }
    }
}

data class AgentPolicy(
    val mode: String,
    val intervalMinutes: Long
)
