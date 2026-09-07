package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ClusterCardBorder
import com.example.ui.theme.ClusterSurface
import com.example.ui.theme.GaugeCyan
import com.example.ui.theme.GaugeOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MetricCard(
    title: String,
    value: String?,
    unit: String,
    modifier: Modifier = Modifier,
    isUnsupported: Boolean = false,
    unsupportedReason: String? = null,
    progressFraction: Float? = null,
    accentColor: Color = GaugeCyan,
    subtext: String? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ClusterSurface)
            .border(1.dp, ClusterCardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )

                if (isUnsupported) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(GaugeOrange.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PID no soportado",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = GaugeOrange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Value and Unit
            if (isUnsupported) {
                Text(
                    text = unsupportedReason ?: "No disponible en ECU",
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = value ?: "--",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = if (value != null) TextPrimary else TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    if (unit.isNotBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = unit,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = accentColor,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }

            // Optional subtext or mini progress indicator
            if (progressFraction != null && !isUnsupported) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = accentColor,
                    trackColor = Color(0xFF1E293B)
                )
            } else if (!subtext.isNullOrBlank() && !isUnsupported) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtext,
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }
    }
}
