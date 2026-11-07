package com.bookwise.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    background = Color(0xFFF5F0E8),
    surface = Color(0xFFF5F0E8),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    primary = Color(0xFF6B4C3B),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF8B7355),
    onSecondary = Color(0xFFFFFFFF)
)

private val DarkColorScheme = darkColorScheme(
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1D8),
    onSurface = Color(0xFFE6E1D8),
    primary = Color(0xFFD4A574),
    onPrimary = Color(0xFF1C1B1F),
    secondary = Color(0xFFB8A080),
    onSecondary = Color(0xFF1C1B1F)
)

@Composable
fun BookwiseTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}
