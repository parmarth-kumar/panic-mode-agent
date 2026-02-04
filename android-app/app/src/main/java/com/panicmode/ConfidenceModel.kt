package com.panicmode

/**
 * Internal heuristic model that estimates confidence in the agent’s current state
 * based on signal quality, battery health, and startup context.
 * Used for diagnostics and logging only, not user-facing decisions.
 */
object ConfidenceModel {

    fun calculate(
        hasLocation: Boolean,
        isLive: Boolean,
        batteryPct: Int,
        isColdStart: Boolean
    ): Confidence {

        var score = 100

        // Penalize missing or degraded signals rather than hard-failing
        if (!hasLocation) score -= 30
        if (hasLocation && !isLive) score -= 15
        if (batteryPct < 15) score -= 20
        if (isColdStart) score -= 10

        // Clamp to avoid misleading extremes in logs and demos
        score = score.coerceIn(40, 100)

        val label = when {
            score >= 85 -> "HIGH"
            score >= 65 -> "MEDIUM"
            else -> "LOW"
        }

        return Confidence(score, label)
    }

    data class Confidence(
        val score: Int,
        val label: String
    )
}
