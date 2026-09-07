package com.example.domain.model

enum class UnitSystem(val displayName: String) {
    METRIC("Métrico (km/h, °C, kPa)"),
    IMPERIAL("Imperial (mph, °F, psi)")
}

enum class PressureUnit(val displayName: String, val symbol: String) {
    KPA("Kilopascales", "kPa"),
    BAR("Bares", "bar"),
    PSI("Libras por pulgada²", "psi")
}

enum class ObdProtocol(val command: String, val displayName: String) {
    AUTO("ATSP0", "Automático (Recomendado - ATSP0)"),
    SAE_J1850_PWM("ATSP1", "SAE J1850 PWM (41.6 kbaud - Ford)"),
    SAE_J1850_VPW("ATSP2", "SAE J1850 VPW (10.4 kbaud - GM)"),
    ISO_9141_2("ATSP3", "ISO 9141-2 (5 baud init)"),
    ISO_14230_4_KWP_5BAUD("ATSP4", "ISO 14230-4 KWP (5 baud init)"),
    ISO_14230_4_KWP_FAST("ATSP5", "ISO 14230-4 KWP (Fast init)"),
    ISO_15765_4_CAN_11_500("ATSP6", "ISO 15765-4 CAN (11 bit ID, 500 kbaud)"),
    ISO_15765_4_CAN_29_500("ATSP7", "ISO 15765-4 CAN (29 bit ID, 500 kbaud)"),
    ISO_15765_4_CAN_11_250("ATSP8", "ISO 15765-4 CAN (11 bit ID, 250 kbaud)"),
    ISO_15765_4_CAN_29_250("ATSP9", "ISO 15765-4 CAN (29 bit ID, 250 kbaud)")
}

data class ObdSettings(
    val protocol: ObdProtocol = ObdProtocol.AUTO,
    val commandTimeoutMs: Long = 1500L,
    val maxRetries: Int = 3,
    val fastPollIntervalMs: Long = 80L,
    val slowPollIntervalMs: Long = 2500L,
    val reconnectDelayMs: Long = 4000L,
    val autoReconnect: Boolean = true,
    val logTraffic: Boolean = true,
    val showAtCommands: Boolean = true,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val pressureUnit: PressureUnit = PressureUnit.KPA,
    val isDemoMode: Boolean = false,
    val customAtInitSequence: String = "ATZ\nATE0\nATL0\nATS0\nATH0"
)
