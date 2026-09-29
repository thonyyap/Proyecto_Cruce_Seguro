package com.cruceseguro.decision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DecisionEngineTest {
    private fun validInput(
        timeMs: Long,
        walking: Boolean = true,
        sensorsRecent: Boolean = true,
        distanceMeters: Double? = 12.0,
        approaching: Boolean = true,
        locationAccurate: Boolean = true,
    ) = DecisionInput(
        walking = walking,
        sensorsRecent = sensorsRecent,
        distanceMeters = distanceMeters,
        approaching = approaching,
        locationAccurate = locationAccurate,
        timestampMs = timeMs,
    )

    @Test
    fun `all input conditions are required`() {
        val invalidInputs = listOf(
            validInput(0, walking = false),
            validInput(0, sensorsRecent = false),
            validInput(0, distanceMeters = 31.0),
            validInput(0, distanceMeters = null),
            validInput(0, approaching = false),
            validInput(0, locationAccurate = false),
        )

        invalidInputs.forEach { input ->
            assertFalse(DecisionEngine().evaluate(input).shouldAlert)
        }
    }

    @Test
    fun `conditions must persist for exactly 1500 milliseconds`() {
        val engine = DecisionEngine()

        assertFalse(engine.evaluate(validInput(100)).shouldAlert)
        assertFalse(engine.evaluate(validInput(1599)).shouldAlert)
        val decision = engine.evaluate(validInput(1600))

        assertTrue(decision.shouldAlert)
        assertEquals(ProximityLevel.NEAR, decision.level)
    }

    @Test
    fun `breaking one condition restarts persistence`() {
        val engine = DecisionEngine()

        engine.evaluate(validInput(0))
        engine.evaluate(validInput(1000, approaching = false))
        assertFalse(engine.evaluate(validInput(1500)).shouldAlert)
        assertTrue(engine.evaluate(validInput(3000)).shouldAlert)
    }

    @Test
    fun `cooldown blocks alerts and requires new persistence afterward`() {
        val engine = DecisionEngine()
        engine.evaluate(validInput(0))
        assertTrue(engine.evaluate(validInput(1500)).shouldAlert)

        assertFalse(engine.evaluate(validInput(21499)).shouldAlert)
        assertFalse(engine.evaluate(validInput(21500)).shouldAlert)
        assertFalse(engine.evaluate(validInput(22999)).shouldAlert)
        assertTrue(engine.evaluate(validInput(23000)).shouldAlert)
    }

    @Test
    fun `reset clears persistence and cooldown`() {
        val engine = DecisionEngine()
        engine.evaluate(validInput(0))
        assertTrue(engine.evaluate(validInput(1500)).shouldAlert)

        engine.reset()

        assertFalse(engine.evaluate(validInput(1600)).shouldAlert)
        assertTrue(engine.evaluate(validInput(3100)).shouldAlert)
    }

    @Test
    fun `time moving backward safely restarts persistence`() {
        val engine = DecisionEngine()
        engine.evaluate(validInput(1000))

        assertFalse(engine.evaluate(validInput(900)).shouldAlert)
        assertFalse(engine.evaluate(validInput(2399)).shouldAlert)
        assertTrue(engine.evaluate(validInput(2400)).shouldAlert)
    }
}
