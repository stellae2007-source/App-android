package com.example.data.obd

import com.example.domain.model.DtcCode
import com.example.domain.model.SupportedPids

object PidDecoder {

    /**
     * Extracts byte array from response hex string.
     * Handles e.g. "410C092A" or "41 0C 09 2A"
     */
    fun extractDataBytes(response: String, expectedService: Int, expectedPid: Int): List<Int>? {
        val clean = response.replace(" ", "").uppercase()
        val expectedPrefix = String.format("%02X%02X", expectedService + 0x40, expectedPid)

        val idx = clean.indexOf(expectedPrefix)
        if (idx == -1) return null

        val dataHex = clean.substring(idx + expectedPrefix.length)
        if (dataHex.length % 2 != 0 && dataHex.length < 2) return null

        val bytes = mutableListOf<Int>()
        var i = 0
        while (i + 1 < dataHex.length) {
            val byteStr = dataHex.substring(i, i + 2)
            val byteVal = byteStr.toIntOrNull(16) ?: break
            bytes.add(byteVal)
            i += 2
        }
        return bytes
    }

    /**
     * Parses Mode 01 Supported PIDs bitmask (PIDs 0x00, 0x20, 0x40, 0x60, 0x80, 0xA0)
     */
    fun parseSupportedPids(response: String, basePid: Int): Set<Int> {
        val bytes = extractDataBytes(response, 0x01, basePid) ?: return emptySet()
        if (bytes.size < 4) return emptySet()
        return SupportedPids.parseFromBitmask(basePid, bytes.take(4))
    }

    // 01 04 - Engine Load (%)
    fun decodeEngineLoad(response: String): Float? {
        val bytes = extractDataBytes(response, 0x01, 0x04) ?: return null
        return if (bytes.isNotEmpty()) (bytes[0] * 100f) / 255f else null
    }

    // 01 05 - Coolant Temperature (°C)
    fun decodeCoolantTemp(response: String): Int? {
        val bytes = extractDataBytes(response, 0x01, 0x05) ?: return null
        return if (bytes.isNotEmpty()) bytes[0] - 40 else null
    }

    // 01 06 - Short Term Fuel Trim Bank 1 (%)
    fun decodeShortFuelTrimB1(response: String): Float? {
        val bytes = extractDataBytes(response, 0x01, 0x06) ?: return null
        return if (bytes.isNotEmpty()) ((bytes[0] - 128) * 100f) / 128f else null
    }

    // 01 07 - Long Term Fuel Trim Bank 1 (%)
    fun decodeLongFuelTrimB1(response: String): Float? {
        val bytes = extractDataBytes(response, 0x01, 0x07) ?: return null
        return if (bytes.isNotEmpty()) ((bytes[0] - 128) * 100f) / 128f else null
    }

    // 01 08 - Short Term Fuel Trim Bank 2 (%)
    fun decodeShortFuelTrimB2(response: String): Float? {
        val bytes = extractDataBytes(response, 0x01, 0x08) ?: return null
        return if (bytes.isNotEmpty()) ((bytes[0] - 128) * 100f) / 128f else null
    }

    // 01 09 - Long Term Fuel Trim Bank 2 (%)
    fun decodeLongFuelTrimB2(response: String): Float? {
        val bytes = extractDataBytes(response, 0x01, 0x09) ?: return null
        return if (bytes.isNotEmpty()) ((bytes[0] - 128) * 100f) / 128f else null
    }

    // 01 0A - Fuel Rail Pressure (kPa gauge)
    fun decodeFuelRailPressure(response: String): Float? {
        val bytes = extractDataBytes(response, 0x01, 0x0A) ?: return null
        return if (bytes.isNotEmpty()) (bytes[0] * 3f) else null
    }

    // 01 0B - Intake Manifold Absolute Pressure (kPa)
    fun decodeMap(response: String): Int? {
        val bytes = extractDataBytes(response, 0x01, 0x0B) ?: return null
        return if (bytes.isNotEmpty()) bytes[0] else null
    }

    // 01 0C - Engine RPM
    fun decodeRpm(response: String): Int? {
        val bytes = extractDataBytes(response, 0x01, 0x0C) ?: return null
        if (bytes.size < 2) return null
        return ((bytes[0] * 256) + bytes[1]) / 4
    }

    // 01 0D - Vehicle Speed (km/h)
    fun decodeSpeed(response: String): Int? {
        val bytes = extractDataBytes(response, 0x01, 0x0D) ?: return null
        return if (bytes.isNotEmpty()) bytes[0] else null
    }

    // 01 0E - Timing Advance (degrees)
    fun decodeTimingAdvance(response: String): Float? {
        val bytes = extractDataBytes(response, 0x01, 0x0E) ?: return null
        return if (bytes.isNotEmpty()) (bytes[0] / 2f) - 64f else null
    }

    // 01 0F - Intake Air Temperature (°C)
    fun decodeIntakeAirTemp(response: String): Int? {
        val bytes = extractDataBytes(response, 0x01, 0x0F) ?: return null
        return if (bytes.isNotEmpty()) bytes[0] - 40 else null
    }

    // 01 10 - MAF Air Flow Rate (g/s)
    fun decodeMaf(response: String): Float? {
        val bytes = extractDataBytes(response, 0x01, 0x10) ?: return null
        if (bytes.size < 2) return null
        return ((bytes[0] * 256) + bytes[1]) / 100f
    }

    // 01 11 - Throttle Position (%)
    fun decodeThrottlePosition(response: String): Float? {
        val bytes = extractDataBytes(response, 0x01, 0x11) ?: return null
        return if (bytes.isNotEmpty()) (bytes[0] * 100f) / 255f else null
    }

    // 01 1F - Run Time Since Engine Start (seconds)
    fun decodeRunTime(response: String): Int? {
        val bytes = extractDataBytes(response, 0x01, 0x1F) ?: return null
        if (bytes.size < 2) return null
        return (bytes[0] * 256) + bytes[1]
    }

    // 01 21 - Distance Traveled with MIL on (km)
    fun decodeDistanceMilOn(response: String): Int? {
        val bytes = extractDataBytes(response, 0x01, 0x21) ?: return null
        if (bytes.size < 2) return null
        return (bytes[0] * 256) + bytes[1]
    }

    // 01 2F - Fuel Tank Level (%)
    fun decodeFuelLevel(response: String): Float? {
        val bytes = extractDataBytes(response, 0x01, 0x2F) ?: return null
        return if (bytes.isNotEmpty()) (bytes[0] * 100f) / 255f else null
    }

    // 01 31 - Distance Traveled Since DTCs Cleared (km)
    fun decodeDistanceDtcCleared(response: String): Int? {
        val bytes = extractDataBytes(response, 0x01, 0x31) ?: return null
        if (bytes.size < 2) return null
        return (bytes[0] * 256) + bytes[1]
    }

    // 01 33 - Absolute Barometric Pressure (kPa)
    fun decodeBarometricPressure(response: String): Int? {
        val bytes = extractDataBytes(response, 0x01, 0x33) ?: return null
        return if (bytes.isNotEmpty()) bytes[0] else null
    }

    // 01 42 - Control Module Voltage (V)
    fun decodeModuleVoltage(response: String): Float? {
        val bytes = extractDataBytes(response, 0x01, 0x42) ?: return null
        if (bytes.size < 2) return null
        return ((bytes[0] * 256) + bytes[1]) / 1000f
    }

    // 01 46 - Ambient Air Temperature (°C)
    fun decodeAmbientAirTemp(response: String): Int? {
        val bytes = extractDataBytes(response, 0x01, 0x46) ?: return null
        return if (bytes.isNotEmpty()) bytes[0] - 40 else null
    }

    // 01 5E - Engine Fuel Rate (L/h)
    fun decodeFuelRate(response: String): Float? {
        val bytes = extractDataBytes(response, 0x01, 0x5E) ?: return null
        if (bytes.size < 2) return null
        return ((bytes[0] * 256) + bytes[1]) / 20f
    }

    // 09 02 - VIN (Vehicle Identification Number)
    fun decodeVin(response: String): String? {
        val clean = response.replace(" ", "").replace("\r", "").replace("\n", "").uppercase()
        val idx = clean.indexOf("4902")
        if (idx == -1) return null

        val payload = clean.substring(idx + 4)
        val sb = StringBuilder()
        var i = 0
        while (i + 1 < payload.length) {
            val hex = payload.substring(i, i + 2)
            val code = hex.toIntOrNull(16) ?: break
            // ASCII alphanumeric printable characters
            if (code in 32..126) {
                val char = code.toChar()
                if (char.isLetterOrDigit()) {
                    sb.append(char)
                }
            }
            i += 2
        }
        val candidate = sb.toString()
        return if (candidate.length >= 11) candidate.take(17) else null
    }

    // Mode 03 - Stored Diagnostic Trouble Codes (DTCs)
    fun decodeDtcList(response: String): List<DtcCode> {
        val clean = response.replace(" ", "").replace("\r", "").replace("\n", "").uppercase()
        val idx = clean.indexOf("43")
        if (idx == -1) return emptyList()

        val dtcList = mutableListOf<DtcCode>()
        val payload = clean.substring(idx + 2)

        var i = 0
        while (i + 3 < payload.length) {
            val hexA = payload.substring(i, i + 2).toIntOrNull(16) ?: break
            val hexB = payload.substring(i + 2, i + 4).toIntOrNull(16) ?: break

            if (hexA == 0 && hexB == 0) {
                // No DTC
                i += 4
                continue
            }

            // High 2 bits determine category: 00=P, 01=C, 10=B, 11=U
            val catPrefix = when ((hexA and 0xC0) shr 6) {
                0 -> "P"
                1 -> "C"
                2 -> "B"
                3 -> "U"
                else -> "P"
            }

            val secondDigit = (hexA and 0x30) shr 4
            val thirdDigit = hexA and 0x0F
            val fourthFifth = String.format("%02X", hexB)

            val fullCode = "$catPrefix$secondDigit${Integer.toHexString(thirdDigit).uppercase()}$fourthFifth"
            dtcList.add(DtcCode.lookup(fullCode))

            i += 4
        }
        return dtcList
    }
}
