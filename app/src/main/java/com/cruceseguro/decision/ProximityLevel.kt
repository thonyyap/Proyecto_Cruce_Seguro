package com.cruceseguro.decision

enum class ProximityLevel {
    UNAVAILABLE,
    CLEAR,
    PREVENTIVE,
    NEAR,
    CRITICAL,
}

fun classifyProximity(distanceMeters: Double?): ProximityLevel {
    if (distanceMeters == null || !distanceMeters.isFinite() || distanceMeters < 0.0) {
        return ProximityLevel.UNAVAILABLE
    }

    return when {
        distanceMeters <= 8.0 -> ProximityLevel.CRITICAL
        distanceMeters <= 15.0 -> ProximityLevel.NEAR
        distanceMeters <= 30.0 -> ProximityLevel.PREVENTIVE
        else -> ProximityLevel.CLEAR
    }
}
