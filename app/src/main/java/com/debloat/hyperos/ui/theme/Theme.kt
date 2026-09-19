package com.debloat.hyperos.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ---- الألوان المشتركة ----
val AccentOrange = Color(0xFFFF7A00)
val AccentOrangeVariant = Color(0xFFFFA24D)
val RemovedRed = Color(0xFFFF5252)
val RemovedRedTint = Color(0xFF3A1414)
val SuccessGreen = Color(0xFF4CD97B)

// ---- ألوان الوضع الداكن العادي ----
val BackgroundDark = Color(0xFF121212)
val SurfaceCardDark = Color(0xFF1E1E24)
val TextPrimaryDark = Color(0xFFF5F5F5)
val TextSecondaryDark = Color(0xFF9C9CA3)
val TextDimmedDark = Color(0xFF6B6B72)
val DividerDark = Color(0xFF2A2A31)

// ---- ألوان الوضع الفاتح ----
val BackgroundLight = Color(0xFFF5F5F7)
val SurfaceCardLight = Color(0xFFFFFFFF)
val TextPrimaryLight = Color(0xFF19191D)
val TextSecondaryLight = Color(0xFF686873)
val TextDimmedLight = Color(0xFF9898A0)
val DividerLight = Color(0xFFE4E4E8)

// ---- ألوان وضع AMOLED (سواد تام 100%) ----
val BackgroundAmoled = Color(0xFF000000)
val SurfaceCardAmoled = Color(0xFF0A0A0C)
val DividerAmoled = Color(0xFF1F1F24)

val SurfaceCard = SurfaceCardDark
val TextPrimary = TextPrimaryDark
val TextSecondary = TextSecondaryDark
val TextDimmed = TextDimmedDark
val DividerColor = DividerDark

private val DarkColorScheme = darkColorScheme(
    primary = AccentOrange,
    onPrimary = Color.Black,
    secondary = AccentOrangeVariant,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceCardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceCardDark,
    onSurfaceVariant = TextSecondaryDark,
    error = RemovedRed,
    onError = Color.Black,
    outline = DividerDark
)

private val AmoledColorScheme = darkColorScheme(
    primary = AccentOrange,
    onPrimary = Color.Black,
    secondary = AccentOrangeVariant,
    background = BackgroundAmoled,
    onBackground = TextPrimaryDark,
    surface = SurfaceCardAmoled,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceCardAmoled,
    onSurfaceVariant = TextSecondaryDark,
    error = RemovedRed,
    onError = Color.Black,
    outline = DividerAmoled
)

private val LightColorScheme = lightColorScheme(
    primary = AccentOrange,
    onPrimary = Color.White,
    secondary = AccentOrangeVariant,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceCardLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceCardLight,
    onSurfaceVariant = TextSecondaryLight,
    error = RemovedRed,
    onError = Color.White,
    outline = DividerLight
)

enum class ThemeOption {
    SYSTEM, LIGHT, DARK, AMOLED
}

@Composable
fun DebloatHyperOSTheme(
    themeOption: ThemeOption = ThemeOption.SYSTEM,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeOption) {
        ThemeOption.SYSTEM -> isSystemDark
        ThemeOption.LIGHT -> false
        ThemeOption.DARK, ThemeOption.AMOLED -> true
    }

    val colorScheme = when (themeOption) {
        ThemeOption.LIGHT -> LightColorScheme
        ThemeOption.DARK -> DarkColorScheme
        ThemeOption.AMOLED -> AmoledColorScheme
        ThemeOption.SYSTEM -> if (isSystemDark) DarkColorScheme else LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DebloatTypography,
        content = content
    )
}