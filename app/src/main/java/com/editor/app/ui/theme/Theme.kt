package com.editor.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Neon Accent Colors ───────────────────────────────────────────────────────

val NeonCyan      = Color(0xFF00F5FF)
val NeonPurple    = Color(0xFFBF00FF)
val NeonPink      = Color(0xFFFF006E)
val WaveformGreen = Color(0xFF00FF88)
val AccentYellow  = Color(0xFFFFD600)
val AccentOrange  = Color(0xFFFF6B00)

// ─── Background / Surface Colors ─────────────────────────────────────────────

val DeepBlack   = Color(0xFF0A0A0F)
val SurfaceDark = Color(0xFF12121A)
val CardDark    = Color(0xFF1A1A26)
val CardDarker  = Color(0xFF0F0F18)
val TimelineBar = Color(0xFF16161F)

// ─── Text Colors ─────────────────────────────────────────────────────────────

val TextPrimary   = Color(0xFFE8E8F0)
val TextSecondary = Color(0xFF7A7A9A)

// ─── Color Scheme ─────────────────────────────────────────────────────────────

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

// ─── Theme Entry Point ────────────────────────────────────────────────────────

@Composable
fun VideoAudioEditorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = EditorTypography,   // comes from Typography.kt
        content     = content
    )
}