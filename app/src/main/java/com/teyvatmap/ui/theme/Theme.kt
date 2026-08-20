package com.teyvatmap.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Teal400,
    primaryContainer = Teal700,
    secondary = Teal300,
    secondaryContainer = Teal600,
    tertiary = Green,
    tertiaryContainer = Color(0xFF2E7D32),
    error = Red,
    background = DarkBg,
    surface = PanelBg,
    surfaceVariant = PanelBg2,
    onPrimary = Color.White,
    onPrimaryContainer = Color.White,
    onSecondary = Color.Black,
    onSecondaryContainer = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = MutedText,
    outline = Border,
    outlineVariant = Border.copy(alpha = 0.5f),
    inverseSurface = Color.White,
    inverseOnSurface = Color.Black,
    inversePrimary = Teal700,
    scrim = Color.Black.copy(alpha = 0.8f),
    surfaceTint = Teal400,
)

@Composable
fun TeyvatMapTheme(
    darkTheme: Boolean = true, // Force dark theme for this app
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}