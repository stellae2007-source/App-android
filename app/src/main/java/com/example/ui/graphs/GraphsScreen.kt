package com.example.ui.graphs

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.theme.ClusterBackground
import com.example.ui.theme.ClusterCardBorder
import com.example.ui.theme.ClusterSurface
import com.example.ui.theme.GaugeBlue
import com.example.ui.theme.GaugeCyan
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.GaugeOrange
import com.example.ui.theme.GaugeRed
import com.example.ui.theme.GaugeYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class GraphMetric(val label: String, val unit: String, val color: Color, val maxRange: Float) {
    SPEED("Velocidad", "km/h", GaugeGreen, 200f),
    RPM("RPM Motor", "rpm", GaugeCyan, 7000f),
    COOLANT("Temp. Motor", "°C", GaugeOrange, 130f),
    LOAD("Carga Motor", "%", GaugeYellow, 100f),
    THROTTLE("Acelerador", "%", GaugeBlue, 100f),
    MAF("Flujo MAF", "g/s", GaugeCyan, 120f),
    VOLTAGE("Voltaje", "V", GaugeRed, 16f)
}

@Composable
fun GraphsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val liveData by viewModel.liveData.collectAsStateWithLifecycle()
    var selectedMetric by remember { mutableStateOf(GraphMetric.SPEED) }
    var secondaryMetric by remember { mutableStateOf<GraphMetric?>(GraphMetric.RPM) }

    val historyPrimary = remember { mutableStateListOf<Float>() }
    val historySecondary = remember { mutableStateListOf<Float>() }
    val maxPoints = 50

    // Append points when liveData updates
    LaunchedEffect(liveData.timestamp) {
        val pVal = when (selectedMetric) {
            GraphMetric.SPEED -> liveData.speed?.toFloat() ?: 0f
            GraphMetric.RPM -> liveData.rpm?.toFloat() ?: 0f
            GraphMetric.COOLANT -> liveData.coolantTemperature?.toFloat() ?: 0f
            GraphMetric.LOAD -> liveData.engineLoad ?: 0f
            GraphMetric.THROTTLE -> liveData.throttlePosition ?: 0f
            GraphMetric.MAF -> liveData.maf ?: 0f
            GraphMetric.VOLTAGE -> liveData.batteryVoltage ?: 0f
        }
        historyPrimary.add(pVal)
        if (historyPrimary.size > maxPoints) historyPrimary.removeAt(0)

        secondaryMetric?.let { sec ->
            val sVal = when (sec) {
                GraphMetric.SPEED -> liveData.speed?.toFloat() ?: 0f
                GraphMetric.RPM -> liveData.rpm?.toFloat() ?: 0f
                GraphMetric.COOLANT -> liveData.coolantTemperature?.toFloat() ?: 0f
                GraphMetric.LOAD -> liveData.engineLoad ?: 0f
                GraphMetric.THROTTLE -> liveData.throttlePosition ?: 0f
                GraphMetric.MAF -> liveData.maf ?: 0f
                GraphMetric.VOLTAGE -> liveData.batteryVoltage ?: 0f
            }
            historySecondary.add(sVal)
            if (historySecondary.size > maxPoints) historySecondary.removeAt(0)
        }
    }

    val currentVal = historyPrimary.lastOrNull() ?: 0f
    val minVal = if (historyPrimary.isNotEmpty()) historyPrimary.minOrNull() ?: 0f else 0f
    val maxVal = if (historyPrimary.isNotEmpty()) historyPrimary.maxOrNull() ?: 0f else 0f
    val avgVal = if (historyPrimary.isNotEmpty()) historyPrimary.average().toFloat() else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClusterBackground)
            .padding(16.dp)
            .testTag("graphs_screen")
    ) {
        Text(
            text = "TELEMETRÍA EN TIEMPO REAL",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Metric Selector Chips
        LazyRow(modifier = Modifier.fillMaxWidth()) {
            items(GraphMetric.values()) { metric ->
                val isSelected = selectedMetric == metric
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) metric.color else ClusterSurface)
                        .border(1.dp, if (isSelected) metric.color else ClusterCardBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            selectedMetric = metric
                            historyPrimary.clear()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = metric.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Live Real-Time Canvas Graph Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(ClusterSurface)
                .border(1.dp, ClusterCardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with current values and legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(selectedMetric.color))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${selectedMetric.label}: ${String.format(java.util.Locale.US, "%.1f", currentVal)} ${selectedMetric.unit}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = selectedMetric.color,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    secondaryMetric?.let { sec ->
                        val secVal = historySecondary.lastOrNull() ?: 0f
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(sec.color))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${sec.label}: ${String.format(java.util.Locale.US, "%.1f", secVal)} ${sec.unit}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = sec.color,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Canvas Waveform Drawing
                Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    val w = size.width
                    val h = size.height

                    // Grid lines
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = (h / gridLines) * i
                        drawLine(
                            color = Color(0xFF1E293B),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Draw Secondary Metric Line
                    if (historySecondary.size > 1 && secondaryMetric != null) {
                        val secMax = secondaryMetric!!.maxRange
                        val secPath = Path()
                        val stepX = w / (maxPoints - 1)

                        historySecondary.forEachIndexed { i, value ->
                            val x = i * stepX
                            val norm = (value / secMax).coerceIn(0f, 1f)
                            val y = h - (norm * h)
                            if (i == 0) secPath.moveTo(x, y) else secPath.lineTo(x, y)
                        }
                        drawPath(
                            path = secPath,
                            color = secondaryMetric!!.color.copy(alpha = 0.5f),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Draw Primary Metric Line
                    if (historyPrimary.size > 1) {
                        val pMax = selectedMetric.maxRange
                        val pPath = Path()
                        val stepX = w / (maxPoints - 1)

                        historyPrimary.forEachIndexed { i, value ->
                            val x = i * stepX
                            val norm = (value / pMax).coerceIn(0f, 1f)
                            val y = h - (norm * h)
                            if (i == 0) pPath.moveTo(x, y) else pPath.lineTo(x, y)
                        }
                        drawPath(
                            path = pPath,
                            color = selectedMetric.color,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bottom Statistics Banner (Min, Avg, Max)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatCard(title = "MÍNIMO", value = String.format(java.util.Locale.US, "%.1f", minVal), unit = selectedMetric.unit, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(10.dp))
            StatCard(title = "PROMEDIO", value = String.format(java.util.Locale.US, "%.1f", avgVal), unit = selectedMetric.unit, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(10.dp))
            StatCard(title = "MÁXIMO", value = String.format(java.util.Locale.US, "%.1f", maxVal), unit = selectedMetric.unit, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ClusterSurface)
            .border(1.dp, ClusterCardBorder, RoundedCornerShape(10.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "$value $unit", fontSize = 14.sp, fontWeight = FontWeight.Black, color = TextPrimary, fontFamily = FontFamily.Monospace)
        }
    }
}
