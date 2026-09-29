package com.cruceseguro.capture

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.cruceseguro.model.Availability
import com.cruceseguro.model.LocationSnapshot
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationService(
    private val context: Context,
) {
    private val client: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val _snapshots = MutableStateFlow(LocationSnapshot())
    val snapshots: StateFlow<LocationSnapshot> = _snapshots.asStateFlow()

    private val callback =
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                _snapshots.value =
                    LocationSnapshot(
                        status = Availability.AVAILABLE,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracyMeters = location.accuracy,
                        timestampMs = SystemClock.elapsedRealtime(),
                    )
            }
        }

    @SuppressLint("MissingPermission")
    fun start() {
        if (!hasLocationPermission()) {
            _snapshots.value = LocationSnapshot(status = Availability.PERMISSION_REQUIRED)
            return
        }
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            _snapshots.value = LocationSnapshot(status = Availability.DISABLED)
            return
        }
        val request =
            LocationRequest
                .Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
                .setMinUpdateIntervalMillis(500L)
                .build()
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    fun refreshPermissionState() = start()

    fun stop() = client.removeLocationUpdates(callback)

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
}
