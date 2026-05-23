package com.editor.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Neon Accent Colors ───────────────────────────────────────────────────────

val NeonCyan      = Color(0xFF00F5FF)
val NeonPurple    = Color(0xFFBF00FF)
val NeonPink      = Color(0xFFFF006E)
val WaveformGreen = Color(0xFF00FF88)
val AccentYellow  = Color(0xFFFFD600)
val AccentOrange  = Color(0xFFFF6B00)

// ─── Dark Theme Colors ───────────────────────────────────────────────────────

val DeepBlack   = Color(0xFF0A0A0F)
val SurfaceDark = Color(0xFF12121A)
val CardDark    = Color(0xFF1A1A26)
val CardDarker  = Color(0xFF0F0F18)
val TimelineBar = Color(0xFF16161F)

// ─── Light Theme Colors ──────────────────────────────────────────────────────

val LightBackground = Color(0xFFF5F5F8)
val LightSurface    = Color(0xFFFFFFFF)
val LightCard       = Color(0xFFFAFAFA)
val LightCardDarker = Color(0xFFF0F0F5)
val LightTimeline   = Color(0xFFE8E8EC)

val LightTextPrimary   = Color(0xFF1A1A2E)
val LightTextSecondary = Color(0xFF6A6A8A)

// ─── Text Colors (Dark Theme) ───────────────────────────────────────────────

val TextPrimary   = Color(0xFFE8E8F0)
val TextSecondary = Color(0xFF7A7A9A)

// ─── Color Schemes ─────────────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary        = NeonCyan,
    secondary      = NeonPurple,
    tertiary       = NeonPink,
    background     = DeepBlack,
    surface        = SurfaceDark,
    onPrimary      = DeepBlack,
    onSecondary    = Color.White,
    onTertiary     = Color.White,
    onBackground   = TextPrimary,
    onSurface      = TextPrimary,
    surfaceVariant = CardDark,
    outline        = Color(0xFF2A2A3A)
)

private val LightColorScheme = lightColorScheme(
    primary        = NeonCyan,
    secondary      = NeonPurple,
    tertiary       = NeonPink,
    background     = LightBackground,
    surface        = LightSurface,
    onPrimary      = DeepBlack,
    onSecondary    = Color.White,
    onTertiary     = Color.White,
    onBackground   = LightTextPrimary,
    onSurface      = LightTextPrimary,
    surfaceVariant = LightCard,
    outline        = Color(0xFFD0D0E0)
)

// ─── Theme Mode ────────────────────────────────────────────────────────────────

enum class ThemeMode { DARK, LIGHT, SYSTEM }

// ─── Theme Entry Point ────────────────────────────────────────────────────────

@Composable
fun VideoAudioEditorTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.DARK -> DarkColorScheme
        ThemeMode.LIGHT -> LightColorScheme
        ThemeMode.SYSTEM -> {
            val isDark = java.time.LocalTime.now().hour < 6 || java.time.LocalTime.now().hour >= 18
            if (isDark) DarkColorScheme else LightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = EditorTypography,
        content     = content
    )
}

// ─── Get Theme Colors for Use in Composables ─────────────────────────────────

object ThemeColors {
    val background: Color @Composable get() = if (isDarkTheme()) DeepBlack else LightBackground
    val surface: Color @Composable get() = if (isDarkTheme()) SurfaceDark else LightSurface
    val card: Color @Composable get() = if (isDarkTheme()) CardDark else LightCard
    val cardDarker: Color @Composable get() = if (isDarkTheme()) CardDarker else LightCardDarker
    val timelineBar: Color @Composable get() = if (isDarkTheme()) TimelineBar else LightTimeline
    val textPrimary: Color @Composable get() = if (isDarkTheme()) TextPrimary else LightTextPrimary
    val textSecondary: Color @Composable get() = if (isDarkTheme()) TextSecondary else LightTextSecondary
}

@Composable
private fun isDarkTheme(): Boolean {
    return MaterialTheme.colorScheme.background.luminance() < 0.5f
}

private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}