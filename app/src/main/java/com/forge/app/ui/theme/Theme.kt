package com.forge.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ForgeScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Black,
    secondary = AccentDim,
    onSecondary = TextPrimary,
    background = Black,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextSecondary,
    outline = Border,
    error = Danger,
    onError = Color.White
)

@Composable
fun ForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ForgeScheme,
        typography = Typography,
        content = content
    )
}
