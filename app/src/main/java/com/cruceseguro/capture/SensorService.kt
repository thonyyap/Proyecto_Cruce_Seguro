package com.cruceseguro.capture

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import com.cruceseguro.model.Availability
import com.cruceseguro.model.SensorSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SensorService(
    context: Context,
) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val _snapshots =
        MutableStateFlow(
            SensorSnapshot(
                accelerometerStatus = if (accelerometer == null) Availability.UNAVAILABLE else Availability.AVAILABLE,
                gyroscopeStatus = if (gyroscope == null) Availability.UNAVAILABLE else Availability.AVAILABLE,
            ),
        )

    val snapshots: StateFlow<SensorSnapshot> = _snapshots.asStateFlow()

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() = sensorManager.unregisterListener(this)

    override fun onSensorChanged(event: SensorEvent) {
        val now = SystemClock.elapsedRealtime()
        _snapshots.value =
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER ->
                    _snapshots.value.copy(
                        accelerationX = event.values[0].toDouble(),
                        accelerationY = event.values[1].toDouble(),
                        accelerationZ = event.values[2].toDouble(),
                        accelerometerTimestampMs = now,
                    )
                Sensor.TYPE_GYROSCOPE ->
                    _snapshots.value.copy(
                        gyroscopeX = event.values[0].toDouble(),
                        gyroscopeY = event.values[1].toDouble(),
                        gyroscopeZ = event.values[2].toDouble(),
                        gyroscopeTimestampMs = now,
                    )
                else -> _snapshots.value
            }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int,
    ) = Unit
}
