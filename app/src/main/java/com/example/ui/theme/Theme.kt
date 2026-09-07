package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ClusterColorScheme = darkColorScheme(
    primary = GaugeCyan,
    onPrimary = Color.Black,
    primaryContainer = ClusterSurfaceVariant,
    onPrimaryContainer = GaugeCyan,
    secondary = GaugeOrange,
    onSecondary = Color.Black,
    secondaryContainer = ClusterSurfaceVariant,
    onSecondaryContainer = GaugeOrange,
    tertiary = GaugeGreen,
    onTertiary = Color.Black,
    background = ClusterBackground,
    onBackground = TextPrimary,
    surface = ClusterSurface,
    onSurface = TextPrimary,
    surfaceVariant = ClusterSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = ClusterCardBorder,
    error = AlertError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ClusterColorScheme,
        typography = Typography,
        content = content
    )
}

