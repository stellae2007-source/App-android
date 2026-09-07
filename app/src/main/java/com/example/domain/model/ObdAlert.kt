package com.example.domain.model

data class ObdAlert(
    val id: String,
    val title: String,
    val message: String,
    val isCritical: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class AlertSettings(
    val coolantTempMaxAlertEnabled: Boolean = true,
    val coolantTempMaxLimit: Int = 105,

    val batteryVoltageLowAlertEnabled: Boolean = true,
    val batteryVoltageLowLimit: Float = 11.8f,

    val batteryVoltageHighAlertEnabled: Boolean = true,
    val batteryVoltageHighLimit: Float = 15.0f,

    val rpmHighAlertEnabled: Boolean = true,
    val rpmHighLimit: Int = 5500,

    val intakeTempHighAlertEnabled: Boolean = true,
    val intakeTempHighLimit: Int = 60,

    val engineLoadHighAlertEnabled: Boolean = true,
    val engineLoadHighLimit: Float = 90.0f
)
