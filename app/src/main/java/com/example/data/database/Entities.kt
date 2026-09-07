package com.example.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val vin: String? = null,
    val protocol: String? = null,
    val ecuName: String? = null,
    val lastConnected: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false
)

@Entity(tableName = "trip_sessions")
data class TripSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long? = null,
    val vehicleName: String = "Vehículo Principal",
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0,
    val distanceKm: Float = 0f,
    val maxSpeedKmh: Int = 0,
    val avgSpeedKmh: Float = 0f,
    val maxRpm: Int = 0,
    val minCoolantTemp: Int = 0,
    val maxCoolantTemp: Int = 0,
    val minBatteryVoltage: Float = 0f,
    val maxBatteryVoltage: Float = 0f,
    val estimatedFuelLiters: Float = 0f,
    val avgConsumptionL100km: Float = 0f,
    val totalPointsCount: Int = 0
)

@Entity(
    tableName = "trip_telemetry_points",
    foreignKeys = [
        ForeignKey(
            entity = TripSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class TripPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val speedKmh: Int? = null,
    val rpm: Int? = null,
    val coolantTemp: Int? = null,
    val engineLoad: Float? = null,
    val throttle: Float? = null,
    val maf: Float? = null,
    val mapKpa: Int? = null,
    val batteryVoltage: Float? = null,
    val fuelLevel: Float? = null,
    val gpsLat: Double? = null,
    val gpsLon: Double? = null,
    val gpsSpeed: Float? = null
)
