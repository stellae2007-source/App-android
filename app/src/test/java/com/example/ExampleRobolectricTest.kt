package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.obd.ConsumptionCalculator
import com.example.data.obd.PidDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("OBD Monitor", appName)
    }

    @Test
    fun `test PID decoder RPM calculation`() {
        // Mode 01 PID 0C: ((256 * A) + B) / 4
        // For A = 0x09 (9), B = 0x2A (42) -> ((256 * 9) + 42) / 4 = 2346 / 4 = 586 RPM
        val decodedRpm = PidDecoder.decodeRpm("410C092A")
        assertEquals(586, decodedRpm)
    }

    @Test
    fun `test PID decoder Speed calculation`() {
        // Mode 01 PID 0D: A
        // For A = 0x48 (72 km/h)
        val decodedSpeed = PidDecoder.decodeSpeed("410D48")
        assertEquals(72, decodedSpeed)
    }

    @Test
    fun `test PID decoder Coolant Temperature calculation`() {
        // Mode 01 PID 05: A - 40
        // For A = 0x7E (126 - 40 = 86 °C)
        val decodedCoolant = PidDecoder.decodeCoolantTemp("41057E")
        assertEquals(86, decodedCoolant)
    }

    @Test
    fun `test consumption calculation with MAF and Speed`() {
        // 120 km/h with 25 g/s MAF
        val result = ConsumptionCalculator.calculate(
            speedKmh = 120,
            mafGramsPerSec = 25f,
            fuelRateLitersPerHour = null,
            mapKpa = 60,
            rpm = 2500,
            intakeTempC = 25
        )
        assertNotNull(result.instantL100km)
        assertTrue(result.instantL100km!! > 0f)
    }
}
