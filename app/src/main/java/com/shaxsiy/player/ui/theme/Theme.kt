package com.shaxsiy.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// iPhone Glassmorphism & Neon Palette
val DeepSpaceBg = Color(0xFF060714)
val AmoledBlack = Color(0xFF050611)
val SurfaceDark = Color(0xFF101224)
val SurfaceElevated = Color(0xFF191C34)

val NeonCyan = Color(0xFF00F0FF)
val NeonMagenta = Color(0xFFFF2E93)
val NeonViolet = Color(0xFF7B2CBF)
val AccentGreen = Color(0xFF00F0FF) // Asosiy aksent Cyan ga moslandi
val AccentSpotify = Color(0xFF1DB954)
val AccentTeal = Color(0xFF00D2A0)
val YandexGold = Color(0xFFFFCC00)

val GlassFillLight = Color(0x1FFFFFFF)
val GlassFillMedium = Color(0x14FFFFFF)
val GlassFillDark = Color(0x990D1021)
val GlassBorderBright = Color(0x52FFFFFF)
val GlassBorderSoft = Color(0x14FFFFFF)

val TextPrimary = Color(0xFFF7F8FC)
val TextSecondary = Color(0xFFA7ACC4)
val DangerRed = Color(0xFFFF4D6D)

private val GlassColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DeepSpaceBg,
    secondary = NeonMagenta,
    onSecondary = TextPrimary,
    tertiary = NeonViolet,
    background = DeepSpaceBg,
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
        colorScheme = GlassColorScheme,
        content = content
    )
}