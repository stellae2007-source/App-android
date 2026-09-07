package com.example.ui.debug

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.domain.model.ObdDebugMessage
import com.example.ui.MainViewModel
import com.example.ui.theme.AlertError
import com.example.ui.theme.ClusterBackground
import com.example.ui.theme.ClusterCardBorder
import com.example.ui.theme.ClusterSurface
import com.example.ui.theme.GaugeCyan
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.GaugeOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DebugScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val logs by viewModel.debugLogs.collectAsStateWithLifecycle()
    var customCommand by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClusterBackground)
            .padding(16.dp)
            .testTag("debug_screen")
    ) {
        // Top Header with Clear Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TERMINAL Y TRAZA OBD-II / ELM327",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            IconButton(onClick = { viewModel.clearDebugLogs() }) {
                Icon(Icons.Default.Delete, contentDescription = "Limpiar consola", tint = GaugeOrange)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Log Console Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF090D14))
                .border(1.dp, ClusterCardBorder, RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Sin registros de comunicación aún.\nConecte un adaptador o active el modo demo para ver la traza.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), reverseLayout = true) {
                    items(logs) { log ->
                        DebugLogItem(log = log)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Preset Commands
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PresetButton("ATZ", "Reset") { customCommand = "ATZ" }
            PresetButton("ATRV", "Voltaje") { customCommand = "ATRV" }
            PresetButton("0100", "PIDs 1-20") { customCommand = "0100" }
            PresetButton("010C", "RPM") { customCommand = "010C" }
            PresetButton("0902", "VIN") { customCommand = "0902" }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Custom Command Input Field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customCommand,
                onValueChange = { customCommand = it.uppercase() },
                placeholder = { Text("Comando OBD (ej: 010D, ATRV)", color = TextMuted) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GaugeCyan,
                    unfocusedBorderColor = ClusterCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (customCommand.isNotBlank()) {
                        viewModel.sendDebugCommand(customCommand.trim())
                        customCommand = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GaugeCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Enviar")
            }
        }
    }
}

@Composable
fun PresetButton(cmd: String, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, ClusterCardBorder, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = cmd,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = GaugeCyan,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun DebugLogItem(log: ObdDebugMessage) {
    val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    val time = sdf.format(Date(log.timestamp))

    val dirColor = when (log.direction) {
        ObdDebugMessage.Direction.TX -> GaugeCyan
        ObdDebugMessage.Direction.RX -> GaugeGreen
        ObdDebugMessage.Direction.ERROR -> AlertError
        ObdDebugMessage.Direction.INFO -> GaugeOrange
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "[$time]",
            fontSize = 10.sp,
            color = TextMuted,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = log.direction.name,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = dirColor,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = log.command,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace
        )
        log.interpretation?.let {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "→ $it",
                fontSize = 11.sp,
                color = dirColor,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
