package com.panicmode

import android.content.Context

/**
 * Stores Mobilerun credentials and target device ID locally.
 * Kept intentionally minimal and user-controlled for demo
 * and hackathon environments (not long-term secret storage).
 */
object MobilerunPrefs {

    private const val PREF = "mobilerun_prefs"
    private const val KEY_API = "api_key"
    private const val KEY_DEVICE = "device_id"

    private fun p(c: Context) =
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun save(c: Context, api: String, device: String) {
        p(c).edit()
            .putString(KEY_API, api)
            .putString(KEY_DEVICE, device)
            .apply()
    }

    fun getApi(c: Context) =
        p(c).getString(KEY_API, "") ?: ""

    fun getDevice(c: Context) =
        p(c).getString(KEY_DEVICE, "") ?: ""

    fun isConfigured(c: Context) =
        getApi(c).isNotBlank() && getDevice(c).isNotBlank()

    // Clears stored credentials on explicit user action
    fun clear(c: Context) {
        p(c).edit().clear().apply()
    }
}
