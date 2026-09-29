package com.cruceseguro.processing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulationSessionTest {
    @Test
    fun `stopping clears an established approach`() {
        val session = SimulationSession()
        session.updateDistance(30.0, 0)
        session.updateDistance(29.0, 250)
        session.updateDistance(28.0, 500)
        session.updateDistance(27.0, 750)
        assertTrue(session.isApproaching(750))

        session.stop()

        assertFalse(session.isApproaching(751))
    }

    @Test
    fun `approach expires when distance stops changing`() {
        val session = SimulationSession(approachValidityMs = 1_000)
        session.updateDistance(30.0, 0)
        session.updateDistance(29.0, 250)
        session.updateDistance(28.0, 500)
        session.updateDistance(27.0, 750)

        assertTrue(session.isApproaching(1_750))
        assertFalse(session.isApproaching(1_751))
    }

    @Test
    fun `reset prevents distance history crossing mode boundaries`() {
        val session = SimulationSession()
        listOf(30.0, 29.0, 28.0, 27.0).forEachIndexed { index, distance ->
            session.updateDistance(distance, index * 250L)
        }

        session.reset()

        assertFalse(session.updateDistance(20.0, 2_000))
    }
}
