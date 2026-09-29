package com.cruceseguro.decision

data class DecisionInput(
    val walking: Boolean,
    val sensorsRecent: Boolean,
    val distanceMeters: Double?,
    val approaching: Boolean,
    val locationAccurate: Boolean,
    val timestampMs: Long,
)

data class AlertDecision(
    val shouldAlert: Boolean,
    val level: ProximityLevel = ProximityLevel.UNAVAILABLE,
)

class DecisionEngine(
    private val persistenceMs: Long = 1_500L,
    private val cooldownMs: Long = 20_000L,
) {
    private var candidateSinceMs: Long? = null
    private var lastAlertMs: Long? = null
    private var lastTimestampMs: Long? = null

    fun evaluate(input: DecisionInput): AlertDecision {
        if (lastTimestampMs?.let { input.timestampMs < it } == true) {
            candidateSinceMs = null
            lastAlertMs = null
        }
        lastTimestampMs = input.timestampMs

        val level = classifyProximity(input.distanceMeters)
        val allConditionsMet =
            input.walking &&
                input.sensorsRecent &&
                input.approaching &&
                input.locationAccurate &&
                level in ALERT_LEVELS

        if (!allConditionsMet) {
            candidateSinceMs = null
            return AlertDecision(shouldAlert = false, level = level)
        }

        val previousAlert = lastAlertMs
        if (previousAlert != null && input.timestampMs - previousAlert < cooldownMs) {
            candidateSinceMs = null
            return AlertDecision(shouldAlert = false, level = level)
        }

        val startedAt = candidateSinceMs
        if (startedAt == null) {
            candidateSinceMs = input.timestampMs
            return AlertDecision(shouldAlert = false, level = level)
        }

        if (input.timestampMs - startedAt < persistenceMs) {
            return AlertDecision(shouldAlert = false, level = level)
        }

        lastAlertMs = input.timestampMs
        candidateSinceMs = null
        return AlertDecision(shouldAlert = true, level = level)
    }

    fun reset() {
        candidateSinceMs = null
        lastAlertMs = null
        lastTimestampMs = null
    }

    private companion object {
        val ALERT_LEVELS =
            setOf(
                ProximityLevel.PREVENTIVE,
                ProximityLevel.NEAR,
                ProximityLevel.CRITICAL,
            )
    }
}
