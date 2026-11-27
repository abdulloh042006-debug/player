package com.shaxsiy.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AmoledBlack = Color(0xFF000000)
val SurfaceDark = Color(0xFF101014)
val SurfaceElevated = Color(0xFF1A1A22)
val AccentGreen = Color(0xFF1DB954)
val AccentTeal = Color(0xFF00D2A0)
val TextPrimary = Color(0xFFF5F5F7)
val TextSecondary = Color(0xFF9A9AA6)
val DangerRed = Color(0xFFFF5252)

private val AmoledColorScheme = darkColorScheme(
    primary = AccentGreen,
    onPrimary = AmoledBlack,
    secondary = AccentTeal,
    onSecondary = AmoledBlack,
    background = AmoledBlack,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    error = DangerRed
)

@Composable
fun ShaxsiyPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AmoledColorScheme,
        content = content
    )
}