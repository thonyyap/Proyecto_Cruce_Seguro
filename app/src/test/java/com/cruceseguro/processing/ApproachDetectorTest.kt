package com.cruceseguro.processing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApproachDetectorTest {
    @Test
    fun `clear decreasing trend is approaching`() {
        val detector = ApproachDetector()

        listOf(28.0, 27.3, 26.5).forEach { detector.addDistance(it) }

        assertTrue(detector.addDistance(25.8))
    }

    @Test
    fun `small gps noise is not approaching`() {
        val detector = ApproachDetector()

        listOf(20.0, 20.3, 19.8).forEach { detector.addDistance(it) }

        assertFalse(detector.addDistance(20.1))
    }

    @Test
    fun `increasing trend is moving away`() {
        val detector = ApproachDetector()

        listOf(10.0, 10.8, 11.7).forEach { detector.addDistance(it) }

        assertFalse(detector.addDistance(12.5))
    }

    @Test
    fun `invalid values cannot create a trend`() {
        val detector = ApproachDetector()

        assertFalse(detector.addDistance(null))
        assertFalse(detector.addDistance(-1.0))
        assertFalse(detector.addDistance(Double.NaN))
        assertFalse(detector.addDistance(Double.POSITIVE_INFINITY))
    }

    @Test
    fun `reset removes previous distance history`() {
        val detector = ApproachDetector()
        listOf(30.0, 28.0, 26.0, 24.0).forEach { detector.addDistance(it) }

        detector.reset()

        assertFalse(detector.addDistance(20.0))
    }
}
