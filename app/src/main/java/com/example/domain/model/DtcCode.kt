package com.example.domain.model

enum class DtcCategory(val displayName: String) {
    POWERTRAIN("Tren Motriz (Motor/Transmisión)"),
    CHASSIS("Chasis (Frenos/Suspensión)"),
    BODY("Carrocería / Confort"),
    NETWORK("Red / Comunicación bus")
}

data class DtcCode(
    val code: String,
    val description: String,
    val category: DtcCategory,
    val isSevere: Boolean = false
) {
    companion object {
        fun lookup(code: String): DtcCode {
            val upper = code.uppercase().trim()
            val category = when (upper.firstOrNull()) {
                'P' -> DtcCategory.POWERTRAIN
                'C' -> DtcCategory.CHASSIS
                'B' -> DtcCategory.BODY
                'U' -> DtcCategory.NETWORK
                else -> DtcCategory.POWERTRAIN
            }

            val desc = when (upper) {
                "P0100" -> "Fallo en circuito del sensor de flujo de aire (MAF/VAF)"
                "P0101" -> "Rendimiento/rango del sensor de flujo de masa de aire (MAF)"
                "P0102" -> "Entrada baja en circuito del sensor MAF"
                "P0103" -> "Entrada alta en circuito del sensor MAF"
                "P0115" -> "Fallo en circuito de temperatura de refrigerante (ECT)"
                "P0117" -> "Entrada baja de sensor temperatura refrigerante"
                "P0118" -> "Entrada alta de sensor temperatura refrigerante"
                "P0120" -> "Fallo circuito sensor posición mariposa (TPS)/Pedal A"
                "P0125" -> "Temperatura insuficiente para control de combustible cerrado"
                "P0130" -> "Fallo en circuito del sensor de oxígeno (Banco 1 Sensor 1)"
                "P0133" -> "Respuesta lenta del sensor de oxígeno (Banco 1 Sensor 1)"
                "P0171" -> "Sistema demasiado pobre (Banco 1)"
                "P0172" -> "Sistema demasiado rico (Banco 1)"
                "P0201" -> "Fallo de inyector cilindro 1"
                "P0300" -> "Fallo de encendido detectado en múltiples cilindros"
                "P0301" -> "Fallo de encendido detectado - Cilindro 1"
                "P0302" -> "Fallo de encendido detectado - Cilindro 2"
                "P0303" -> "Fallo de encendido detectado - Cilindro 3"
                "P0304" -> "Fallo de encendido detectado - Cilindro 4"
                "P0325" -> "Fallo en circuito sensor de detonación (Knock Sensor 1)"
                "P0340" -> "Fallo en circuito sensor de posición árbol de levas (CMP)"
                "P0420" -> "Eficiencia del catalizador por debajo del umbral (Banco 1)"
                "P0440" -> "Fallo en sistema de control de emisiones por evaporación (EVAP)"
                "P0500" -> "Fallo en sensor de velocidad del vehículo (VSS)"
                "P0505" -> "Fallo en sistema de control de ralentí (IAC)"
                "P0562" -> "Voltaje bajo del sistema del vehículo"
                "P0600" -> "Fallo de comunicación serie interna"
                "U0100" -> "Pérdida de comunicación con el ECM/PCM (ECU Motor)"
                "U0101" -> "Pérdida de comunicación con el TCM (Transmisión)"
                "U0121" -> "Pérdida de comunicación con el módulo de control ABS"
                "C0035" -> "Circuito sensor velocidad rueda delantera izquierda"
                "B0001" -> "Control de despliegue airbag conductor frontal"
                else -> "Código OBD-II genérico detectado en la centralita ($upper)"
            }

            val severe = upper.startsWith("P03") || upper in listOf("P0118", "P0117", "P0562", "U0100")

            return DtcCode(
                code = upper,
                description = desc,
                category = category,
                isSevere = severe
            )
        }
    }
}
