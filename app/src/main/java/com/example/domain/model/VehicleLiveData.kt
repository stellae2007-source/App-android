package com.example.domain.model

data class VehicleLiveData(
    val rpm: Int? = null,
    val speed: Int? = null,
    val coolantTemperature: Int? = null,
    val engineLoad: Float? = null,
    val throttlePosition: Float? = null,
    val maf: Float? = null,
    val map: Int? = null,
    val intakeTemperature: Int? = null,
    val fuelLevel: Float? = null,
    val batteryVoltage: Float? = null,
    val fuelTrimShort: Float? = null,
    val fuelTrimLong: Float? = null,
    val fuelTrimShortB2: Float? = null,
    val fuelTrimLongB2: Float? = null,
    val fuelRailPressure: Float? = null,
    val timingAdvance: Float? = null,
    val ambientTemperature: Int? = null,
    val barometricPressure: Int? = null,
    val runTimeSeconds: Int? = null,
    val distanceMilOn: Int? = null,
    val distanceDtcCleared: Int? = null,
    val fuelRate: Float? = null,
    val oxygenSensorVoltage: Float? = null,
    val fuelSystemStatus: String? = null,
    val obdStandard: String? = null,

    // Calculated fields
    val instantConsumptionL100km: Float? = null,
    val instantConsumptionKmPerL: Float? = null,
    val instantLitersPerHour: Float? = null,
    val consumptionUnavailableReason: String? = null,

    // GPS telemetry (optional)
    val gpsSpeed: Float? = null,
    val gpsLatitude: Double? = null,
    val gpsLongitude: Double? = null,
    val gpsAltitude: Double? = null,
    val gpsDistanceKm: Float? = null,

    // Telemetry metadata
    val timestamp: Long = System.currentTimeMillis(),
    val updateRateHz: Float = 0f,
    val latencyMs: Long = 0L
)
