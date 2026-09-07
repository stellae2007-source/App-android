package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ObdApplication
import com.example.data.bluetooth.BluetoothDeviceModel
import com.example.data.database.TripSessionEntity
import com.example.data.database.VehicleEntity
import com.example.domain.model.AlertSettings
import com.example.domain.model.ConnectionState
import com.example.domain.model.DtcCode
import com.example.domain.model.ObdAlert
import com.example.domain.model.ObdDebugMessage
import com.example.domain.model.ObdSettings
import com.example.domain.model.SupportedPids
import com.example.domain.model.VehicleLiveData
import com.example.utils.ExportHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val app = ObdApplication.instance
    private val repo = app.obdRepository
    private val btManager = app.bluetoothManager
    private val vehicleRepo = app.vehicleRepository
    private val tripDao = app.database.tripDao()

    val connectionState: StateFlow<ConnectionState> = repo.connectionState
    val liveData: StateFlow<VehicleLiveData> = repo.liveData
    val supportedPids: StateFlow<SupportedPids> = repo.supportedPids
    val storedDtcs: StateFlow<List<DtcCode>> = repo.storedDtcs
    val activeVehicle: StateFlow<VehicleEntity?> = repo.activeVehicle
    val activeProtocol: StateFlow<String> = repo.activeProtocol
    val vinNumber: StateFlow<String?> = repo.vinNumber
    val activeAlerts: StateFlow<List<ObdAlert>> = repo.activeAlerts
    val obdSettings: StateFlow<ObdSettings> = repo.obdSettings
    val alertSettings: StateFlow<AlertSettings> = repo.alertSettings
    val debugLogs: StateFlow<List<ObdDebugMessage>> = repo.debugLogs

    val isBluetoothEnabled: StateFlow<Boolean> = btManager.isBluetoothEnabled
    val pairedDevices: StateFlow<List<BluetoothDeviceModel>> = btManager.pairedDevices
    val discoveredDevices: StateFlow<List<BluetoothDeviceModel>> = btManager.discoveredDevices

    val isTripRecording: StateFlow<Boolean> = repo.tripRecorder.isRecording
    val tripDurationSec: StateFlow<Long> = repo.tripRecorder.currentSessionDurationSec
    val tripDistanceKm: StateFlow<Float> = repo.tripRecorder.currentDistanceKm

    val allTrips: StateFlow<List<TripSessionEntity>> = tripDao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVehicles: StateFlow<List<VehicleEntity>> = vehicleRepo.allVehicles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isClearingDtcs = MutableStateFlow(false)
    val isClearingDtcs: StateFlow<Boolean> = _isClearingDtcs.asStateFlow()

    private val _dtcActionMessage = MutableStateFlow<String?>(null)
    val dtcActionMessage: StateFlow<String?> = _dtcActionMessage.asStateFlow()

    fun startBluetoothDiscovery() {
        btManager.startDiscovery()
    }

    fun stopBluetoothDiscovery() {
        btManager.stopDiscovery()
    }

    fun refreshPairedDevices() {
        btManager.refreshPairedDevices()
    }

    fun connectDevice(device: BluetoothDeviceModel) {
        repo.connectToDevice(device)
    }

    fun disconnect() {
        repo.disconnect()
    }

    fun toggleTripRecording() {
        viewModelScope.launch {
            if (isTripRecording.value) {
                repo.stopTrip()
            } else {
                repo.startTrip()
            }
        }
    }

    fun readDtcs() {
        viewModelScope.launch {
            _dtcActionMessage.value = "Consultando códigos de avería (Modo 03)..."
            val dtcs = repo.readDtcs()
            _dtcActionMessage.value = if (dtcs.isEmpty()) {
                "No se encontraron códigos de avería almacenados en la ECU."
            } else {
                "Se encontraron ${dtcs.size} códigos de avería."
            }
        }
    }

    fun clearDtcs() {
        viewModelScope.launch {
            _isClearingDtcs.value = true
            _dtcActionMessage.value = "Borrando códigos de avería (Modo 04)..."
            val success = repo.clearDtcs()
            _isClearingDtcs.value = false
            _dtcActionMessage.value = if (success) {
                "Códigos de avería borrados correctamente. Testigo MIL apagado."
            } else {
                "Fallo al borrar códigos. Asegúrese de que el motor está apagado con contacto puesto."
            }
        }
    }

    fun clearDtcActionMessage() {
        _dtcActionMessage.value = null
    }

    fun updateSettings(newSettings: ObdSettings) {
        repo.updateSettings(newSettings)
    }

    fun updateAlertSettings(newSettings: AlertSettings) {
        repo.updateAlertSettings(newSettings)
    }

    fun toggleDemoMode(enabled: Boolean) {
        repo.toggleDemoMode(enabled)
    }

    fun sendDebugCommand(command: String) {
        viewModelScope.launch {
            repo.sendCustomCommand(command)
        }
    }

    fun clearDebugLogs() {
        repo.clearDebugLogs()
    }

    fun deleteTrip(sessionId: Long) {
        viewModelScope.launch {
            tripDao.deleteSessionById(sessionId)
        }
    }

    fun exportTrip(context: Context, session: TripSessionEntity, format: String) {
        viewModelScope.launch {
            val points = tripDao.getPointsForSession(session.id)
            when (format.uppercase()) {
                "CSV" -> {
                    val csv = ExportHelper.generateCsv(session, points)
                    ExportHelper.shareExport(context, "viaje_${session.id}.csv", csv, "text/csv")
                }
                "JSON" -> {
                    val json = ExportHelper.generateJson(session, points)
                    ExportHelper.shareExport(context, "viaje_${session.id}.json", json, "application/json")
                }
                "GPX" -> {
                    val gpx = ExportHelper.generateGpx(session, points)
                    ExportHelper.shareExport(context, "viaje_${session.id}.gpx", gpx, "application/gpx+xml")
                }
            }
        }
    }

    fun addVehicle(name: String, vin: String? = null, protocol: String? = null) {
        viewModelScope.launch {
            vehicleRepo.addVehicle(name, vin, protocol)
        }
    }

    fun selectVehicle(vehicle: VehicleEntity) {
        repo.setActiveVehicle(vehicle)
    }

    fun deleteVehicle(vehicleId: Long) {
        viewModelScope.launch {
            vehicleRepo.deleteVehicle(vehicleId)
        }
    }

    fun updateGpsTelemetry(speedKmh: Float?, lat: Double?, lon: Double?, alt: Double?, distKm: Float?) {
        repo.updateGpsLocation(speedKmh, lat, lon, alt, distKm)
    }
}
