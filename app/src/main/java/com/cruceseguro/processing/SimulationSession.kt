package com.cruceseguro.processing

class SimulationSession(
    private val approachValidityMs: Long = 1_000L,
    private val approachDetector: ApproachDetector = ApproachDetector(),
) {
    private var lastApproachTimestampMs: Long? = null

    fun updateDistance(
        distanceMeters: Double,
        timestampMs: Long,
    ): Boolean {
        val approaching = approachDetector.addDistance(distanceMeters)
        if (approaching) lastApproachTimestampMs = timestampMs
        return isApproaching(timestampMs)
    }

    fun isApproaching(timestampMs: Long): Boolean {
        val lastApproach = lastApproachTimestampMs ?: return false
        return timestampMs - lastApproach in 0..approachValidityMs
    }

    fun stop() = reset()

    fun reset() {
        approachDetector.reset()
        lastApproachTimestampMs = null
    }
}
