package com.panicmode

import android.content.Context

object PanicPreferences {
    private const val PREF_NAME = "panic_agent_memory"

    private const val KEY_CONTACT = "trusted_contact"
    private const val KEY_TRIGGER = "trigger_phrase"
    private const val KEY_BATTERY_CAPACITY = "battery_capacity_mah"
    private const val KEY_USER_INTENT = "user_intent"

    const val DEFAULT_TRIGGER = ""
    const val DEFAULT_CONTACT = ""
    const val DEFAULT_MAH = 5000

    fun saveSettings(context: Context, contact: String, trigger: String, mah: Int, intent: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().apply {
            putString(KEY_CONTACT, contact)
            putString(KEY_TRIGGER, trigger)
            putInt(KEY_BATTERY_CAPACITY, mah)
            putString(KEY_USER_INTENT, intent)
            apply()
        }
    }

    fun getContact(c: Context) = c.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_CONTACT, DEFAULT_CONTACT) ?: DEFAULT_CONTACT
    fun getTrigger(c: Context) = c.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_TRIGGER, DEFAULT_TRIGGER) ?: DEFAULT_TRIGGER
    fun getCapacity(c: Context) = c.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getInt(KEY_BATTERY_CAPACITY, DEFAULT_MAH)
    fun getUserIntent(c: Context) = c.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_USER_INTENT, "NORMAL") ?: "NORMAL"
}