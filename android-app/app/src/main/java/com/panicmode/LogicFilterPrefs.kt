package com.panicmode

import android.content.Context

/**
 * Persists user-selected filters for the activity log view.
 * Allows log visibility preferences to survive configuration
 * changes and process restarts.
 */
object LogFilterPrefs {

    private const val PREF = "log_filter_prefs"
    private const val AGENT = "show_agent"
    private const val SAFETY = "show_dms"
    private const val MOB = "show_mob"

    private fun p(c: Context) =
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun save(c: Context, a: Boolean, d: Boolean, m: Boolean) {
        p(c).edit()
            .putBoolean(AGENT, a)
            .putBoolean(SAFETY, d)
            .putBoolean(MOB, m)
            .apply()
    }

    fun load(c: Context): Triple<Boolean, Boolean, Boolean> {
        val pref = p(c)
        return Triple(
            pref.getBoolean(AGENT, true),
            pref.getBoolean(SAFETY, true),
            pref.getBoolean(MOB, true)
        )
    }
}
