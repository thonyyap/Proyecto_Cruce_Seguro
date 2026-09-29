package com.cruceseguro.processing

import com.cruceseguro.model.AppMode
import com.cruceseguro.model.Availability

fun isLocationUsable(
    mode: AppMode,
    status: Availability,
    accuracyMeters: Float?,
    fixTimestampMs: Long?,
    nowMs: Long,
    maxAccuracyMeters: Float = 25f,
    maxAgeMs: Long = 4_000L,
): Boolean {
    if (mode == AppMode.SIMULATED) return true
    val timestamp = fixTimestampMs ?: return false
    val accuracy = accuracyMeters ?: return false
    val age = nowMs - timestamp
    return status == Availability.AVAILABLE &&
        accuracy <= maxAccuracyMeters &&
        age in 0..maxAgeMs
}
