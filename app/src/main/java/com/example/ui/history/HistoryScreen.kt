package com.example.ui.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.database.TripSessionEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.ClusterBackground
import com.example.ui.theme.ClusterCardBorder
import com.example.ui.theme.ClusterSurface
import com.example.ui.theme.GaugeCyan
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.GaugeOrange
import com.example.ui.theme.GaugeRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val trips by viewModel.allTrips.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ClusterBackground)
            .padding(16.dp)
            .testTag("history_screen")
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HISTORIAL DE SESIONES (${trips.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        if (trips.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ClusterSurface)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Route,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Aún no hay viajes registrados",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pulse 'Grabar' en el cuadro de mandos o modo conducción para registrar telemetría",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(trips) { session ->
                TripCard(
                    session = session,
                    onExportCsv = { viewModel.exportTrip(context, session, "CSV") },
                    onExportJson = { viewModel.exportTrip(context, session, "JSON") },
                    onExportGpx = { viewModel.exportTrip(context, session, "GPX") },
                    onDelete = { viewModel.deleteTrip(session.id) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun TripCard(
    session: TripSessionEntity,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    onExportGpx: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateStr = sdf.format(Date(session.startTime))
    val min = session.durationSeconds / 60
    val sec = session.durationSeconds % 60
    val durationStr = String.format("%02d:%02d min", min, sec)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ClusterSurface)
            .border(1.dp, ClusterCardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Vehicle and Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = GaugeCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = session.vehicleName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = GaugeRed, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "DISTANCIA", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format(Locale.US, "%.1f km", session.distanceKm),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = GaugeGreen,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column {
                    Text(text = "DURACIÓN", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text(
                        text = durationStr,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column {
                    Text(text = "VEL. MÁX", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${session.maxSpeedKmh} km/h",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = GaugeCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column {
                    Text(text = "RPM MÁX", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${session.maxRpm}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = GaugeOrange,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary Stats: Temps & Voltage & Consumption
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Temp: ${session.minCoolantTemp}..${session.maxCoolantTemp}°C • Voltaje: ${String.format(Locale.US, "%.1f", session.minBatteryVoltage)}..${String.format(Locale.US, "%.1f", session.maxBatteryVoltage)}V",
                    fontSize = 11.sp,
                    color = TextMuted
                )

                if (session.avgConsumptionL100km > 0f) {
                    Text(
                        text = String.format(Locale.US, "%.1f L/100km", session.avgConsumptionL100km),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GaugeOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Export Actions (CSV, JSON, GPX)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onExportCsv,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = GaugeCyan),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = onExportJson,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = GaugeGreen),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("JSON", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = onExportGpx,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = GaugeOrange),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("GPX", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
