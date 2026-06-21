package com.namaaztracker.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4CAF50),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFA5D6A7),
    secondary = Color(0xFFF9A825),
    onSecondary = Color(0xFF000000),
    background = Color(0xFF0D1B12),
    onBackground = Color(0xFFE8F5E9),
    surface = Color(0xFF1A2E1F),
    onSurface = Color(0xFFE8F5E9),
    surfaceVariant = Color(0xFF243B28),
    onSurfaceVariant = Color(0xFFC8E6C9),
    outline = Color(0xFF388E3C),
)

@Composable
fun NamaazTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content,
    )
}
