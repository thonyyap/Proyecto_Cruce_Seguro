package com.cruceseguro.controller

import android.app.Application
import android.location.Location
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cruceseguro.actuation.AlertService
import com.cruceseguro.capture.LocationService
import com.cruceseguro.capture.SensorService
import com.cruceseguro.decision.DecisionEngine
import com.cruceseguro.decision.DecisionInput
import com.cruceseguro.decision.ProximityLevel
import com.cruceseguro.decision.classifyProximity
import com.cruceseguro.model.AppMode
import com.cruceseguro.model.Availability
import com.cruceseguro.model.CrossingLocation
import com.cruceseguro.model.LocationSnapshot
import com.cruceseguro.model.SensorSnapshot
import com.cruceseguro.processing.ApproachDetector
import com.cruceseguro.processing.WalkingDetector
import com.cruceseguro.processing.WalkingSample
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val sensorService = SensorService(application)
    private val locationService = LocationService(application)
    private val alertService = AlertService(application)
    private val walkingDetector = WalkingDetector()
    private val approachDetector = ApproachDetector()
    private val decisionEngine = DecisionEngine()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var latestSensors = SensorSnapshot()
    private var latestLocation = LocationSnapshot()
    private var crossing: CrossingLocation? = null
    private var lastProcessedAccelerationMs: Long? = null
    private var simulationJob: Job? = null
    private var criticalJob: Job? = null

    init {
        sensorService.start()
        locationService.start()
        viewModelScope.launch { sensorService.snapshots.collect(::onSensorSnapshot) }
        viewModelScope.launch { locationService.snapshots.collect(::onLocationSnapshot) }
    }

    fun setMode(mode: AppMode) {
        if (_uiState.value.mode == mode) return
        stopSimulation()
        approachDetector.reset()
        decisionEngine.reset()
        walkingDetector.reset()
        _uiState.value = _uiState.value.copy(
            mode = mode,
            distanceMeters = if (mode == AppMode.SIMULATED) _uiState.value.simulatedDistanceMeters else realDistance(),
            approaching = false,
            nearCrossing = false,
            criticalAlertVisible = false,
        )
        processDecision()
    }

    fun setSimulatedDistance(distanceMeters: Double) {
        val clamped = distanceMeters.coerceIn(0.0, 50.0)
        val approaching = approachDetector.addDistance(clamped)
        _uiState.value = _uiState.value.copy(
            simulatedDistanceMeters = clamped,
            distanceMeters = if (_uiState.value.mode == AppMode.SIMULATED) clamped else _uiState.value.distanceMeters,
            approaching = if (_uiState.value.mode == AppMode.SIMULATED) approaching else _uiState.value.approaching,
        )
        if (_uiState.value.mode == AppMode.SIMULATED) processDecision()
    }

    fun startApproaching() = startSimulation(SimulationMotion.APPROACHING, -0.5)

    fun startMovingAway() = startSimulation(SimulationMotion.MOVING_AWAY, 0.5)

    fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
        _uiState.value = _uiState.value.copy(simulationMotion = SimulationMotion.STOPPED)
    }

    fun createCrossingHere(): Boolean {
        val latitude = latestLocation.latitude ?: return false
        val longitude = latestLocation.longitude ?: return false
        if (latestLocation.status != Availability.AVAILABLE) return false
        crossing = CrossingLocation(latitude, longitude)
        approachDetector.reset()
        decisionEngine.reset()
        _uiState.value = _uiState.value.copy(
            crossingCreated = true,
            distanceMeters = realDistance(),
            approaching = false,
        )
        processDecision()
        return true
    }

    fun refreshLocationPermission() = locationService.refreshPermissionState()

    private fun startSimulation(motion: SimulationMotion, delta: Double) {
        if (_uiState.value.mode != AppMode.SIMULATED) return
        simulationJob?.cancel()
        _uiState.value = _uiState.value.copy(simulationMotion = motion)
        simulationJob = viewModelScope.launch {
            while (true) {
                setSimulatedDistance(_uiState.value.simulatedDistanceMeters + delta)
                if (_uiState.value.simulatedDistanceMeters == 0.0 || _uiState.value.simulatedDistanceMeters == 50.0) {
                    stopSimulation()
                    break
                }
                delay(250L)
            }
        }
    }

    private fun onSensorSnapshot(snapshot: SensorSnapshot) {
        latestSensors = snapshot
        var walking = _uiState.value.walking
        var variation = _uiState.value.accelerationVariation
        if (snapshot.accelerometerTimestampMs != null && snapshot.accelerometerTimestampMs != lastProcessedAccelerationMs) {
            lastProcessedAccelerationMs = snapshot.accelerometerTimestampMs
            val state = walkingDetector.addSample(
                WalkingSample(snapshot.accelerationX, snapshot.accelerationY, snapshot.accelerationZ),
            )
            walking = state.isWalking
            variation = state.variation
        }
        _uiState.value = _uiState.value.copy(
            walking = walking,
            accelerationVariation = variation,
            accelerometerStatus = snapshot.accelerometerStatus,
            gyroscopeStatus = snapshot.gyroscopeStatus,
            gyroscopeX = snapshot.gyroscopeX,
            gyroscopeY = snapshot.gyroscopeY,
            gyroscopeZ = snapshot.gyroscopeZ,
        )
        processDecision()
    }

    private fun onLocationSnapshot(snapshot: LocationSnapshot) {
        latestLocation = snapshot
        val distance = if (_uiState.value.mode == AppMode.REAL) realDistance() else _uiState.value.distanceMeters
        val approaching = if (_uiState.value.mode == AppMode.REAL) approachDetector.addDistance(distance) else _uiState.value.approaching
        _uiState.value = _uiState.value.copy(
            gpsStatus = snapshot.status,
            gpsAccuracyMeters = snapshot.accuracyMeters,
            distanceMeters = distance,
            approaching = approaching,
        )
        processDecision()
    }

    private fun processDecision() {
        val now = SystemClock.elapsedRealtime()
        val state = _uiState.value
        val distance = state.distanceMeters
        val level = classifyProximity(distance)
        val sensorRecent = latestSensors.accelerometerTimestampMs?.let { now - it <= SENSOR_FRESHNESS_MS } == true &&
            latestSensors.gyroscopeTimestampMs?.let { now - it <= SENSOR_FRESHNESS_MS } == true
        val locationAccurate = state.mode == AppMode.SIMULATED ||
            (latestLocation.status == Availability.AVAILABLE && (latestLocation.accuracyMeters ?: Float.MAX_VALUE) <= MAX_GPS_ACCURACY_METERS)
        val decision = decisionEngine.evaluate(
            DecisionInput(
                walking = state.walking,
                sensorsRecent = sensorRecent,
                distanceMeters = distance,
                approaching = state.approaching,
                locationAccurate = locationAccurate,
                timestampMs = now,
            ),
        )
        _uiState.value = state.copy(
            proximityLevel = level,
            nearCrossing = level in ALERT_LEVELS,
        )
        if (decision.shouldAlert) {
            alertService.alert(decision.level)
            if (decision.level == ProximityLevel.CRITICAL) showCriticalAlert()
        }
    }

    private fun showCriticalAlert() {
        criticalJob?.cancel()
        _uiState.value = _uiState.value.copy(criticalAlertVisible = true)
        criticalJob = viewModelScope.launch {
            delay(5_000L)
            _uiState.value = _uiState.value.copy(criticalAlertVisible = false)
        }
    }

    private fun realDistance(): Double? {
        val target = crossing ?: return null
        val latitude = latestLocation.latitude ?: return null
        val longitude = latestLocation.longitude ?: return null
        val results = FloatArray(1)
        Location.distanceBetween(latitude, longitude, target.latitude, target.longitude, results)
        return results[0].toDouble()
    }

    override fun onCleared() {
        simulationJob?.cancel()
        criticalJob?.cancel()
        sensorService.stop()
        locationService.stop()
        alertService.close()
        super.onCleared()
    }

    private companion object {
        const val SENSOR_FRESHNESS_MS = 2_000L
        const val MAX_GPS_ACCURACY_METERS = 25f
        val ALERT_LEVELS = setOf(ProximityLevel.PREVENTIVE, ProximityLevel.NEAR, ProximityLevel.CRITICAL)
    }
}
