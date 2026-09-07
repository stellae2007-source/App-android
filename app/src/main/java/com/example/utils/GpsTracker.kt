package com.example.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class GpsTracker(
    private val context: Context,
    private val onLocationUpdate: (speedKmh: Float?, lat: Double?, lon: Double?, alt: Double?, distKm: Float?) -> Unit
) {
    private val fusedClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var isTracking = false
    private var lastLocation: Location? = null
    private var totalDistanceMeters = 0f

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return

            lastLocation?.let { prev ->
                val dist = prev.distanceTo(location)
                if (dist > 1.0f) { // filter micro GPS jitter
                    totalDistanceMeters += dist
                }
            }
            lastLocation = location

            // Speed in m/s converted to km/h
            val speedKmh = if (location.hasSpeed()) location.speed * 3.6f else null
            val distKm = totalDistanceMeters / 1000f

            onLocationUpdate(
                speedKmh,
                location.latitude,
                location.longitude,
                if (location.hasAltitude()) location.altitude else null,
                distKm
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (isTracking) return
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .setMinUpdateDistanceMeters(1f)
            .build()

        try {
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            isTracking = true
        } catch (e: Exception) {
            // Location permission not granted
        }
    }

    fun stopTracking() {
        if (!isTracking) return
        try {
            fusedClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {}
        isTracking = false
    }

    fun resetDistance() {
        totalDistanceMeters = 0f
        lastLocation = null
    }
}
