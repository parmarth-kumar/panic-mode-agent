package com.panicmode

import android.content.Context

/**
 * Persistent runtime state for the Dead Man’s Switch (DMS) system.
 * Stores enablement, timing, and miss counters to allow alarm recovery
 * and UI continuity across process death and device idle.
 */
object DmsPreferences {
    private const val PREF_NAME = "dms_agent_memory"

    private const val KEY_DMS_ENABLED = "dms_enabled"
    private const val KEY_DMS_LAST_CONFIRMED = "dms_last_confirmed"
    private const val KEY_CHECK_INTERVAL_MINUTES = "dms_check_interval_minutes"
    private const val KEY_TIMEOUT_SECONDS = "dms_timeout_seconds"
    private const val KEY_MISSED_COUNT = "missed_count"

    // Persisted timestamps allow UI + alarms to reconstruct state after restarts
    private const val KEY_NEXT_CHECK_AT = "next_check_at"
    private const val KEY_NEXT_TIMEOUT_AT = "next_timeout_at"

    // Shared notification ID used to replace, not stack, safety notifications
    const val DMS_NOTIFICATION_ID = 999

    private const val DEFAULT_CHECK_INTERVAL_MINUTES = 30L
    private const val DEFAULT_TIMEOUT_SECONDS = 600L

    private fun prefs(c: Context) =
        c.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun enableDms(context: Context) {
        prefs(context).edit().apply {
            putBoolean(KEY_DMS_ENABLED, true)
            putLong(KEY_DMS_LAST_CONFIRMED, System.currentTimeMillis())
            putInt(KEY_MISSED_COUNT, 0)
            apply()
        }
    }

    fun setEnabled(c: Context, enabled: Boolean) {
        prefs(c).edit().putBoolean(KEY_DMS_ENABLED, enabled).apply()
    }

    fun clearRuntimeState(context: Context) {
        // Clears only volatile DMS state; configuration values are preserved
        prefs(context).edit().apply {
            remove(KEY_DMS_ENABLED)
            remove(KEY_DMS_LAST_CONFIRMED)
            remove(KEY_MISSED_COUNT)
            remove(KEY_NEXT_CHECK_AT)
            remove(KEY_NEXT_TIMEOUT_AT)
            apply()
        }
    }

    fun isDmsEnabled(c: Context) =
        prefs(c).getBoolean(KEY_DMS_ENABLED, false)

    fun confirmCheck(c: Context) {
        prefs(c).edit().apply {
            putLong(KEY_DMS_LAST_CONFIRMED, System.currentTimeMillis())
            putInt(KEY_MISSED_COUNT, 0)
            putLong(KEY_NEXT_TIMEOUT_AT, 0L) // Explicitly clear timeout on confirm
            apply()
        }
    }

    fun getLastConfirmedTime(c: Context) =
        prefs(c).getLong(KEY_DMS_LAST_CONFIRMED, 0)

    fun getCheckIntervalMinutes(c: Context) =
        prefs(c).getLong(KEY_CHECK_INTERVAL_MINUTES, DEFAULT_CHECK_INTERVAL_MINUTES)

    fun getTimeoutSeconds(c: Context) =
        prefs(c).getLong(KEY_TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECONDS)

    fun setCheckIntervalMinutes(c: Context, v: Long) {
        prefs(c).edit().putLong(KEY_CHECK_INTERVAL_MINUTES, v).apply()
    }

    fun setTimeoutSeconds(c: Context, v: Long) {
        prefs(c).edit().putLong(KEY_TIMEOUT_SECONDS, v).apply()
    }

    fun incrementMissed(c: Context) {
        val m = prefs(c).getInt(KEY_MISSED_COUNT, 0) + 1
        prefs(c).edit().putInt(KEY_MISSED_COUNT, m).apply()
    }

    fun getMissed(c: Context) =
        prefs(c).getInt(KEY_MISSED_COUNT, 0)

    fun setNextCheckAt(c: Context, timestamp: Long) {
        prefs(c).edit().putLong(KEY_NEXT_CHECK_AT, timestamp).apply()
    }

    fun getNextCheckAt(c: Context) =
        prefs(c).getLong(KEY_NEXT_CHECK_AT, 0L)

    fun setNextTimeoutAt(c: Context, timestamp: Long) {
        prefs(c).edit().putLong(KEY_NEXT_TIMEOUT_AT, timestamp).apply()
    }

    fun getNextTimeoutAt(c: Context) =
        prefs(c).getLong(KEY_NEXT_TIMEOUT_AT, 0L)
}
