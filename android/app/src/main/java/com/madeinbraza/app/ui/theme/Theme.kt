package com.madeinbraza.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BrazaColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF6A1B9A), // Purple 800 - selected items
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF888888),
    onSecondary = Color.Black,
    tertiary = Color(0xFFFBBF24), // Amber 400 - dourado para "Livre"
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF3D2E00), // Dark amber background
    onTertiaryContainer = Color(0xFFFBBF24), // Amber 400
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF121212),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2C), // Gray for regular slots
    onSurfaceVariant = Color(0xFFCACACA),
    error = Color(0xFFCF6679),
    onError = Color.Black
)

@Composable
fun BrazaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BrazaColorScheme,
        content = content
    )
}
