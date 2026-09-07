package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GaugeCyan
import com.example.ui.theme.GaugeOrange
import com.example.ui.theme.GaugeRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircularGauge(
    title: String,
    value: Float?,
    maxValue: Float,
    minValue: Float = 0f,
    unit: String,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp,
    primaryColor: Color = GaugeCyan,
    redlineStartValue: Float? = null,
    isUnsupported: Boolean = false
) {
    val targetNormalized = if (value != null && !isUnsupported) {
        ((value - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
    } else {
        0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = targetNormalized,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "gauge_anim"
    )

    val startAngle = 140f
    val totalSweepAngle = 260f

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = size.toPx() * 0.08f
            val arcPadding = strokeWidth / 2f
            val arcSize = Size(this.size.width - arcPadding * 2, this.size.height - arcPadding * 2)
            val arcTopLeft = Offset(arcPadding, arcPadding)

            // Background arc
            drawArc(
                color = Color(0xFF1B2330),
                startAngle = startAngle,
                sweepAngle = totalSweepAngle,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Redline zone background if specified
            redlineStartValue?.let { redline ->
                val redlineNorm = ((redline - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
                val redlineSweep = totalSweepAngle * (1f - redlineNorm)
                val redlineStart = startAngle + (totalSweepAngle * redlineNorm)
                drawArc(
                    color = GaugeRed.copy(alpha = 0.35f),
                    startAngle = redlineStart,
                    sweepAngle = redlineSweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Foreground Active Arc
            if (animatedProgress > 0f) {
                val activeColor = if (redlineStartValue != null && (value ?: 0f) >= redlineStartValue) {
                    GaugeRed
                } else if (animatedProgress > 0.75f) {
                    GaugeOrange
                } else {
                    primaryColor
                }

                drawArc(
                    color = activeColor,
                    startAngle = startAngle,
                    sweepAngle = totalSweepAngle * animatedProgress,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Subtle tick marks along perimeter
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val outerRadius = (this.size.width / 2) - strokeWidth - 4.dp.toPx()
            val innerRadius = outerRadius - 6.dp.toPx()
            val totalTicks = 13

            for (i in 0 until totalTicks) {
                val fraction = i.toFloat() / (totalTicks - 1)
                val tickAngle = Math.toRadians((startAngle + (totalSweepAngle * fraction)).toDouble())
                val p1 = Offset(
                    (center.x + outerRadius * cos(tickAngle)).toFloat(),
                    (center.y + outerRadius * sin(tickAngle)).toFloat()
                )
                val p2 = Offset(
                    (center.x + innerRadius * cos(tickAngle)).toFloat(),
                    (center.y + innerRadius * sin(tickAngle)).toFloat()
                )

                val tickColor = if (fraction <= animatedProgress) primaryColor else Color(0xFF334155)
                drawLine(
                    color = tickColor,
                    start = p1,
                    end = p2,
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (isUnsupported) {
                Text(
                    text = "NO SOPORTADO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GaugeOrange
                )
            } else if (value == null) {
                Text(
                    text = "--",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                val formattedVal = if (value >= 1000) {
                    String.format(java.util.Locale.US, "%,.0f", value)
                } else if (value % 1f == 0f) {
                    value.toInt().toString()
                } else {
                    String.format(java.util.Locale.US, "%.1f", value)
                }

                Text(
                    text = formattedVal,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = unit,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = primaryColor
            )
        }
    }
}
