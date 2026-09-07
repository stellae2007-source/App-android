package com.example.data.obd

import com.example.data.bluetooth.BluetoothManager
import com.example.domain.model.ObdDebugMessage
import com.example.domain.model.ObdProtocol
import com.example.domain.model.ObdSettings
import kotlinx.coroutines.delay
import java.io.IOException

sealed class ElmResponse {
    data class Success(val raw: String, val cleaned: String) : ElmResponse()
    data class Error(val message: String, val isRecoverable: Boolean = true) : ElmResponse()
    object NoData : ElmResponse()
    object Stopped : ElmResponse()
}

class Elm327Manager(
    private val bluetoothManager: BluetoothManager,
    private val onDebugLog: (ObdDebugMessage) -> Unit
) {
    var detectedProtocol: String = "Desconocido"
        private set

    var detectedVin: String? = null
        private set

    suspend fun executeCommand(command: String, timeoutMs: Long = 1500L): ElmResponse {
        val trimmedCmd = command.trim()
        onDebugLog(
            ObdDebugMessage(
                direction = ObdDebugMessage.Direction.TX,
                command = trimmedCmd
            )
        )

        try {
            bluetoothManager.sendRaw(trimmedCmd)
            val raw = bluetoothManager.readUntilPrompt(timeoutMs)
            val cleaned = cleanResponse(raw, trimmedCmd)

            // Check for known ELM327 status strings
            val upper = cleaned.uppercase()
            val response = when {
                upper.contains("NO DATA") -> ElmResponse.NoData
                upper.contains("STOPPED") -> ElmResponse.Stopped
                upper.contains("BUS INIT: ERROR") || upper.contains("BUS INIT ERROR") ->
                    ElmResponse.Error("Error de inicialización del bus del vehículo (BUS INIT ERROR)")
                upper.contains("CAN ERROR") ->
                    ElmResponse.Error("Error en el bus CAN del vehículo (CAN ERROR)")
                upper.contains("UNABLE TO CONNECT") ->
                    ElmResponse.Error("No se pudo conectar con la ECU (UNABLE TO CONNECT)")
                upper.contains("ERROR") || upper.contains("?") ->
                    ElmResponse.Error("Respuesta de comando inválida ($cleaned)")
                else -> ElmResponse.Success(raw = raw, cleaned = cleaned)
            }

            val interpretation = when (response) {
                is ElmResponse.Success -> "RX: $cleaned"
                is ElmResponse.NoData -> "NO DATA (PID no responde)"
                is ElmResponse.Stopped -> "STOPPED (Tráfico detenido)"
                is ElmResponse.Error -> "ERROR: ${response.message}"
            }

            onDebugLog(
                ObdDebugMessage(
                    direction = if (response is ElmResponse.Error) ObdDebugMessage.Direction.ERROR else ObdDebugMessage.Direction.RX,
                    command = trimmedCmd,
                    rawResponse = raw,
                    interpretation = interpretation
                )
            )

            return response
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Error de comunicación con ELM327"
            onDebugLog(
                ObdDebugMessage(
                    direction = ObdDebugMessage.Direction.ERROR,
                    command = trimmedCmd,
                    rawResponse = null,
                    interpretation = "Fallo de envío: $errorMsg"
                )
            )
            return ElmResponse.Error(errorMsg, isRecoverable = true)
        }
    }

    suspend fun initializeAdapter(settings: ObdSettings): Boolean {
        // Reset and establish clean communication
        delay(200)

        // ATZ: Reset
        val atzRes = executeCommand("ATZ", 3000L)
        delay(800)

        // Turn echo off (ATE0)
        executeCommand("ATE0", settings.commandTimeoutMs)
        delay(100)

        // Turn linefeeds off (ATL0)
        executeCommand("ATL0", settings.commandTimeoutMs)

        // Turn spaces off for easier parsing (ATS0)
        executeCommand("ATS0", settings.commandTimeoutMs)

        // Turn headers off for standard responses (ATH0)
        executeCommand("ATH0", settings.commandTimeoutMs)

        // Set protocol (ATSP0 or specific)
        val protocolCmd = settings.protocol.command
        executeCommand(protocolCmd, settings.commandTimeoutMs)

        // Describe protocol (ATDP)
        val dpRes = executeCommand("ATDP", settings.commandTimeoutMs)
        if (dpRes is ElmResponse.Success) {
            detectedProtocol = dpRes.cleaned.ifBlank { settings.protocol.displayName }
        }

        // Test basic vehicle communication by sending Mode 01 PID 00 (Supported PIDs)
        var ecuConnected = false
        for (attempt in 1..3) {
            val testRes = executeCommand("0100", 3500L)
            if (testRes is ElmResponse.Success && testRes.cleaned.startsWith("4100")) {
                ecuConnected = true
                break
            }
            delay(500)
        }

        return ecuConnected
    }

    suspend fun readBatteryVoltage(): Float? {
        val res = executeCommand("ATRV", 1000L)
        if (res is ElmResponse.Success) {
            // Returns e.g. "12.6V" or "14.2V"
            val digits = res.cleaned.replace("V", "").replace("v", "").trim()
            return digits.toFloatOrNull()
        }
        return null
    }

    private fun cleanResponse(raw: String, command: String): String {
        var clean = raw.replace("\r", " ").replace("\n", " ").trim()
        // Remove echo of sent command if present
        if (clean.startsWith(command, ignoreCase = true)) {
            clean = clean.substring(command.length).trim()
        }
        // Remove spaces inside hex strings for unified parser
        clean = clean.replace(" ", "").trim()
        return clean
    }
}
