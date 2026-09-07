package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.ObdProtocol
import com.example.domain.model.PressureUnit
import com.example.domain.model.UnitSystem
import com.example.ui.MainViewModel
import com.example.ui.theme.ClusterBackground
import com.example.ui.theme.ClusterCardBorder
import com.example.ui.theme.ClusterSurface
import com.example.ui.theme.GaugeCyan
import com.example.ui.theme.GaugeOrange
import com.example.ui.theme.GaugeRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val obdSettings by viewModel.obdSettings.collectAsStateWithLifecycle()
    val alertSettings by viewModel.alertSettings.collectAsStateWithLifecycle()

    var protocolMenuExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ClusterBackground)
            .padding(16.dp)
            .testTag("settings_screen")
    ) {
        // Section: OBD2 Protocol & Communication
        item {
            Text(
                text = "PROTOCOLO Y COMUNICACIÓN OBD-II",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ClusterSurface)
                    .border(1.dp, ClusterCardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Protocol Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { protocolMenuExpanded = true }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Protocolo OBD-II", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = obdSettings.protocol.displayName, fontSize = 12.sp, color = GaugeCyan)
                        }
                        Text(text = "Cambiar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GaugeCyan)
                    }

                    DropdownMenu(
                        expanded = protocolMenuExpanded,
                        onDismissRequest = { protocolMenuExpanded = false }
                    ) {
                        ObdProtocol.values().forEach { proto ->
                            DropdownMenuItem(
                                text = { Text(proto.displayName) },
                                onClick = {
                                    viewModel.updateSettings(obdSettings.copy(protocol = proto))
                                    protocolMenuExpanded = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Auto-Reconnect Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Reconexión Automática", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "Reintenta conectar si se corta la señal Bluetooth", fontSize = 11.sp, color = TextMuted)
                        }
                        Switch(
                            checked = obdSettings.autoReconnect,
                            onCheckedChange = { viewModel.updateSettings(obdSettings.copy(autoReconnect = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = GaugeCyan)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Fast Poll Interval Slider
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Frecuencia de Muestreo Rápida", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text(text = "${obdSettings.fastPollIntervalMs} ms", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GaugeCyan, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = obdSettings.fastPollIntervalMs.toFloat(),
                            onValueChange = { viewModel.updateSettings(obdSettings.copy(fastPollIntervalMs = it.toLong())) },
                            valueRange = 30f..400f,
                            steps = 7,
                            colors = SliderDefaults.colors(thumbColor = GaugeCyan, activeTrackColor = GaugeCyan)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        // Section: Units & Display
        item {
            Text(
                text = "UNIDADES Y SISTEMA DE MEDIDA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ClusterSurface)
                    .border(1.dp, ClusterCardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Metric vs Imperial
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Sistema de Unidades", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                text = if (obdSettings.unitSystem == UnitSystem.METRIC) "Métrico (km/h, °C, L/100km)" else "Imperial (mph, °F, MPG)",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                        Row {
                            UnitPill(
                                label = "Métrico",
                                isSelected = obdSettings.unitSystem == UnitSystem.METRIC,
                                onClick = { viewModel.updateSettings(obdSettings.copy(unitSystem = UnitSystem.METRIC)) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            UnitPill(
                                label = "Imperial",
                                isSelected = obdSettings.unitSystem == UnitSystem.IMPERIAL,
                                onClick = { viewModel.updateSettings(obdSettings.copy(unitSystem = UnitSystem.IMPERIAL)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Pressure Unit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Unidad Presión Admisión (MAP)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Row {
                            PressureUnit.values().forEach { pUnit ->
                                UnitPill(
                                    label = pUnit.symbol,
                                    isSelected = obdSettings.pressureUnit == pUnit,
                                    onClick = { viewModel.updateSettings(obdSettings.copy(pressureUnit = pUnit)) }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        // Section: Safety Alert Thresholds
        item {
            Text(
                text = "LÍMITES DE ALERTA DE SEGURIDAD",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ClusterSurface)
                    .border(1.dp, ClusterCardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Max Coolant Temp Alert
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Alerta Temp. Máxima Refrigerante", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text(text = "${alertSettings.coolantTempMaxLimit} °C", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GaugeOrange, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = alertSettings.coolantTempMaxLimit.toFloat(),
                            onValueChange = { viewModel.updateAlertSettings(alertSettings.copy(coolantTempMaxLimit = it.toInt())) },
                            valueRange = 85f..120f,
                            steps = 7,
                            colors = SliderDefaults.colors(thumbColor = GaugeOrange, activeTrackColor = GaugeOrange)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Low Battery Voltage Alert
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Alerta Voltaje Batería Bajo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text(text = String.format(java.util.Locale.US, "%.1f V", alertSettings.batteryVoltageLowLimit), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GaugeRed, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = alertSettings.batteryVoltageLowLimit,
                            onValueChange = { viewModel.updateAlertSettings(alertSettings.copy(batteryVoltageLowLimit = it)) },
                            valueRange = 10.5f..12.6f,
                            steps = 7,
                            colors = SliderDefaults.colors(thumbColor = GaugeRed, activeTrackColor = GaugeRed)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // High RPM Alert
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Alerta Límite RPM", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text(text = "${alertSettings.rpmHighLimit} RPM", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GaugeCyan, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = alertSettings.rpmHighLimit.toFloat(),
                            onValueChange = { viewModel.updateAlertSettings(alertSettings.copy(rpmHighLimit = it.toInt())) },
                            valueRange = 4000f..7500f,
                            steps = 7,
                            colors = SliderDefaults.colors(thumbColor = GaugeCyan, activeTrackColor = GaugeCyan)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun UnitPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) GaugeCyan else Color(0xFF1E293B))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.Black else TextSecondary
        )
    }
}
