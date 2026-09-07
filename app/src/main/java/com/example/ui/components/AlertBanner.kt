package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ObdAlert
import com.example.ui.theme.AlertError
import com.example.ui.theme.AlertWarning

@Composable
fun AlertBanner(
    alerts: List<ObdAlert>,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = alerts.isNotEmpty()) {
        Column(modifier = modifier.fillMaxWidth()) {
            alerts.forEach { alert ->
                val bg = if (alert.isCritical) AlertError.copy(alpha = 0.15f) else AlertWarning.copy(alpha = 0.15f)
                val border = if (alert.isCritical) AlertError else AlertWarning
                val iconColor = if (alert.isCritical) AlertError else AlertWarning

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .border(1.dp, border, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alerta",
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "⚠ ${alert.title}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = iconColor,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = alert.message,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
