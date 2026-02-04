package com.panicmode

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.provider.Settings
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Central decision engine for the Panic Agent.
 *
 * Converts user intent and runtime context (battery, suspension state)
 * into a concrete execution policy, schedules heartbeats,
 * and triggers immediate state updates.
 */
object PolicyManager {

    fun applyPolicy(context: Context): AgentPolicy {

        // Agent suspension short-circuits all policy logic
        if (PanicPreferences.isSuspended(context)) {
            return AgentPolicy("SUSPENDED", 0)
        }

        Log.i("PanicMode", "🧠 AGENT BRAIN ACTIVE")

        val batteryPct = getBatteryLevel(context)
        val rawIntent = PanicPreferences.getUserIntent(context)

        val mode = normalizeIntent(rawIntent)
        val policy = generatePolicy(mode, batteryPct)

        Log.i(
            "PanicMode",
            "📜 POLICY → Raw=$rawIntent | Mode=${policy.mode} | Interval=${policy.intervalMinutes}"
        )

        // NOTE: Brightness reduction is intentionally NOT done here.
        // It runs once during service start to avoid repeated system writes.
        scheduleHeartbeat(context, policy)
        triggerImmediateUpdate(context)

        AgentLog.log(
            context,
            AgentLog.Type.AGENT,
            "Policy applied → ${policy.mode} (${policy.intervalMinutes}m)"
        )

        return policy
    }

    // ---------- INTENT NORMALIZATION ----------

    // Maps user-facing intent into internal strategy buckets
    private fun normalizeIntent(raw: String): String {
        return when (raw) {
            "TRAVELING", "SAVE_BATTERY" -> "SURVIVAL"
            "CROWDED", "LOST" -> "VISIBILITY"
            "AGGRESSIVE" -> "AGGRESSIVE"
            else -> "ADAPTIVE"
        }
    }

    // ---------- POLICY DECISION ENGINE ----------

    // Determines heartbeat frequency based on intent + battery constraints
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

    // ---------- WORK SCHEDULING ----------

    // Schedules periodic heartbeat updates according to active policy
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

    // Triggers an immediate one-time update on policy changes
    private fun triggerImmediateUpdate(context: Context) {
        val work = OneTimeWorkRequestBuilder<LocationWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueue(work)
    }

    // ---------- SYSTEM CONTROLS ----------

    // Called once during panic activation to conserve power
    fun reduceBrightness(context: Context) {
        if (Settings.System.canWrite(context)) {
            try {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    10
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

/**
 * Immutable execution policy produced by the agent decision engine.
 */
data class AgentPolicy(
    val mode: String,
    val intervalMinutes: Long
)
