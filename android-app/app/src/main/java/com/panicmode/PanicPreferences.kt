package com.panicmode

import android.content.Context

/**
 * Central persistence layer for the Panic Agent.
 *
 * Stores user configuration, agent lifecycle state (armed/suspended),
 * and real-time panic status used across services, workers, and UI.
 */
object PanicPreferences {

    private const val PREF_NAME = "panic_agent_memory"

    private const val KEY_CONTACT = "trusted_contact"
    private const val KEY_TRIGGER = "trigger_phrase"
    private const val KEY_BATTERY_CAPACITY = "battery_capacity_mah"
    private const val KEY_USER_INTENT = "user_intent"
    private const val KEY_SUSPENDED = "agent_suspended"
    private const val KEY_PANIC_ACTIVE = "panic_active"
    private const val KEY_AGENT_ARMED = "agent_armed"

    const val DEFAULT_TRIGGER = ""
    const val DEFAULT_CONTACT = ""
    const val DEFAULT_MAH = 5000
    private const val DEFAULT_INTENT = "NORMAL"

    private fun getPreferences(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ---------- USER CONFIGURATION ----------

    fun saveSettings(
        context: Context,
        contact: String,
        trigger: String,
        mah: Int,
        intent: String
    ) {
        getPreferences(context).edit().apply {
            putString(KEY_CONTACT, contact)
            putString(KEY_TRIGGER, trigger)
            putInt(KEY_BATTERY_CAPACITY, mah)
            putString(KEY_USER_INTENT, intent)
            apply()
        }
    }

    fun getContact(c: Context) =
        getPreferences(c).getString(KEY_CONTACT, DEFAULT_CONTACT) ?: DEFAULT_CONTACT

    fun getTrigger(c: Context) =
        getPreferences(c).getString(KEY_TRIGGER, DEFAULT_TRIGGER) ?: DEFAULT_TRIGGER

    fun getCapacity(c: Context) =
        getPreferences(c).getInt(KEY_BATTERY_CAPACITY, DEFAULT_MAH)

    fun getUserIntent(c: Context) =
        getPreferences(c).getString(KEY_USER_INTENT, DEFAULT_INTENT) ?: DEFAULT_INTENT

    // ---------- AGENT RUNTIME STATE ----------

    fun setSuspended(context: Context, suspended: Boolean) {
        getPreferences(context).edit()
            .putBoolean(KEY_SUSPENDED, suspended)
            .apply()
    }

    fun isSuspended(context: Context): Boolean =
        getPreferences(context).getBoolean(KEY_SUSPENDED, false)

    fun setPanicActive(context: Context, active: Boolean) {
        getPreferences(context).edit()
            .putBoolean(KEY_PANIC_ACTIVE, active)
            .apply()
    }

    fun isPanicActive(context: Context): Boolean =
        getPreferences(context).getBoolean(KEY_PANIC_ACTIVE, false)

    fun setAgentArmed(context: Context, armed: Boolean) {
        getPreferences(context).edit()
            .putBoolean(KEY_AGENT_ARMED, armed)
            .apply()
    }

    fun isAgentArmed(context: Context): Boolean =
        getPreferences(context).getBoolean(KEY_AGENT_ARMED, false)
}
