package com.panicmode

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/*
 * Append-only structured logger for agent, safety, and cloud execution events.
 * Persists logs as NDJSON for durability/export while providing a formatted
 * reverse-chronological view for in-app debugging and demos.
 */
object AgentLog {

    // Explicit categories to separate agent reasoning, safety flow, and cloud execution
    enum class Type { AGENT, SAFETY, MOBILERUN }

    private const val FILE_NAME = "agent_log.ndjson"

    // Hard cap to prevent unbounded disk growth during long-running demos
    private const val MAX_BYTES = 64 * 1024

    // Single-process lock to keep writes atomic and ordering deterministic
    private val lock = Any()

    // Stable timestamp format for storage and later parsing/export
    private val isoTime =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    // Lightweight timestamp optimized for frequent UI rendering
    private val uiTime =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun log(context: Context, type: Type, message: String) {
        val file = File(context.filesDir, FILE_NAME)

        val entry = JSONObject().apply {
            put("ts", isoTime.format(Date()))
            put("type", type.name)
            put("msg", message)
        }

        synchronized(lock) {
            try {
                file.appendText(entry.toString() + "\n")

                // Trim older entries when size cap is exceeded, preserving recent context
                if (file.length() > MAX_BYTES) {
                    val lines = file.readLines()
                    file.writeText(
                        lines.takeLast(lines.size / 2).joinToString("\n") + "\n"
                    )
                }
            } catch (e: Exception) {
                // Logging must fail loudly; silent failure hides agent behavior during demos
                android.util.Log.e(
                    "AgentLog",
                    "LOG_WRITE_FAILED: ${e.message}"
                )
            }
        }
    }

    // Raw NDJSON output for export, debugging, or offline analysis
    fun getRawJsonLogs(context: Context): String {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return ""
        return file.readText()
    }

    // Formatted, reverse-chronological view intended for in-app activity display
    fun getLogs(context: Context): List<String> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()

        return file.readLines()
            .asReversed()
            .mapNotNull { line ->
                try {
                    val json = JSONObject(line)
                    val ts = isoTime.parse(json.getString("ts")) ?: Date()
                    "[${uiTime.format(ts)}] [${json.getString("type")}] ${json.getString("msg")}"
                } catch (_: Exception) {
                    // Defensive: skip malformed or partial log entries
                    null
                }
            }
    }

    // Clears persisted logs (used between demos or controlled test resets)
    fun clear(context: Context) {
        synchronized(lock) {
            File(context.filesDir, FILE_NAME).delete()
        }
    }
}
