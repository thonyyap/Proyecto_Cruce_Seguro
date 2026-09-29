package com.cruceseguro.decision

import org.junit.Assert.assertEquals
import org.junit.Test

class ProximityClassifierTest {
    @Test
    fun `invalid distances are unavailable`() {
        assertEquals(ProximityLevel.UNAVAILABLE, classifyProximity(null))
        assertEquals(ProximityLevel.UNAVAILABLE, classifyProximity(-0.1))
        assertEquals(ProximityLevel.UNAVAILABLE, classifyProximity(Double.NaN))
        assertEquals(ProximityLevel.UNAVAILABLE, classifyProximity(Double.POSITIVE_INFINITY))
    }

    @Test
    fun `distances outside the crossing zone are clear`() {
        assertEquals(ProximityLevel.CLEAR, classifyProximity(30.01))
        assertEquals(ProximityLevel.CLEAR, classifyProximity(50.0))
    }

    @Test
    fun `boundary distances select the three warning levels`() {
        assertEquals(ProximityLevel.PREVENTIVE, classifyProximity(30.0))
        assertEquals(ProximityLevel.PREVENTIVE, classifyProximity(15.01))
        assertEquals(ProximityLevel.NEAR, classifyProximity(15.0))
        assertEquals(ProximityLevel.NEAR, classifyProximity(8.01))
        assertEquals(ProximityLevel.CRITICAL, classifyProximity(8.0))
        assertEquals(ProximityLevel.CRITICAL, classifyProximity(0.0))
    }
}
