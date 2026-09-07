package com.example.ui.diagnostics

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.domain.model.DtcCode
import com.example.ui.MainViewModel
import com.example.ui.theme.AlertError
import com.example.ui.theme.AlertWarning
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

@Composable
fun DiagnosticsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val dtcs by viewModel.storedDtcs.collectAsStateWithLifecycle()
    val supportedPids by viewModel.supportedPids.collectAsStateWithLifecycle()
    val isClearing by viewModel.isClearingDtcs.collectAsStateWithLifecycle()
    val actionMessage by viewModel.dtcActionMessage.collectAsStateWithLifecycle()
    var showConfirmClearDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ClusterBackground)
            .padding(16.dp)
            .testTag("diagnostics_screen")
    ) {
        // Section: MIL & DTC Status Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ClusterSurface)
                    .border(1.dp, ClusterCardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (dtcs.isNotEmpty()) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (dtcs.isNotEmpty()) AlertError else GaugeGreen,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "TESTIGO CHECK ENGINE (MIL)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                                Text(
                                    text = if (dtcs.isNotEmpty()) "${dtcs.size} CÓDIGOS ACTIVOS" else "SIN AVERÍAS REGISTRADAS",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (dtcs.isNotEmpty()) AlertError else GaugeGreen
                                )
                            }
                        }
                    }

                    if (actionMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = actionMessage ?: "",
                            fontSize = 12.sp,
                            color = GaugeCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons: Read & Clear
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { viewModel.readDtcs() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GaugeCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Leer DTCs", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = { showConfirmClearDialog = true },
                            modifier = Modifier.weight(1f),
                            enabled = !isClearing,
                            colors = ButtonDefaults.buttonColors(containerColor = GaugeRed, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isClearing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Borrar DTCs", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section: DTC List
        item {
            Text(
                text = "CÓDIGOS DE DIAGNÓSTICO (DTC)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
        }

        if (dtcs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ClusterSurface)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay códigos de error almacenados en la ECU del vehículo",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        } else {
            items(dtcs) { dtc ->
                DtcCard(dtc = dtc)
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Section: Supported PIDs Matrix
        item {
            Text(
                text = "PIDs ESTÁNDAR SOPORTADOS POR LA ECU (${supportedPids.totalDiscovered})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ClusterSurface)
                    .border(1.dp, ClusterCardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val pidsSample = listOf(
                        Pair(0x04, "Carga del motor (01 04)"),
                        Pair(0x05, "Temperatura refrigerante (01 05)"),
                        Pair(0x06, "Fuel Trim corto B1 (01 06)"),
                        Pair(0x07, "Fuel Trim largo B1 (01 07)"),
                        Pair(0x0B, "Presión absoluta admisión MAP (01 0B)"),
                        Pair(0x0C, "RPM del motor (01 0C)"),
                        Pair(0x0D, "Velocidad del vehículo (01 0D)"),
                        Pair(0x0E, "Avance de encendido (01 0E)"),
                        Pair(0x0F, "Temp. aire de admisión (01 0F)"),
                        Pair(0x10, "Flujo de masa de aire MAF (01 10)"),
                        Pair(0x11, "Posición acelerador (01 11)"),
                        Pair(0x1F, "Tiempo desde arranque (01 1F)"),
                        Pair(0x2F, "Nivel de combustible (01 2F)"),
                        Pair(0x42, "Voltaje módulo de control (01 42)"),
                        Pair(0x46, "Temperatura exterior ambiente (01 46)"),
                        Pair(0x5E, "Tasa consumo combustible (01 5E)")
                    )

                    pidsSample.forEach { (pid, name) ->
                        val isSupported = supportedPids.isPidSupported(pid)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = name,
                                fontSize = 12.sp,
                                color = if (isSupported) TextPrimary else TextMuted
                            )
                            Text(
                                text = if (isSupported) "SOPORTADO" else "NO DISPONIBLE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSupported) GaugeGreen else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Safety Confirmation Dialog for Clearing DTCs
    if (showConfirmClearDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearDialog = false },
            title = {
                Text(
                    text = "Confirmar Borrado de Averías",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlertError
                )
            },
            text = {
                Text(
                    text = "Esta acción enviará el comando Modo 04 a la ECU. Se borrarán los códigos de avería almacenados y se apagará el testigo MIL (Check Engine).\n\n" +
                            "El motor debe estar apagado pero con el contacto en posición de encendido (ON). ¿Desea proceder?",
                    fontSize = 13.sp,
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmClearDialog = false
                        viewModel.clearDtcs()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertError)
                ) {
                    Text("Borrar Averías", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = ClusterSurface
        )
    }
}

@Composable
fun DtcCard(dtc: DtcCode) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ClusterSurface)
            .border(1.dp, AlertError.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dtc.code,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = AlertError,
                    fontFamily = FontFamily.Monospace
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(AlertError.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = dtc.category.displayName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AlertError
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = dtc.description,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
    }
}
