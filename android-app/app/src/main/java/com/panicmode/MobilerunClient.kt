package com.panicmode

import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Thin client for Mobilerun task execution.
 * Acts as the bridge between parsed agent intent and
 * remote Android automation running on the user's device.
 */
object MobilerunClient {

    private val client = OkHttpClient()

    fun createTask(
        apiKey: String,
        deviceId: String,
        prompt: String,
        callback: (String) -> Unit
    ) {
        // Construct Mobilerun task payload (LLM-driven automation)
        val json = JSONObject().apply {
            put("task", prompt)
            put("llmModel", "google/gemini-3-flash")
            put("maxSteps", 1000)
            put("executionTimeout", 1000)
            put("temperature", 0.5)
            put("reasoning", false)
            put("vision", false)
            put("deviceId", deviceId)
        }

        val body = json.toString()
            .toRequestBody("application/json".toMediaType())

        val req = Request.Builder()
            .url("https://api.mobilerun.ai/v1/tasks/")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        // Async execution to avoid blocking UI / agent threads
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("ERROR: ${e.message}")
            }

            override fun onResponse(call: Call, res: Response) {
                callback(res.body?.string() ?: "No response")
            }
        })
    }
}
