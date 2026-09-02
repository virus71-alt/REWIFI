package com.rewifi.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Immutable
data class RewifiColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val shadow: Color,
    val accent: Color = Yellow,
    val onAccent: Color = Ink,
    val green: Color = Green,
    val red: Color = Red,
    val blue: Color = Blue,
)

val LightRewifiColors = RewifiColors(
    isDark = false,
    background = Paper,
    surface = Snow,
    surfaceVariant = Color(0xFFEBEBE8),
    textPrimary = Ink,
    textSecondary = Muted,
    border = Ink,
    shadow = Ink,
    accent = Yellow,
    onAccent = Ink,
    green = Green,
    red = Red,
    blue = Blue
)

val DarkRewifiColors = RewifiColors(
    isDark = true,
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    border = DarkBorder,
    shadow = DarkShadow,
    accent = Yellow,
    onAccent = Ink,
    green = Green,
    red = Red,
    blue = Blue
)

val LocalRewifiColors = staticCompositionLocalOf { LightRewifiColors }

object RewifiTheme {
    val colors: RewifiColors
        @Composable
        get() = LocalRewifiColors.current
}

private val LightColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Snow,
    secondary = Yellow,
    onSecondary = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Snow,
    onSurface = Ink,
    error = Red,
)

private val DarkColorScheme = darkColorScheme(
    primary = Yellow,
    onPrimary = Ink,
    secondary = Yellow,
    onSecondary = Ink,
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    error = Red,
)

@Composable
fun RewifiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val rewifiColors = if (darkTheme) DarkRewifiColors else LightRewifiColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalRewifiColors provides rewifiColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = RewifiType,
            content = content
        )
    }
}

