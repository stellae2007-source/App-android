package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.ui.graphics.vector.ImageVector

enum class MainDestination(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val isPrimaryBottomNav: Boolean = true
) {
    DASHBOARD("dashboard", "Cuadro", Icons.Default.Speed, true),
    DRIVING("driving", "Conducción", Icons.Default.DirectionsCar, true),
    CONNECTION("connection", "Conexión", Icons.Default.Bluetooth, true),
    DIAGNOSTICS("diagnostics", "Diagnóstico", Icons.Default.Build, true),
    HISTORY("history", "Historial", Icons.Default.History, true),
    GRAPHS("graphs", "Gráficos", Icons.Default.Insights, false),
    VEHICLES("vehicles", "Vehículos", Icons.Default.DirectionsCar, false),
    SETTINGS("settings", "Ajustes", Icons.Default.Settings, false),
    DEBUG("debug", "Terminal", Icons.Default.Terminal, false)
}
