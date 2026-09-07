package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ConnectionState
import com.example.ui.theme.ClusterCardBorder
import com.example.ui.theme.ClusterSurface
import com.example.ui.theme.GaugeCyan
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.GaugeOrange
import com.example.ui.theme.GaugeRed
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusDisconnected
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusScanning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StatusHeader(
    connectionState: ConnectionState,
    updateRateHz: Float,
    latencyMs: Long,
    isRecording: Boolean,
    recordingDurationSec: Long,
    activeVehicleName: String?,
    vin: String?,
    protocol: String?,
    isDemoMode: Boolean,
    onToggleRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (connectionState) {
        ConnectionState.MONITORING, ConnectionState.CONNECTED -> StatusConnected
        ConnectionState.SCANNING, ConnectionState.CONNECTING, ConnectionState.INITIALIZING -> StatusScanning
        ConnectionState.RECONNECTING -> GaugeOrange
        ConnectionState.ERROR -> StatusError
        ConnectionState.DISCONNECTED, ConnectionState.READY -> StatusDisconnected
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ClusterSurface)
            .border(1.dp, ClusterCardBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Connection State Pill
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isDemoMode) "MODO SIMULACIÓN OBD" else connectionState.displayName.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDemoMode) GaugeCyan else statusColor,
                        letterSpacing = 0.5.sp
                    )
                }

                // Telemetry Stats: Hz & Latency
                if (connectionState == ConnectionState.MONITORING) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f Hz", updateRateHz),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GaugeGreen,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${latencyMs}ms",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subrow with Vehicle, VIN & Trip Recording Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeVehicleName ?: "Vehículo no configurado",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    val details = listOfNotNull(
                        vin?.let { "VIN: $it" },
                        protocol?.takeIf { it != "Desconocido" }?.let { it.take(24) }
                    ).joinToString(" • ")

                    if (details.isNotBlank()) {
                        Text(
                            text = details,
                            fontSize = 10.sp,
                            color = TextMuted,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Trip Recording Action
                Button(
                    onClick = onToggleRecording,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) GaugeRed else Color(0xFF1E293B),
                        contentColor = if (isRecording) Color.White else GaugeCyan
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (isRecording) {
                        val min = recordingDurationSec / 60
                        val sec = recordingDurationSec % 60
                        Text(
                            text = String.format("%02d:%02d", min, sec),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        Text(
                            text = "GRABAR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
