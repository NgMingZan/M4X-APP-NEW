package com.m4xtheme.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val M4XColors = darkColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF30205D),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = Color(0xFF22D3EE),
    onSecondary = Color(0xFF042F35),
    secondaryContainer = Color(0xFF103E48),
    tertiary = Color(0xFFFFB84D),
    background = Color(0xFF070B15),
    onBackground = Color(0xFFF3F0FA),
    surface = Color(0xFF101727),
    onSurface = Color(0xFFF3F0FA),
    surfaceVariant = Color(0xFF1B2538),
    onSurfaceVariant = Color(0xFFC7C9D4),
    outline = Color(0xFF647089),
    error = Color(0xFFFFB4AB)
)

@Composable
fun M4XTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = M4XColors, content = content)
}
