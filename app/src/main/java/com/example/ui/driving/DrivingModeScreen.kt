package com.example.ui.driving

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.UnitSystem
import com.example.ui.MainViewModel
import com.example.ui.theme.AlertError
import com.example.ui.theme.AlertWarning
import com.example.ui.theme.ClusterCardBorder
import com.example.ui.theme.ClusterSurface
import com.example.ui.theme.GaugeCyan
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.GaugeOrange
import com.example.ui.theme.GaugeRed
import com.example.ui.theme.GaugeYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DrivingModeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val liveData by viewModel.liveData.collectAsStateWithLifecycle()
    val activeAlerts by viewModel.activeAlerts.collectAsStateWithLifecycle()
    val obdSettings by viewModel.obdSettings.collectAsStateWithLifecycle()
    val isRecording by viewModel.isTripRecording.collectAsStateWithLifecycle()
    val tripDuration by viewModel.tripDurationSec.collectAsStateWithLifecycle()
    val tripDistance by viewModel.tripDistanceKm.collectAsStateWithLifecycle()

    // Keep screen awake while in Driving Mode
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val isMetric = obdSettings.unitSystem == UnitSystem.METRIC
    val speedValue = liveData.speed?.let { if (isMetric) it else (it * 0.621371f).toInt() }
    val speedUnit = if (isMetric) "km/h" else "mph"

    val rpmValue = liveData.rpm ?: 0
    val rpmFraction = (rpmValue / 7000f).coerceIn(0f, 1f)
    val rpmBarColor = if (rpmValue >= 5800) GaugeRed else if (rpmValue >= 4200) GaugeOrange else GaugeCyan

    val coolantTemp = liveData.coolantTemperature
    val isTempHigh = (coolantTemp ?: 0) >= 105

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            .testTag("driving_mode_screen"),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Section: Critical Alerts & Status
        Column(modifier = Modifier.fillMaxWidth()) {
            if (activeAlerts.isNotEmpty()) {
                val critical = activeAlerts.firstOrNull { it.isCritical } ?: activeAlerts.first()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (critical.isCritical) AlertError else AlertWarning)
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = critical.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                            Text(
                                text = critical.message,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MODO CONDUCCIÓN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(GaugeGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PANTALLA ACTIVA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GaugeGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // RPM Shift / Tachometer Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "RPM: ${if (liveData.rpm != null) String.format(java.util.Locale.US, "%,d", liveData.rpm) else "--"}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = rpmBarColor,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "LÍMITE 6,500",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { rpmFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = rpmBarColor,
                    trackColor = Color(0xFF1B2330)
                )
            }
        }

        // Center: Giant Digital Speedometer
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = speedValue?.toString() ?: "--",
                fontSize = 108.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                lineHeight = 110.sp
            )

            Text(
                text = speedUnit.uppercase(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = GaugeGreen,
                letterSpacing = 2.sp
            )
        }

        // Bottom Section: Crucial Secondary Gauges & Trip Recording Button
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Coolant Temp Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isTempHigh) GaugeRed.copy(alpha = 0.2f) else ClusterSurface)
                        .border(1.dp, if (isTempHigh) GaugeRed else ClusterCardBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TEMP. MOTOR",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = coolantTemp?.let { "$it°C" } ?: "--°C",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isTempHigh) GaugeRed else TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Voltage Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ClusterSurface)
                        .border(1.dp, ClusterCardBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "VOLTAJE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = liveData.batteryVoltage?.let { String.format(java.util.Locale.US, "%.1fV", it) } ?: "--V",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = GaugeGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Throttle / Load Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ClusterSurface)
                        .border(1.dp, ClusterCardBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "CARGA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = liveData.engineLoad?.let { String.format(java.util.Locale.US, "%.0f%%", it) } ?: "--%",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = GaugeOrange,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Large Recording Button & Trip Metrics
            Button(
                onClick = { viewModel.toggleTripRecording() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("driving_trip_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) GaugeRed else GaugeCyan,
                    contentColor = if (isRecording) Color.White else Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (isRecording) {
                    val min = tripDuration / 60
                    val sec = tripDuration % 60
                    Text(
                        text = "DETENER GRABACIÓN (${String.format("%02d:%02d", min, sec)} • ${String.format(java.util.Locale.US, "%.1f km", tripDistance)})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                } else {
                    Text(
                        text = "INICIAR GRABACIÓN DE VIAJE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
