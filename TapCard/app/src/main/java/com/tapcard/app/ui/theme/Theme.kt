package com.tapcard.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.tapcard.app.data.ThemeMode
import com.tapcard.app.data.ThemeController

private val DarkColors = darkColorScheme(
    primary = DPrimary,
    onPrimary = Color(0xFF0D0418),
    primaryContainer = Color(0xFF2A1D4D),
    onPrimaryContainer = Color(0xFFE4D6FF),
    secondary = DSecondary,
    onSecondary = Color(0xFF12160A),
    secondaryContainer = Color(0xFF2E3512),
    onSecondaryContainer = Color(0xFFE6F7A0),
    tertiary = Lime,
    background = VoidBlack,
    onBackground = TextBright,
    surface = PanelDark,
    onSurface = TextBright,
    surfaceVariant = PanelDarkElevated,
    onSurfaceVariant = TextDim,
    outline = BorderViolet,
    outlineVariant = Color(0xFF1B1728),
    error = ErrorRedDark,
    onError = Color(0xFF2A0F07),
)

private val LightColors = lightColorScheme(
    primary = LPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7DBFF),
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = LSecondary,
    onSecondary = Color(0xFF161B06),
    secondaryContainer = Color(0xFFEDF7C4),
    onSecondaryContainer = Color(0xFF2A3208),
    tertiary = Success,
    background = LBg,
    onBackground = LOnBg,
    surface = LSurface,
    onSurface = LOnBg,
    surfaceVariant = LSurfaceVariant,
    onSurfaceVariant = Color(0xFF5F5B70),
    outline = LOutline,
    outlineVariant = Color(0xFFEAE6F3),
    error = ErrorRed,
    onError = Color.White,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun TapCardTheme(content: @Composable () -> Unit) {
    val mode by ThemeController.mode.collectAsState()
    val darkTheme = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        else -> isSystemInDarkTheme()
    }
    val colors = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    val context = LocalContext.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
