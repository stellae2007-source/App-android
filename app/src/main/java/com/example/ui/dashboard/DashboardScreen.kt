package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.PressureUnit
import com.example.domain.model.UnitSystem
import com.example.ui.MainViewModel
import com.example.ui.components.AlertBanner
import com.example.ui.components.CircularGauge
import com.example.ui.components.MetricCard
import com.example.ui.components.StatusHeader
import com.example.ui.theme.ClusterBackground
import com.example.ui.theme.GaugeBlue
import com.example.ui.theme.GaugeCyan
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.GaugeOrange
import com.example.ui.theme.GaugeRed
import com.example.ui.theme.GaugeYellow
import com.example.ui.theme.TextSecondary

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val liveData by viewModel.liveData.collectAsStateWithLifecycle()
    val supportedPids by viewModel.supportedPids.collectAsStateWithLifecycle()
    val activeVehicle by viewModel.activeVehicle.collectAsStateWithLifecycle()
    val vin by viewModel.vinNumber.collectAsStateWithLifecycle()
    val protocol by viewModel.activeProtocol.collectAsStateWithLifecycle()
    val activeAlerts by viewModel.activeAlerts.collectAsStateWithLifecycle()
    val obdSettings by viewModel.obdSettings.collectAsStateWithLifecycle()
    val isRecording by viewModel.isTripRecording.collectAsStateWithLifecycle()
    val recordingDuration by viewModel.tripDurationSec.collectAsStateWithLifecycle()

    val isMetric = obdSettings.unitSystem == UnitSystem.METRIC

    // Unit conversions
    val speedValue = liveData.speed?.let { if (isMetric) it.toFloat() else it * 0.621371f }
    val speedUnit = if (isMetric) "km/h" else "mph"
    val coolantValue = liveData.coolantTemperature?.let { if (isMetric) it.toFloat() else (it * 9f / 5f) + 32f }
    val tempUnit = if (isMetric) "°C" else "°F"

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ClusterBackground)
            .testTag("dashboard_screen")
    ) {
        val isWideScreen = maxWidth >= 600.dp
        val gaugeSize = if (isWideScreen) 210.dp else 180.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Status Header
            StatusHeader(
                connectionState = connectionState,
                updateRateHz = liveData.updateRateHz,
                latencyMs = liveData.latencyMs,
                isRecording = isRecording,
                recordingDurationSec = recordingDuration,
                activeVehicleName = activeVehicle?.name,
                vin = vin,
                protocol = protocol,
                isDemoMode = obdSettings.isDemoMode,
                onToggleRecording = { viewModel.toggleTripRecording() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Active Alert Banner
            AlertBanner(alerts = activeAlerts)

            Spacer(modifier = Modifier.height(12.dp))

            // Section: Primary Digital Cluster Gauges
            Text(
                text = "CUADRO DE INSTRUMENTOS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            if (isWideScreen) {
                // Wide / Tablet Layout: 3 Gauges in a Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularGauge(
                        title = "RPM MOTOR",
                        value = liveData.rpm?.toFloat(),
                        maxValue = 8000f,
                        unit = "rpm",
                        size = gaugeSize,
                        primaryColor = GaugeCyan,
                        redlineStartValue = 6200f,
                        isUnsupported = !supportedPids.isPidSupported(0x0C) && supportedPids.supportedPidsSet.isNotEmpty()
                    )

                    CircularGauge(
                        title = "VELOCIDAD",
                        value = speedValue,
                        maxValue = if (isMetric) 240f else 150f,
                        unit = speedUnit,
                        size = gaugeSize * 1.1f,
                        primaryColor = GaugeGreen,
                        isUnsupported = !supportedPids.isPidSupported(0x0D) && supportedPids.supportedPidsSet.isNotEmpty()
                    )

                    CircularGauge(
                        title = "TEMP. MOTOR",
                        value = coolantValue,
                        maxValue = if (isMetric) 130f else 260f,
                        minValue = if (isMetric) 40f else 100f,
                        unit = tempUnit,
                        size = gaugeSize,
                        primaryColor = GaugeOrange,
                        redlineStartValue = if (isMetric) 105f else 220f,
                        isUnsupported = !supportedPids.isPidSupported(0x05) && supportedPids.supportedPidsSet.isNotEmpty()
                    )
                }
            } else {
                // Mobile Layout: Big Speed in Center, RPM and Coolant adjacent
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularGauge(
                        title = "VELOCIDAD",
                        value = speedValue,
                        maxValue = if (isMetric) 240f else 150f,
                        unit = speedUnit,
                        size = 190.dp,
                        primaryColor = GaugeGreen,
                        isUnsupported = !supportedPids.isPidSupported(0x0D) && supportedPids.supportedPidsSet.isNotEmpty()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CircularGauge(
                        title = "RPM MOTOR",
                        value = liveData.rpm?.toFloat(),
                        maxValue = 8000f,
                        unit = "rpm",
                        size = 160.dp,
                        primaryColor = GaugeCyan,
                        redlineStartValue = 6200f,
                        isUnsupported = !supportedPids.isPidSupported(0x0C) && supportedPids.supportedPidsSet.isNotEmpty()
                    )

                    CircularGauge(
                        title = "TEMP. REFRIGERANTE",
                        value = coolantValue,
                        maxValue = if (isMetric) 130f else 260f,
                        minValue = if (isMetric) 40f else 100f,
                        unit = tempUnit,
                        size = 160.dp,
                        primaryColor = GaugeOrange,
                        redlineStartValue = if (isMetric) 105f else 220f,
                        isUnsupported = !supportedPids.isPidSupported(0x05) && supportedPids.supportedPidsSet.isNotEmpty()
                    )
                }
            }

            // GPS vs OBD Speed Comparison banner if GPS is active
            if (liveData.gpsSpeed != null && liveData.speed != null) {
                val gpsSpeedConverted = if (isMetric) liveData.gpsSpeed!! else liveData.gpsSpeed!! * 0.621371f
                val delta = (speedValue ?: 0f) - gpsSpeedConverted
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "GPS: ${String.format(java.util.Locale.US, "%.0f", gpsSpeedConverted)} $speedUnit",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GaugeBlue
                    )
                    Text(
                        text = "OBD: ${speedValue?.toInt() ?: "--"} $speedUnit",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GaugeGreen
                    )
                    Text(
                        text = "Dif: ${String.format(java.util.Locale.US, "%+.0f", delta)} $speedUnit",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Secondary Compact Telemetry Cards
            Text(
                text = "TELEMETRÍA EN TIEMPO REAL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            // Grid Row 1: Carga Motor & Throttle
            Row(modifier = Modifier.fillMaxWidth()) {
                val isLoadUnsupported = !supportedPids.isPidSupported(0x04) && supportedPids.supportedPidsSet.isNotEmpty()
                MetricCard(
                    title = "Carga Motor",
                    value = liveData.engineLoad?.let { String.format(java.util.Locale.US, "%.0f", it) },
                    unit = "%",
                    isUnsupported = isLoadUnsupported,
                    progressFraction = liveData.engineLoad?.let { it / 100f },
                    accentColor = GaugeOrange,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(10.dp))

                val isThrottleUnsupported = !supportedPids.isPidSupported(0x11) && supportedPids.supportedPidsSet.isNotEmpty()
                MetricCard(
                    title = "Posición Acelerador",
                    value = liveData.throttlePosition?.let { String.format(java.util.Locale.US, "%.0f", it) },
                    unit = "%",
                    isUnsupported = isThrottleUnsupported,
                    progressFraction = liveData.throttlePosition?.let { it / 100f },
                    accentColor = GaugeCyan,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Grid Row 2: MAF & MAP
            Row(modifier = Modifier.fillMaxWidth()) {
                val isMafUnsupported = !supportedPids.isPidSupported(0x10) && supportedPids.supportedPidsSet.isNotEmpty()
                MetricCard(
                    title = "Flujo MAF",
                    value = liveData.maf?.let { String.format(java.util.Locale.US, "%.1f", it) },
                    unit = "g/s",
                    isUnsupported = isMafUnsupported,
                    accentColor = GaugeCyan,
                    subtext = "Caudalímetro de aire",
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(10.dp))

                val isMapUnsupported = !supportedPids.isPidSupported(0x0B) && supportedPids.supportedPidsSet.isNotEmpty()
                val mapDisplay = liveData.map?.let {
                    when (obdSettings.pressureUnit) {
                        PressureUnit.KPA -> "$it"
                        PressureUnit.BAR -> String.format(java.util.Locale.US, "%.2f", it / 100f)
                        PressureUnit.PSI -> String.format(java.util.Locale.US, "%.1f", it * 0.145038f)
                    }
                }
                MetricCard(
                    title = "Presión MAP",
                    value = mapDisplay,
                    unit = obdSettings.pressureUnit.symbol,
                    isUnsupported = isMapUnsupported,
                    accentColor = GaugeBlue,
                    subtext = "Admisión absoluta",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Grid Row 3: Voltaje ECU & Temp Aire Admisión
            Row(modifier = Modifier.fillMaxWidth()) {
                val isVoltUnsupported = !supportedPids.isPidSupported(0x42) && supportedPids.supportedPidsSet.isNotEmpty() && liveData.batteryVoltage == null
                MetricCard(
                    title = "Voltaje Batería / ECU",
                    value = liveData.batteryVoltage?.let { String.format(java.util.Locale.US, "%.1f", it) },
                    unit = "V",
                    isUnsupported = isVoltUnsupported,
                    accentColor = if ((liveData.batteryVoltage ?: 14f) < 12.0f) GaugeRed else GaugeGreen,
                    subtext = if ((liveData.batteryVoltage ?: 14f) > 13.5f) "Alternador cargando" else "Batería",
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(10.dp))

                val isIntakeUnsupported = !supportedPids.isPidSupported(0x0F) && supportedPids.supportedPidsSet.isNotEmpty()
                val inTemp = liveData.intakeTemperature?.let { if (isMetric) it.toFloat() else (it * 9f / 5f) + 32f }
                MetricCard(
                    title = "Temp. Admisión",
                    value = inTemp?.let { String.format(java.util.Locale.US, "%.0f", it) },
                    unit = tempUnit,
                    isUnsupported = isIntakeUnsupported,
                    accentColor = GaugeYellow,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Grid Row 4: Nivel Combustible & Consumo Instantáneo
            Row(modifier = Modifier.fillMaxWidth()) {
                val isFuelUnsupported = !supportedPids.isPidSupported(0x2F) && supportedPids.supportedPidsSet.isNotEmpty()
                MetricCard(
                    title = "Nivel Combustible",
                    value = liveData.fuelLevel?.let { String.format(java.util.Locale.US, "%.0f", it) },
                    unit = "%",
                    isUnsupported = isFuelUnsupported,
                    progressFraction = liveData.fuelLevel?.let { it / 100f },
                    accentColor = GaugeGreen,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(10.dp))

                val isStationary = (liveData.speed ?: 0) < 3
                val consValue = if (isStationary) {
                    liveData.instantLitersPerHour?.let { String.format(java.util.Locale.US, "%.1f", it) }
                } else {
                    liveData.instantConsumptionL100km?.let { String.format(java.util.Locale.US, "%.1f", it) }
                }
                val consUnit = if (isStationary) "L/h" else "L/100km"

                MetricCard(
                    title = "Consumo Instantáneo",
                    value = consValue,
                    unit = consUnit,
                    isUnsupported = liveData.consumptionUnavailableReason != null,
                    unsupportedReason = liveData.consumptionUnavailableReason,
                    accentColor = GaugeOrange,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Grid Row 5: Fuel Trims (ST / LT)
            Row(modifier = Modifier.fillMaxWidth()) {
                val isTrimUnsupported = !supportedPids.isPidSupported(0x06) && supportedPids.supportedPidsSet.isNotEmpty()
                MetricCard(
                    title = "Fuel Trim Corto B1",
                    value = liveData.fuelTrimShort?.let { String.format(java.util.Locale.US, "%+.1f", it) },
                    unit = "%",
                    isUnsupported = isTrimUnsupported,
                    accentColor = GaugeCyan,
                    subtext = "Ajuste mezcla STFT",
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(10.dp))

                val isLtTrimUnsupported = !supportedPids.isPidSupported(0x07) && supportedPids.supportedPidsSet.isNotEmpty()
                MetricCard(
                    title = "Fuel Trim Largo B1",
                    value = liveData.fuelTrimLong?.let { String.format(java.util.Locale.US, "%+.1f", it) },
                    unit = "%",
                    isUnsupported = isLtTrimUnsupported,
                    accentColor = GaugeCyan,
                    subtext = "Ajuste mezcla LTFT",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
