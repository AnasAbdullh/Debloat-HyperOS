package com.debloat.hyperos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.debloat.hyperos.R
import com.debloat.hyperos.ui.theme.AccentOrange
import com.debloat.hyperos.ui.theme.AccentOrangeVariant
import com.debloat.hyperos.ui.theme.SuccessGreen
import androidx.compose.ui.graphics.Brush

@Composable
fun HeaderCard(
    shizukuConnected: Boolean,
    shizukuPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // مطابقة تامة للأيقونة الخارجية: دائرة داكنة وصاعقة برتقالية
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(AccentOrange, AccentOrangeVariant))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CleaningServices,
                    contentDescription = null,
                    tint = Color.Black
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Debloat HyperOS",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = "Shizuku-powered · no root required",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                )
            }

            StatusBadge(
                connected = shizukuConnected,
                permissionGranted = shizukuPermissionGranted,
                onClick = onRequestPermission
            )
        }
    }
}

@Composable
private fun StatusBadge(
    connected: Boolean,
    permissionGranted: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val disconnectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    val (label, color) = when {
        permissionGranted -> context.getString(R.string.status_connected) to SuccessGreen
        connected -> "Grant access" to AccentOrange
        else -> context.getString(R.string.status_disconnected) to disconnectedColor
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(enabled = !permissionGranted, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(color = color)
            )
        }
    }
}