package com.cruceseguro.processing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalkingDetectorTest {
    @Test
    fun `steady gravity is not walking`() {
        val detector = WalkingDetector()

        repeat(10) {
            assertFalse(detector.addSample(WalkingSample(0.0, 0.0, 9.81)).isWalking)
        }
    }

    @Test
    fun `repeated step-like acceleration becomes walking`() {
        val detector = WalkingDetector()
        val zValues = listOf(9.81, 11.2, 8.5, 11.0, 8.7, 11.3, 8.4, 10.9)

        val state = zValues.map { detector.addSample(WalkingSample(0.0, 0.0, it)) }.last()

        assertTrue(state.isWalking)
        assertTrue(state.variation > 0.65)
    }

    @Test
    fun `one isolated movement sample does not become walking`() {
        val detector = WalkingDetector()
        repeat(7) { detector.addSample(WalkingSample(0.0, 0.0, 9.81)) }

        assertFalse(detector.addSample(WalkingSample(0.0, 0.0, 13.0)).isWalking)
    }
}
