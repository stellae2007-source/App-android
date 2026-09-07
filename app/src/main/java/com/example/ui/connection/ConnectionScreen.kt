package com.example.ui.connection

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.bluetooth.BluetoothDeviceModel
import com.example.domain.model.ConnectionState
import com.example.ui.MainViewModel
import com.example.ui.theme.ClusterBackground
import com.example.ui.theme.ClusterCardBorder
import com.example.ui.theme.ClusterSurface
import com.example.ui.theme.GaugeCyan
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.GaugeOrange
import com.example.ui.theme.GaugeRed
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusScanning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ConnectionScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isBtEnabled by viewModel.isBluetoothEnabled.collectAsStateWithLifecycle()
    val pairedDevices by viewModel.pairedDevices.collectAsStateWithLifecycle()
    val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val obdSettings by viewModel.obdSettings.collectAsStateWithLifecycle()

    val isScanning = connectionState == ConnectionState.SCANNING
    val isConnecting = connectionState == ConnectionState.CONNECTING || connectionState == ConnectionState.INITIALIZING
    val isConnected = connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.MONITORING

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ClusterBackground)
            .padding(16.dp)
            .testTag("connection_screen")
    ) {
        // Section: Active Connection Status Card
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
                                imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = if (isConnected) StatusConnected else TextSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "ESTADO BLUETOOTH",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                                Text(
                                    text = connectionState.displayName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isConnected) StatusConnected else TextPrimary
                                )
                            }
                        }

                        if (isConnecting || isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = GaugeCyan,
                                strokeWidth = 2.5.dp
                            )
                        } else if (isConnected) {
                            Button(
                                onClick = { viewModel.disconnect() },
                                colors = ButtonDefaults.buttonColors(containerColor = GaugeRed.copy(alpha = 0.2f), contentColor = GaugeRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("disconnect_button")
                            ) {
                                Text("Desconectar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Demo Simulation Mode Switch Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF131A26))
                            .border(1.dp, Color(0xFF1F293D), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = GaugeCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Modo Simulación / Demo",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Genera telemetría dinámica sin adaptador físico",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            Switch(
                                checked = obdSettings.isDemoMode,
                                onCheckedChange = { viewModel.toggleDemoMode(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = GaugeCyan
                                ),
                                modifier = Modifier.testTag("demo_mode_switch")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section: Scan Controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DISPOSITIVOS BLUETOOTH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                Row {
                    IconButton(onClick = { viewModel.refreshPairedDevices() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar vinculados", tint = GaugeCyan)
                    }

                    Button(
                        onClick = {
                            if (isScanning) viewModel.stopBluetoothDiscovery() else viewModel.startBluetoothDiscovery()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScanning) GaugeOrange else GaugeCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("scan_button")
                    ) {
                        Icon(
                            imageVector = if (isScanning) Icons.Default.BluetoothConnected else Icons.Default.BluetoothSearching,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isScanning) "Detener" else "Buscar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Section: Paired Devices Header
        if (pairedDevices.isNotEmpty()) {
            item {
                Text(
                    text = "Dispositivos ya vinculados",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            items(pairedDevices) { device ->
                DeviceItem(
                    device = device,
                    isConnecting = isConnecting,
                    onConnect = { viewModel.connectDevice(device) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Section: Discovered Devices Header
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isScanning) "Buscando dispositivos cercanos..." else "Dispositivos encontrados",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }

        if (discoveredDevices.isEmpty() && !isScanning) {
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
                        text = "Pulse 'Buscar' para detectar adaptadores ELM327 cercanos",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }
            }
        } else {
            items(discoveredDevices) { device ->
                DeviceItem(
                    device = device,
                    isConnecting = isConnecting,
                    onConnect = { viewModel.connectDevice(device) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun DeviceItem(
    device: BluetoothDeviceModel,
    isConnecting: Boolean,
    onConnect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ClusterSurface)
            .border(
                1.dp,
                if (device.isLikelyObd) GaugeCyan.copy(alpha = 0.5f) else ClusterCardBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable(enabled = !isConnecting) { onConnect() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (device.isLikelyObd) GaugeCyan.copy(alpha = 0.15f) else Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (device.isLikelyObd) Icons.Default.DirectionsCar else Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = if (device.isLikelyObd) GaugeCyan else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = device.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        if (device.isLikelyObd) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(GaugeCyan.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "OBD2",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GaugeCyan
                                )
                            }
                        }
                    }

                    Text(
                        text = device.address,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Button(
                onClick = onConnect,
                enabled = !isConnecting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (device.isLikelyObd) GaugeCyan else Color(0xFF2563EB),
                    contentColor = if (device.isLikelyObd) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Conectar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
