package com.kabarinpacar.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkBackground = Color(0xFF1A0A0E)
private val DarkSurface = Color(0xFF2D0F17)
private val DarkSurfaceVariant = Color(0xFF3D1520)

private val LightColorScheme = lightColorScheme(
    primary = Rose500,
    onPrimary = White,
    primaryContainer = Rose100,
    onPrimaryContainer = Rose900,
    secondary = Pink400,
    onSecondary = White,
    secondaryContainer = Pink100,
    onSecondaryContainer = Rose800,
    tertiary = Peach200,
    background = Rose50,
    onBackground = DarkGray,
    surface = White,
    onSurface = DarkGray,
    surfaceVariant = Pink100,
    onSurfaceVariant = MediumGray,
    outline = LightGray
)

private val DarkColorScheme = darkColorScheme(
    primary = Rose300,
    onPrimary = Rose900,
    primaryContainer = Rose700,
    onPrimaryContainer = Rose100,
    secondary = Pink300,
    onSecondary = Rose900,
    secondaryContainer = Rose800,
    onSecondaryContainer = Pink100,
    tertiary = Peach100,
    background = DarkBackground,
    onBackground = Rose50,
    surface = DarkSurface,
    onSurface = Rose50,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Rose200,
    outline = Rose700
)

@Composable
fun KabarinPacarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
