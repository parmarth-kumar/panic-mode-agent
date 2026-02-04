package com.panicmode

import android.content.Context

/**
 * Persists the timestamp of the last successful heartbeat.
 * Used to detect cold starts and adjust confidence scoring
 * without coupling worker logic to runtime memory.
 */
object HeartbeatPrefs {

    private const val PREF = "heartbeat_prefs"
    private const val KEY_LAST_SENT = "last_sent"

    private fun p(c: Context) =
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun getLastSent(c: Context): Long =
        p(c).getLong(KEY_LAST_SENT, 0L)

    fun setLastSent(c: Context, ts: Long) {
        p(c).edit().putLong(KEY_LAST_SENT, ts).apply()
    }

    fun clear(c: Context) {
        p(c).edit().clear().apply()
    }
}
