package com.example.data.obd

import com.example.data.bluetooth.BluetoothManager
import com.example.domain.model.ConnectionState
import com.example.domain.model.DtcCode
import com.example.domain.model.ObdDebugMessage
import com.example.domain.model.ObdSettings
import com.example.domain.model.SupportedPids
import com.example.domain.model.VehicleLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin

class Obd2Manager(
    private val bluetoothManager: BluetoothManager,
    private val elm327Manager: Elm327Manager,
    private val coroutineScope: CoroutineScope,
    private val onDebugLog: (ObdDebugMessage) -> Unit
) {
    private val _liveData = MutableStateFlow(VehicleLiveData())
    val liveData: StateFlow<VehicleLiveData> = _liveData.asStateFlow()

    private val _supportedPids = MutableStateFlow(SupportedPids())
    val supportedPids: StateFlow<SupportedPids> = _supportedPids.asStateFlow()

    private val _storedDtcs = MutableStateFlow<List<DtcCode>>(emptyList())
    val storedDtcs: StateFlow<List<DtcCode>> = _storedDtcs.asStateFlow()

    private val _vinNumber = MutableStateFlow<String?>(null)
    val vinNumber: StateFlow<String?> = _vinNumber.asStateFlow()

    private val _activeProtocol = MutableStateFlow("Desconocido")
    val activeProtocol: StateFlow<String> = _activeProtocol.asStateFlow()

    private var monitoringJob: Job? = null
    private var simulationJob: Job? = null

    var currentSettings = ObdSettings()
        set(value) {
            field = value
            if (value.isDemoMode && simulationJob == null && bluetoothManager.connectionState.value != ConnectionState.MONITORING) {
                startSimulation()
            } else if (!value.isDemoMode && simulationJob != null) {
                stopSimulation()
            }
        }

    fun startMonitoringSequence() {
        monitoringJob?.cancel()
        simulationJob?.cancel()

        monitoringJob = coroutineScope.launch(Dispatchers.IO) {
            bluetoothManager.setConnectionState(ConnectionState.INITIALIZING)

            // Step 1: Initialize ELM327 with AT commands
            val initSuccess = elm327Manager.initializeAdapter(currentSettings)
            _activeProtocol.value = elm327Manager.detectedProtocol

            if (!initSuccess) {
                // If physical vehicle communication failed, report error and try reconnect
                bluetoothManager.setConnectionState(ConnectionState.ERROR)
                delay(3000)
                bluetoothManager.triggerAutoReconnect()
                return@launch
            }

            bluetoothManager.setConnectionState(ConnectionState.READY)

            // Step 2: Query Supported PIDs (0100, 0120, 0140, 0160)
            val allSupported = mutableSetOf<Int>()
            val pidBases = listOf(0x00, 0x20, 0x40, 0x60, 0x80, 0xA0)
            for (base in pidBases) {
                val cmd = String.format("01%02X", base)
                val resp = elm327Manager.executeCommand(cmd, currentSettings.commandTimeoutMs)
                if (resp is ElmResponse.Success) {
                    val pids = PidDecoder.parseSupportedPids(resp.cleaned, base)
                    allSupported.addAll(pids)
                    if (!pids.contains(base + 0x20)) {
                        // Next block not supported
                        break
                    }
                } else {
                    break
                }
                delay(50)
            }
            _supportedPids.value = SupportedPids(allSupported, allSupported.size)

            // Step 3: Attempt to read VIN (0902)
            try {
                val vinResp = elm327Manager.executeCommand("0902", 2500L)
                if (vinResp is ElmResponse.Success) {
                    val vin = PidDecoder.decodeVin(vinResp.cleaned)
                    _vinNumber.value = vin
                }
            } catch (ignored: Exception) {}

            // Step 4: Begin continuous monitoring loop
            bluetoothManager.setConnectionState(ConnectionState.MONITORING)
            runMonitoringLoop()
        }
    }

    private suspend fun runMonitoringLoop() {
        var loopCount = 0L
        var lastCycleTime = System.currentTimeMillis()

        while (coroutineScope.isActive && bluetoothManager.connectionState.value == ConnectionState.MONITORING) {
            val cycleStart = System.currentTimeMillis()
            var current = _liveData.value

            // 1. FAST POLL LOOP: High priority telemetry (RPM, Speed, Throttle, Voltage)
            // RPM (010C)
            if (_supportedPids.value.isPidSupported(0x0C) || _supportedPids.value.supportedPidsSet.isEmpty()) {
                val rpmResp = elm327Manager.executeCommand("010C", currentSettings.commandTimeoutMs)
                if (rpmResp is ElmResponse.Success) {
                    val decoded = PidDecoder.decodeRpm(rpmResp.cleaned)
                    if (decoded != null) current = current.copy(rpm = decoded)
                }
            }

            // Speed (010D)
            if (_supportedPids.value.isPidSupported(0x0D) || _supportedPids.value.supportedPidsSet.isEmpty()) {
                val speedResp = elm327Manager.executeCommand("010D", currentSettings.commandTimeoutMs)
                if (speedResp is ElmResponse.Success) {
                    val decoded = PidDecoder.decodeSpeed(speedResp.cleaned)
                    if (decoded != null) current = current.copy(speed = decoded)
                }
            }

            // Throttle (0111)
            if (_supportedPids.value.isPidSupported(0x11) || _supportedPids.value.supportedPidsSet.isEmpty()) {
                val thrResp = elm327Manager.executeCommand("0111", currentSettings.commandTimeoutMs)
                if (thrResp is ElmResponse.Success) {
                    val decoded = PidDecoder.decodeThrottlePosition(thrResp.cleaned)
                    if (decoded != null) current = current.copy(throttlePosition = decoded)
                }
            }

            // 2. MEDIUM POLL LOOP: Engine Load (0104) & Coolant Temp (0105) (every 3 cycles)
            if (loopCount % 3 == 0L) {
                if (_supportedPids.value.isPidSupported(0x04) || _supportedPids.value.supportedPidsSet.isEmpty()) {
                    val loadResp = elm327Manager.executeCommand("0104", currentSettings.commandTimeoutMs)
                    if (loadResp is ElmResponse.Success) {
                        val decoded = PidDecoder.decodeEngineLoad(loadResp.cleaned)
                        if (decoded != null) current = current.copy(engineLoad = decoded)
                    }
                }

                if (_supportedPids.value.isPidSupported(0x05) || _supportedPids.value.supportedPidsSet.isEmpty()) {
                    val tempResp = elm327Manager.executeCommand("0105", currentSettings.commandTimeoutMs)
                    if (tempResp is ElmResponse.Success) {
                        val decoded = PidDecoder.decodeCoolantTemp(tempResp.cleaned)
                        if (decoded != null) current = current.copy(coolantTemperature = decoded)
                    }
                }
            }

            // 3. SLOW POLL LOOP: MAF, MAP, Intake Temp, Voltage, Fuel Level (every 8 cycles)
            if (loopCount % 8 == 0L) {
                // MAF (0110)
                if (_supportedPids.value.isPidSupported(0x10)) {
                    val mafResp = elm327Manager.executeCommand("0110", currentSettings.commandTimeoutMs)
                    if (mafResp is ElmResponse.Success) {
                        val decoded = PidDecoder.decodeMaf(mafResp.cleaned)
                        if (decoded != null) current = current.copy(maf = decoded)
                    }
                }

                // MAP (010B)
                if (_supportedPids.value.isPidSupported(0x0B)) {
                    val mapResp = elm327Manager.executeCommand("010B", currentSettings.commandTimeoutMs)
                    if (mapResp is ElmResponse.Success) {
                        val decoded = PidDecoder.decodeMap(mapResp.cleaned)
                        if (decoded != null) current = current.copy(map = decoded)
                    }
                }

                // Intake Temp (010F)
                if (_supportedPids.value.isPidSupported(0x0F)) {
                    val inTempResp = elm327Manager.executeCommand("010F", currentSettings.commandTimeoutMs)
                    if (inTempResp is ElmResponse.Success) {
                        val decoded = PidDecoder.decodeIntakeAirTemp(inTempResp.cleaned)
                        if (decoded != null) current = current.copy(intakeTemperature = decoded)
                    }
                }

                // Battery Voltage (0142 or ATRV)
                val volt = if (_supportedPids.value.isPidSupported(0x42)) {
                    val voltResp = elm327Manager.executeCommand("0142", currentSettings.commandTimeoutMs)
                    if (voltResp is ElmResponse.Success) PidDecoder.decodeModuleVoltage(voltResp.cleaned) else null
                } else {
                    elm327Manager.readBatteryVoltage()
                }
                if (volt != null) current = current.copy(batteryVoltage = volt)

                // Fuel level (012F)
                if (_supportedPids.value.isPidSupported(0x2F)) {
                    val flResp = elm327Manager.executeCommand("012F", currentSettings.commandTimeoutMs)
                    if (flResp is ElmResponse.Success) {
                        val decoded = PidDecoder.decodeFuelLevel(flResp.cleaned)
                        if (decoded != null) current = current.copy(fuelLevel = decoded)
                    }
                }

                // Fuel trims (0106, 0107)
                if (_supportedPids.value.isPidSupported(0x06)) {
                    val stResp = elm327Manager.executeCommand("0106", currentSettings.commandTimeoutMs)
                    if (stResp is ElmResponse.Success) current = current.copy(fuelTrimShort = PidDecoder.decodeShortFuelTrimB1(stResp.cleaned))
                }
                if (_supportedPids.value.isPidSupported(0x07)) {
                    val ltResp = elm327Manager.executeCommand("0107", currentSettings.commandTimeoutMs)
                    if (ltResp is ElmResponse.Success) current = current.copy(fuelTrimLong = PidDecoder.decodeLongFuelTrimB1(ltResp.cleaned))
                }

                // Ambient temp (0146)
                if (_supportedPids.value.isPidSupported(0x46)) {
                    val ambResp = elm327Manager.executeCommand("0146", currentSettings.commandTimeoutMs)
                    if (ambResp is ElmResponse.Success) current = current.copy(ambientTemperature = PidDecoder.decodeAmbientAirTemp(ambResp.cleaned))
                }
            }

            // Compute consumption
            val consumption = ConsumptionCalculator.calculate(
                speedKmh = current.speed,
                mafGramsPerSec = current.maf,
                fuelRateLitersPerHour = current.fuelRate,
                mapKpa = current.map,
                rpm = current.rpm,
                intakeTempC = current.intakeTemperature
            )

            val now = System.currentTimeMillis()
            val latency = now - cycleStart
            val deltaSec = (now - lastCycleTime).coerceAtLeast(1) / 1000f
            val hz = if (deltaSec > 0) (1f / deltaSec).coerceIn(0.1f, 50f) else 0f
            lastCycleTime = now

            current = current.copy(
                instantConsumptionL100km = consumption.instantL100km,
                instantConsumptionKmPerL = consumption.instantKmPerL,
                instantLitersPerHour = consumption.instantLitersPerHour,
                consumptionUnavailableReason = consumption.unavailableReason,
                timestamp = now,
                latencyMs = latency,
                updateRateHz = hz
            )

            _liveData.value = current
            loopCount++
            delay(currentSettings.fastPollIntervalMs)
        }
    }

    suspend fun readDtcs(): List<DtcCode> = withContext(Dispatchers.IO) {
        if (currentSettings.isDemoMode) {
            val demoDtcs = listOf(
                DtcCode.lookup("P0171"),
                DtcCode.lookup("P0300")
            )
            _storedDtcs.value = demoDtcs
            return@withContext demoDtcs
        }

        val resp = elm327Manager.executeCommand("03", 3000L)
        if (resp is ElmResponse.Success) {
            val dtcs = PidDecoder.decodeDtcList(resp.cleaned)
            _storedDtcs.value = dtcs
            return@withContext dtcs
        }
        return@withContext emptyList()
    }

    suspend fun clearDtcs(): Boolean = withContext(Dispatchers.IO) {
        if (currentSettings.isDemoMode) {
            _storedDtcs.value = emptyList()
            return@withContext true
        }

        val resp = elm327Manager.executeCommand("04", 3000L)
        if (resp is ElmResponse.Success) {
            _storedDtcs.value = emptyList()
            return@withContext true
        }
        return@withContext false
    }

    fun startSimulation() {
        simulationJob?.cancel()
        bluetoothManager.setConnectionState(ConnectionState.MONITORING)
        _activeProtocol.value = "ISO 15765-4 CAN (11 bit, 500 kbaud) [SIMULADO]"
        _vinNumber.value = "1HGCR2F83HA129841"

        val simPids = setOf(
            0x04, 0x05, 0x06, 0x07, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
            0x10, 0x11, 0x1F, 0x21, 0x2F, 0x31, 0x33, 0x42, 0x46, 0x5E
        )
        _supportedPids.value = SupportedPids(simPids, simPids.size)

        simulationJob = coroutineScope.launch(Dispatchers.Default) {
            var simStep = 0f
            var simSpeed = 45f
            var simRpm = 1800f
            var accelerating = true

            while (isActive) {
                simStep += 0.08f
                if (accelerating) {
                    simSpeed += 1.2f
                    simRpm += 50f
                    if (simSpeed > 118f || simRpm > 4200f) accelerating = false
                } else {
                    simSpeed -= 0.8f
                    simRpm -= 35f
                    if (simSpeed < 30f || simRpm < 1200f) accelerating = true
                }

                val throttle = (simSpeed / 130f * 65f + 15f).coerceIn(10f, 95f)
                val load = (throttle * 0.85f + 12f).coerceIn(15f, 92f)
                val maf = (simRpm / 1000f * 12f + (simSpeed * 0.15f)).coerceIn(2.5f, 98f)
                val coolant = (88 + (sin(simStep * 0.2) * 4)).toInt()
                val voltage = 14.1f + (sin(simStep * 0.5).toFloat() * 0.15f)

                val consumption = ConsumptionCalculator.calculate(
                    speedKmh = simSpeed.toInt(),
                    mafGramsPerSec = maf,
                    fuelRateLitersPerHour = null,
                    mapKpa = 52,
                    rpm = simRpm.toInt(),
                    intakeTempC = 26
                )

                _liveData.value = _liveData.value.copy(
                    rpm = simRpm.toInt(),
                    speed = simSpeed.toInt(),
                    coolantTemperature = coolant,
                    engineLoad = load,
                    throttlePosition = throttle,
                    maf = maf,
                    map = 52,
                    intakeTemperature = 26,
                    fuelLevel = 68.5f,
                    batteryVoltage = voltage,
                    fuelTrimShort = 1.2f,
                    fuelTrimLong = -0.8f,
                    timingAdvance = 14.5f,
                    ambientTemperature = 22,
                    barometricPressure = 101,
                    runTimeSeconds = (simStep * 5).toInt(),
                    instantConsumptionL100km = consumption.instantL100km,
                    instantConsumptionKmPerL = consumption.instantKmPerL,
                    instantLitersPerHour = consumption.instantLitersPerHour,
                    consumptionUnavailableReason = null,
                    timestamp = System.currentTimeMillis(),
                    latencyMs = 28L,
                    updateRateHz = 14.5f
                )

                delay(70)
            }
        }
    }

    fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
        if (bluetoothManager.connectionState.value == ConnectionState.MONITORING) {
            bluetoothManager.setConnectionState(ConnectionState.DISCONNECTED)
        }
    }

    fun updateGpsTelemetry(speedKmh: Float?, lat: Double?, lon: Double?, alt: Double?, distanceKm: Float?) {
        _liveData.value = _liveData.value.copy(
            gpsSpeed = speedKmh,
            gpsLatitude = lat,
            gpsLongitude = lon,
            gpsAltitude = alt,
            gpsDistanceKm = distanceKm
        )
    }

    fun stop() {
        monitoringJob?.cancel()
        simulationJob?.cancel()
        monitoringJob = null
        simulationJob = null
    }
}
