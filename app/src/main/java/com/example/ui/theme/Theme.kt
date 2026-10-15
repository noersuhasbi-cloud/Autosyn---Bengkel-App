package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WorkshopColorScheme = lightColorScheme(
    primary = WorkshopBrandNavy,
    onPrimary = Color.White,
    primaryContainer = WorkshopActivePill,
    onPrimaryContainer = WorkshopBrandNavy,
    secondary = WorkshopNavyDark,
    onSecondary = Color.White,
    background = WorkshopCanvasBg,
    onBackground = WorkshopTextPrimary,
    surface = WorkshopCardBg,
    onSurface = WorkshopTextPrimary,
    surfaceVariant = WorkshopCanvasBg,
    onSurfaceVariant = WorkshopTextSecondary,
    outline = WorkshopBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WorkshopColorScheme,
        typography = Typography,
        content = content
    )
}
