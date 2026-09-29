package com.cruceseguro.processing

class ApproachDetector(
    private val windowSize: Int = 4,
    private val minimumNetDecreaseMeters: Double = 1.5,
) {
    private val distances = ArrayDeque<Double>()

    fun addDistance(distanceMeters: Double?): Boolean {
        if (distanceMeters == null || !distanceMeters.isFinite() || distanceMeters < 0.0) {
            return false
        }

        distances.addLast(distanceMeters)
        while (distances.size > windowSize) distances.removeFirst()
        if (distances.size < windowSize) return false

        val values = distances.toList()
        val netDecrease = values.first() - values.last()
        val decreasingSteps = values.zipWithNext().count { (before, after) -> after < before }
        return netDecrease >= minimumNetDecreaseMeters && decreasingSteps >= 2
    }

    fun reset() = distances.clear()
}
