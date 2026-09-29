package com.cruceseguro.processing

import kotlin.math.abs
import kotlin.math.sqrt

data class WalkingSample(
    val x: Double,
    val y: Double,
    val z: Double,
)

data class WalkingState(
    val isWalking: Boolean,
    val variation: Double,
)

class WalkingDetector(
    private val windowSize: Int = 8,
    private val variationThreshold: Double = 0.65,
    private val activeSampleThreshold: Double = 0.75,
) {
    private val variations = ArrayDeque<Double>()

    fun addSample(sample: WalkingSample): WalkingState {
        val magnitude = sqrt(sample.x * sample.x + sample.y * sample.y + sample.z * sample.z)
        val variation = abs(magnitude - STANDARD_GRAVITY)
        variations.addLast(variation)
        while (variations.size > windowSize) variations.removeFirst()

        val averageVariation = variations.average()
        val requiredActiveSamples = windowSize / 2
        val isWalking =
            variations.size == windowSize &&
                averageVariation >= variationThreshold &&
                variations.count { it >= activeSampleThreshold } >= requiredActiveSamples

        return WalkingState(isWalking = isWalking, variation = averageVariation)
    }

    fun reset() = variations.clear()

    private companion object {
        const val STANDARD_GRAVITY = 9.81
    }
}
