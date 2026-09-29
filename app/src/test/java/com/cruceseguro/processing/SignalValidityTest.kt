package com.cruceseguro.processing

import com.cruceseguro.model.AppMode
import com.cruceseguro.model.Availability
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalValidityTest {
    @Test
    fun `simulated mode does not require a location fix`() {
        assertTrue(
            isLocationUsable(
                mode = AppMode.SIMULATED,
                status = Availability.UNAVAILABLE,
                accuracyMeters = null,
                fixTimestampMs = null,
                nowMs = 10_000,
            ),
        )
    }

    @Test
    fun `real mode rejects stale or inaccurate location fixes`() {
        assertFalse(isLocationUsable(AppMode.REAL, Availability.AVAILABLE, 5f, 1_000, 5_001))
        assertFalse(isLocationUsable(AppMode.REAL, Availability.AVAILABLE, 25.1f, 4_500, 5_000))
        assertFalse(isLocationUsable(AppMode.REAL, Availability.DISABLED, 5f, 4_500, 5_000))
    }

    @Test
    fun `real mode accepts a fresh accurate fix`() {
        assertTrue(isLocationUsable(AppMode.REAL, Availability.AVAILABLE, 25f, 1_000, 5_000))
    }
}
