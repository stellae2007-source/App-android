package com.example.data.repository

import com.example.data.database.TripDao
import com.example.data.database.TripPointEntity
import com.example.data.database.TripSessionEntity
import com.example.domain.model.VehicleLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TripRecorder(
    private val tripDao: TripDao,
    private val coroutineScope: CoroutineScope
) {
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _currentSessionStartTime = MutableStateFlow<Long?>(null)
    val currentSessionStartTime: StateFlow<Long?> = _currentSessionStartTime.asStateFlow()

    private val _currentSessionDurationSec = MutableStateFlow(0L)
    val currentSessionDurationSec: StateFlow<Long> = _currentSessionDurationSec.asStateFlow()

    private val _currentDistanceKm = MutableStateFlow(0f)
    val currentDistanceKm: StateFlow<Float> = _currentDistanceKm.asStateFlow()

    private val recordedPoints = mutableListOf<TripPointEntity>()
    private var sessionVehicleId: Long? = null
    private var sessionVehicleName: String = "Vehículo Principal"

    private var maxSpeed = 0
    private var totalSpeedSum = 0L
    private var speedCount = 0
    private var maxRpm = 0
    private var minCoolant: Int? = null
    private var maxCoolant: Int? = null
    private var minVoltage: Float? = null
    private var maxVoltage: Float? = null
    private var lastRecordedTimestamp = 0L
    private var totalEstimatedFuelLiters = 0f

    fun startTrip(vehicleId: Long?, vehicleName: String) {
        if (_isRecording.value) return
        recordedPoints.clear()
        sessionVehicleId = vehicleId
        sessionVehicleName = vehicleName
        maxSpeed = 0
        totalSpeedSum = 0L
        speedCount = 0
        maxRpm = 0
        minCoolant = null
        maxCoolant = null
        minVoltage = null
        maxVoltage = null
        _currentDistanceKm.value = 0f
        _currentSessionDurationSec.value = 0L
        totalEstimatedFuelLiters = 0f
        lastRecordedTimestamp = System.currentTimeMillis()

        _currentSessionStartTime.value = lastRecordedTimestamp
        _isRecording.value = true
    }

    fun recordPoint(data: VehicleLiveData) {
        if (!_isRecording.value) return
        val now = System.currentTimeMillis()
        val start = _currentSessionStartTime.value ?: now
        _currentSessionDurationSec.value = (now - start) / 1000

        // Distance approximation from speed over delta time
        if (lastRecordedTimestamp > 0 && data.speed != null && data.speed > 0) {
            val deltaHours = (now - lastRecordedTimestamp) / (1000f * 3600f)
            val addedKm = data.speed * deltaHours
            _currentDistanceKm.value += addedKm

            if (data.instantLitersPerHour != null && data.instantLitersPerHour > 0f) {
                totalEstimatedFuelLiters += (data.instantLitersPerHour * deltaHours)
            }
        }
        lastRecordedTimestamp = now

        // Update extremes
        data.speed?.let {
            if (it > maxSpeed) maxSpeed = it
            totalSpeedSum += it
            speedCount++
        }
        data.rpm?.let { if (it > maxRpm) maxRpm = it }
        data.coolantTemperature?.let {
            minCoolant = minOf(minCoolant ?: it, it)
            maxCoolant = maxOf(maxCoolant ?: it, it)
        }
        data.batteryVoltage?.let {
            minVoltage = minOf(minVoltage ?: it, it)
            maxVoltage = maxOf(maxVoltage ?: it, it)
        }

        // Downsample points to ~1 per second to prevent overloading Room
        val shouldSavePoint = recordedPoints.isEmpty() || (now - (recordedPoints.lastOrNull()?.timestamp ?: 0)) >= 900
        if (shouldSavePoint) {
            val point = TripPointEntity(
                sessionId = 0L, // Will be set after session insert
                timestamp = now,
                speedKmh = data.speed,
                rpm = data.rpm,
                coolantTemp = data.coolantTemperature,
                engineLoad = data.engineLoad,
                throttle = data.throttlePosition,
                maf = data.maf,
                mapKpa = data.map,
                batteryVoltage = data.batteryVoltage,
                fuelLevel = data.fuelLevel,
                gpsLat = data.gpsLatitude,
                gpsLon = data.gpsLongitude,
                gpsSpeed = data.gpsSpeed
            )
            recordedPoints.add(point)
        }
    }

    suspend fun stopTrip(): Long? = withContext(Dispatchers.IO) {
        if (!_isRecording.value) return@withContext null
        _isRecording.value = false

        val startTime = _currentSessionStartTime.value ?: System.currentTimeMillis()
        val endTime = System.currentTimeMillis()
        val duration = (endTime - startTime) / 1000
        val avgSpeed = if (speedCount > 0) totalSpeedSum.toFloat() / speedCount else 0f
        val distance = _currentDistanceKm.value
        val avgConsumption = if (distance > 0.5f && totalEstimatedFuelLiters > 0f) {
            (totalEstimatedFuelLiters / distance) * 100f
        } else 0f

        val session = TripSessionEntity(
            vehicleId = sessionVehicleId,
            vehicleName = sessionVehicleName,
            startTime = startTime,
            endTime = endTime,
            durationSeconds = duration,
            distanceKm = distance,
            maxSpeedKmh = maxSpeed,
            avgSpeedKmh = avgSpeed,
            maxRpm = maxRpm,
            minCoolantTemp = minCoolant ?: 0,
            maxCoolantTemp = maxCoolant ?: 0,
            minBatteryVoltage = minVoltage ?: 0f,
            maxBatteryVoltage = maxVoltage ?: 0f,
            estimatedFuelLiters = totalEstimatedFuelLiters,
            avgConsumptionL100km = avgConsumption,
            totalPointsCount = recordedPoints.size
        )

        val sessionId = tripDao.insertSession(session)
        if (recordedPoints.isNotEmpty()) {
            val linkedPoints = recordedPoints.map { it.copy(sessionId = sessionId) }
            tripDao.insertPoints(linkedPoints)
        }

        recordedPoints.clear()
        _currentSessionStartTime.value = null
        return@withContext sessionId
    }
}
