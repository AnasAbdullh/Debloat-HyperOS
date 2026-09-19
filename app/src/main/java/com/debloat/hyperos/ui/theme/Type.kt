package com.debloat.hyperos.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val DebloatTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextPrimary),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = TextPrimary),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, color = TextPrimary),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, color = TextSecondary),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, color = TextSecondary)
)
