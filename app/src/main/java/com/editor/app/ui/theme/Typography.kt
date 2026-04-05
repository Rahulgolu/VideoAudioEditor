package com.editor.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val EditorTypography = Typography(
    displayLarge = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Black,
        fontSize     = 36.sp,
        letterSpacing = (-1).sp
    ),
    headlineLarge = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Bold,
        fontSize     = 24.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 18.sp
    ),
    titleLarge = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 16.sp,
        letterSpacing = 0.5.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Normal,
        fontSize      = 11.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Medium,
        fontSize      = 10.sp,
        letterSpacing = 1.sp
    )
)