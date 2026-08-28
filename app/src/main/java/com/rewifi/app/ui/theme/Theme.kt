package com.rewifi.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val RewifiColors = lightColorScheme(
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

@Composable
fun RewifiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Brutalism stays high-contrast in any system mode — single scheme on purpose.
    MaterialTheme(
        colorScheme = RewifiColors,
        typography = RewifiType,
        content = content
    )
}
