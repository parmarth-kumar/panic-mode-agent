package com.panicmode

import kotlin.random.Random

/**
 * Lightweight heuristic parser for converting free-text user prompts
 * into structured commands for cloud automation.
 * Intentionally rule-based (not NLP) to keep behavior predictable and debuggable.
 */
data class ParsedCommand(
    val contactName: String,
    val code: String,
    val intent: String,
    val enableDms: Boolean
)

object CommandParser {

    fun parse(input: String): ParsedCommand {
        val text = input.lowercase()

        val contact = extractContact(text)
        val intent = extractIntent(text)
        val hours = extractHours(text)
        val enableDms = detectDms(text)

        val code = generateCode(intent, text, hours)

        return ParsedCommand(contact, code, intent, enableDms)
    }

    // Heuristic extraction of contact name from simple natural language patterns
    private fun extractContact(text: String): String {
        val patterns = listOf(
            Regex("to\\s+([a-zA-Z']+)"),
            Regex("for\\s+([a-zA-Z']+)")
        )

        for (r in patterns) {
            val m = r.find(text)
            if (m != null) {
                return m.groupValues[1]
                    .replace("'s", "")
                    .replace("number", "")
                    .replace("my", "")
                    .trim()
            }
        }

        return "unknown"
    }

    // Coarse intent classification based on keyword presence (deterministic by design)
    private fun extractIntent(text: String): String {
        return when {
            listOf("travel", "hiking", "hike", "camp", "camping")
                .any { it in text } -> "TRAVELING"

            listOf("market", "office", "work", "school", "college", "crowd", "city")
                .any { it in text } -> "VISIBILITY"

            else -> "ADAPTIVE"
        }
    }

    // Optional duration hint used only for travel-related codes
    private fun extractHours(text: String): Int? {
        val regex = Regex("(\\d+)\\s*hour")
        val match = regex.find(text)
        return match?.groupValues?.get(1)?.toInt()
    }

    // Detects whether the user intends to enable periodic safety / DMS behavior
    private fun detectDms(text: String): Boolean {
        val keys = listOf(
            "dms",
            "deadman",
            "dead man",
            "turn on dms",
            "safety",
            "turn security",
            "safety check",
            "turn on safety",
            "security check",
            "periodic check",
            "complete protection",
            "personal security",
        )
        return keys.any { it in text }
    }

    // Generates a human-readable activation code aligned with the inferred intent
    private fun generateCode(intent: String, text: String, hours: Int?): String {

        if (intent == "TRAVELING") {
            val prefix = when {
                "hike" in text -> "HIKE"
                "camp" in text -> "CAMP"
                else -> "TRAVEL"
            }

            return if (hours != null) {
                "$prefix-${hours}H"
            } else {
                "$prefix-${Random.nextInt(10, 99)}"
            }
        }

        val double = listOf(11, 22, 33, 44, 55, 66, 77, 88, 99).random()

        return if (intent == "VISIBILITY") {
            "TRACK-$double"
        } else {
            "PANIC-$double"
        }
    }
}
