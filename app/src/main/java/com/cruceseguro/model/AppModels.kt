package com.cruceseguro.model

enum class AppMode { SIMULATED, REAL }

enum class Availability { AVAILABLE, UNAVAILABLE, PERMISSION_REQUIRED, DISABLED }

data class SensorSnapshot(
    val accelerometerStatus: Availability = Availability.UNAVAILABLE,
    val gyroscopeStatus: Availability = Availability.UNAVAILABLE,
    val accelerationX: Double = 0.0,
    val accelerationY: Double = 0.0,
    val accelerationZ: Double = 0.0,
    val gyroscopeX: Double = 0.0,
    val gyroscopeY: Double = 0.0,
    val gyroscopeZ: Double = 0.0,
    val accelerometerTimestampMs: Long? = null,
    val gyroscopeTimestampMs: Long? = null,
)

data class LocationSnapshot(
    val status: Availability = Availability.PERMISSION_REQUIRED,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val timestampMs: Long? = null,
)

data class CrossingLocation(
    val latitude: Double,
    val longitude: Double,
)
