package com.cruceseguro.controller

import com.cruceseguro.decision.ProximityLevel
import com.cruceseguro.model.AppMode
import com.cruceseguro.model.Availability

data class MainUiState(
    val mode: AppMode = AppMode.SIMULATED,
    val distanceMeters: Double? = 50.0,
    val simulatedDistanceMeters: Double = 50.0,
    val proximityLevel: ProximityLevel = ProximityLevel.CLEAR,
    val walking: Boolean = false,
    val nearCrossing: Boolean = false,
    val approaching: Boolean = false,
    val accelerometerStatus: Availability = Availability.UNAVAILABLE,
    val gyroscopeStatus: Availability = Availability.UNAVAILABLE,
    val gpsStatus: Availability = Availability.PERMISSION_REQUIRED,
    val gpsAccuracyMeters: Float? = null,
    val accelerationVariation: Double = 0.0,
    val gyroscopeX: Double = 0.0,
    val gyroscopeY: Double = 0.0,
    val gyroscopeZ: Double = 0.0,
    val crossingCreated: Boolean = false,
    val simulationMotion: SimulationMotion = SimulationMotion.STOPPED,
    val criticalAlertVisible: Boolean = false,
)

enum class SimulationMotion { APPROACHING, MOVING_AWAY, STOPPED }
