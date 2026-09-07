package com.example.data.repository

import com.example.data.bluetooth.BluetoothDeviceModel
import com.example.data.bluetooth.BluetoothManager
import com.example.data.database.VehicleDao
import com.example.data.database.VehicleEntity
import com.example.data.obd.Elm327Manager
import com.example.data.obd.ElmResponse
import com.example.data.obd.Obd2Manager
import com.example.domain.model.AlertSettings
import com.example.domain.model.ConnectionState
import com.example.domain.model.DtcCode
import com.example.domain.model.ObdAlert
import com.example.domain.model.ObdDebugMessage
import com.example.domain.model.ObdSettings
import com.example.domain.model.SupportedPids
import com.example.domain.model.VehicleLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedDeque

class ObdRepository(
    val bluetoothManager: BluetoothManager,
    private val elm327Manager: Elm327Manager,
    private val obd2Manager: Obd2Manager,
    val tripRecorder: TripRecorder,
    private val vehicleDao: VehicleDao,
    private val coroutineScope: CoroutineScope
) {
    val connectionState: StateFlow<ConnectionState> = bluetoothManager.connectionState
    val liveData: StateFlow<VehicleLiveData> = obd2Manager.liveData
    val supportedPids: StateFlow<SupportedPids> = obd2Manager.supportedPids
    val storedDtcs: StateFlow<List<DtcCode>> = obd2Manager.storedDtcs
    val activeProtocol: StateFlow<String> = obd2Manager.activeProtocol
    val vinNumber: StateFlow<String?> = obd2Manager.vinNumber

    private val _activeVehicle = MutableStateFlow<VehicleEntity?>(null)
    val activeVehicle: StateFlow<VehicleEntity?> = _activeVehicle.asStateFlow()

    private val _obdSettings = MutableStateFlow(ObdSettings())
    val obdSettings: StateFlow<ObdSettings> = _obdSettings.asStateFlow()

    private val _alertSettings = MutableStateFlow(AlertSettings())
    val alertSettings: StateFlow<AlertSettings> = _alertSettings.asStateFlow()

    private val _activeAlerts = MutableStateFlow<List<ObdAlert>>(emptyList())
    val activeAlerts: StateFlow<List<ObdAlert>> = _activeAlerts.asStateFlow()

    private val logDeque = ConcurrentLinkedDeque<ObdDebugMessage>()
    private val _debugLogs = MutableStateFlow<List<ObdDebugMessage>>(emptyList())
    val debugLogs: StateFlow<List<ObdDebugMessage>> = _debugLogs.asStateFlow()

    init {
        // Collect live data for trip recording and alert evaluation
        coroutineScope.launch {
            liveData.collect { data ->
                tripRecorder.recordPoint(data)
                evaluateAlerts(data)
            }
        }

        // Initialize default vehicle if exists
        coroutineScope.launch {
            val defaultVeh = vehicleDao.getDefaultVehicle()
            if (defaultVeh != null) {
                _activeVehicle.value = defaultVeh
            } else {
                val newId = vehicleDao.insertVehicle(
                    VehicleEntity(name = "Mi Vehículo", isDefault = true)
                )
                _activeVehicle.value = VehicleEntity(id = newId, name = "Mi Vehículo", isDefault = true)
            }
        }
    }

    fun addDebugLog(msg: ObdDebugMessage) {
        logDeque.addFirst(msg)
        while (logDeque.size > 200) {
            logDeque.removeLast()
        }
        _debugLogs.value = logDeque.toList()
    }

    fun clearDebugLogs() {
        logDeque.clear()
        _debugLogs.value = emptyList()
    }

    fun updateSettings(newSettings: ObdSettings) {
        _obdSettings.value = newSettings
        obd2Manager.currentSettings = newSettings
        bluetoothManager.setAutoReconnect(newSettings.autoReconnect)
    }

    fun updateAlertSettings(newSettings: AlertSettings) {
        _alertSettings.value = newSettings
    }

    fun setActiveVehicle(vehicle: VehicleEntity) {
        _activeVehicle.value = vehicle
    }

    fun connectToDevice(device: BluetoothDeviceModel) {
        coroutineScope.launch {
            val success = bluetoothManager.connect(device)
            if (success) {
                obd2Manager.startMonitoringSequence()
            }
        }
    }

    fun disconnect() {
        obd2Manager.stop()
        bluetoothManager.disconnect()
    }

    fun startTrip() {
        val veh = _activeVehicle.value
        tripRecorder.startTrip(veh?.id, veh?.name ?: "Vehículo Principal")
    }

    suspend fun stopTrip(): Long? {
        return tripRecorder.stopTrip()
    }

    suspend fun readDtcs(): List<DtcCode> {
        return obd2Manager.readDtcs()
    }

    suspend fun clearDtcs(): Boolean {
        return obd2Manager.clearDtcs()
    }

    suspend fun sendCustomCommand(command: String): String {
        return when (val res = elm327Manager.executeCommand(command, _obdSettings.value.commandTimeoutMs)) {
            is ElmResponse.Success -> res.raw
            is ElmResponse.NoData -> "NO DATA"
            is ElmResponse.Stopped -> "STOPPED"
            is ElmResponse.Error -> "ERROR: ${res.message}"
        }
    }

    fun toggleDemoMode(enabled: Boolean) {
        val updated = _obdSettings.value.copy(isDemoMode = enabled)
        updateSettings(updated)
    }

    fun updateGpsLocation(speedKmh: Float?, lat: Double?, lon: Double?, alt: Double?, distKm: Float?) {
        obd2Manager.updateGpsTelemetry(speedKmh, lat, lon, alt, distKm)
    }

    private fun evaluateAlerts(data: VehicleLiveData) {
        val alerts = mutableListOf<ObdAlert>()
        val settings = _alertSettings.value

        data.coolantTemperature?.let { temp ->
            if (settings.coolantTempMaxAlertEnabled && temp >= settings.coolantTempMaxLimit) {
                alerts.add(
                    ObdAlert(
                        id = "temp_high",
                        title = "TEMPERATURA ALTA",
                        message = "$temp °C (Límite: ${settings.coolantTempMaxLimit} °C)",
                        isCritical = true
                    )
                )
            }
        }

        data.batteryVoltage?.let { volt ->
            if (settings.batteryVoltageLowAlertEnabled && volt <= settings.batteryVoltageLowLimit && volt > 5f) {
                alerts.add(
                    ObdAlert(
                        id = "volt_low",
                        title = "VOLTAJE BAJO",
                        message = String.format("%.1f V (Mínimo: %.1f V)", volt, settings.batteryVoltageLowLimit),
                        isCritical = true
                    )
                )
            }
            if (settings.batteryVoltageHighAlertEnabled && volt >= settings.batteryVoltageHighLimit) {
                alerts.add(
                    ObdAlert(
                        id = "volt_high",
                        title = "SOBREVOLTAJE BATERÍA",
                        message = String.format("%.1f V (Máximo: %.1f V)", volt, settings.batteryVoltageHighLimit),
                        isCritical = true
                    )
                )
            }
        }

        data.rpm?.let { rpm ->
            if (settings.rpmHighAlertEnabled && rpm >= settings.rpmHighLimit) {
                alerts.add(
                    ObdAlert(
                        id = "rpm_high",
                        title = "RPM ELEVADAS",
                        message = "$rpm RPM (Límite: ${settings.rpmHighLimit} RPM)",
                        isCritical = false
                    )
                )
            }
        }

        data.engineLoad?.let { load ->
            if (settings.engineLoadHighAlertEnabled && load >= settings.engineLoadHighLimit) {
                alerts.add(
                    ObdAlert(
                        id = "load_high",
                        title = "CARGA MOTOR ALTA",
                        message = String.format("%.0f%% (Límite: %.0f%%)", load, settings.engineLoadHighLimit),
                        isCritical = false
                    )
                )
            }
        }

        data.intakeTemperature?.let { inTemp ->
            if (settings.intakeTempHighAlertEnabled && inTemp >= settings.intakeTempHighLimit) {
                alerts.add(
                    ObdAlert(
                        id = "intake_high",
                        title = "TEMP. ADMISIÓN ALTA",
                        message = "$inTemp °C (Límite: ${settings.intakeTempHighLimit} °C)",
                        isCritical = false
                    )
                )
            }
        }

        _activeAlerts.value = alerts
    }
}
