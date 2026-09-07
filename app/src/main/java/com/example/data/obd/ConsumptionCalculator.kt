package com.example.data.obd

data class ConsumptionResult(
    val instantL100km: Float? = null,
    val instantKmPerL: Float? = null,
    val instantLitersPerHour: Float? = null,
    val unavailableReason: String? = null
)

object ConsumptionCalculator {

    // Stoichiometric air-to-fuel ratio for standard gasoline is 14.7:1
    private const val AIR_FUEL_RATIO_GASOLINE = 14.7f

    // Density of regular gasoline is approx 750 grams per liter (0.75 kg/L)
    private const val GASOLINE_DENSITY_G_PER_L = 750f

    fun calculate(
        speedKmh: Int?,
        mafGramsPerSec: Float?,
        fuelRateLitersPerHour: Float?,
        mapKpa: Int?,
        rpm: Int?,
        intakeTempC: Int?
    ): ConsumptionResult {
        // Priority 1: Direct engine fuel rate (PID 015E) if vehicle ECU provides it
        if (fuelRateLitersPerHour != null && fuelRateLitersPerHour > 0f) {
            val lPerH = fuelRateLitersPerHour
            if (speedKmh != null && speedKmh > 3) {
                val l100km = (lPerH / speedKmh) * 100f
                val kmPerL = if (l100km > 0f) 100f / l100km else null
                return ConsumptionResult(
                    instantL100km = l100km,
                    instantKmPerL = kmPerL,
                    instantLitersPerHour = lPerH
                )
            } else {
                // Vehicle stationary or idling (L/h)
                return ConsumptionResult(
                    instantLitersPerHour = lPerH
                )
            }
        }

        // Priority 2: Standard Mass Air Flow (MAF - PID 0110) calculation
        if (mafGramsPerSec != null && mafGramsPerSec > 0f) {
            val fuelGramsPerSec = mafGramsPerSec / AIR_FUEL_RATIO_GASOLINE
            val fuelGramsPerHour = fuelGramsPerSec * 3600f
            val lPerH = fuelGramsPerHour / GASOLINE_DENSITY_G_PER_L

            if (speedKmh != null && speedKmh > 3) {
                val l100km = (lPerH / speedKmh) * 100f
                val kmPerL = if (l100km > 0f) 100f / l100km else null
                return ConsumptionResult(
                    instantL100km = l100km,
                    instantKmPerL = kmPerL,
                    instantLitersPerHour = lPerH
                )
            } else {
                return ConsumptionResult(
                    instantLitersPerHour = lPerH
                )
            }
        }

        // Missing required PIDs for calculating consumption accurately
        return ConsumptionResult(
            unavailableReason = "Consumo no disponible para este vehículo. Falta PID 0110 (MAF) o PID 015E (Fuel Rate) en la ECU."
        )
    }
}
