package com.example.data.bluetooth

data class BluetoothDeviceModel(
    val name: String,
    val address: String,
    val isBonded: Boolean,
    val isLikelyObd: Boolean = false,
    val rssi: Int = 0
) {
    companion object {
        private val OBD_KEYWORDS = listOf(
            "OBD", "OBDII", "OBD2", "ELM327", "V-LINK", "VLINK",
            "OBDLINK", "VIEOCAR", "CARISTA", "VEEPEAK", "KONNWEI",
            "SCANTOOL", "VGATE", "ECU"
        )

        fun checkLikelyObd(deviceName: String?): Boolean {
            if (deviceName.isNullOrBlank()) return false
            val upper = deviceName.uppercase()
            return OBD_KEYWORDS.any { upper.contains(it) }
        }
    }
}
