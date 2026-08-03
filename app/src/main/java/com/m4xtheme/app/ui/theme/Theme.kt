package com.m4xtheme.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Colors = darkColorScheme(
    primary = Color(0xFF7C5CFF),
    secondary = Color(0xFF22D3EE),
    background = Color(0xFF080B14),
    surface = Color(0xFF111827),
    surfaceVariant = Color(0xFF1B2433)
)

@Composable
fun M4XTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
