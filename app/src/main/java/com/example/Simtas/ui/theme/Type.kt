package com.example.Simtas.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Creates a centralized [Typography] system configured with the selected [AppFontStyle].
 */
fun getTypography(fontStyle: AppFontStyle): Typography {
    val family = fontStyle.fontFamily
    val isBoldSans = fontStyle == AppFontStyle.BoldSansSerif

    val bodyWeight = if (isBoldSans) FontWeight.Medium else FontWeight.Normal
    val titleWeight = if (isBoldSans) FontWeight.ExtraBold else FontWeight.Bold
    val subWeight = if (isBoldSans) FontWeight.SemiBold else FontWeight.Medium

    return Typography(
        displayLarge = TextStyle(
            fontFamily = family,
            fontWeight = titleWeight,
            fontSize = 57.sp,
            lineHeight = 64.sp
        ),
        displayMedium = TextStyle(
            fontFamily = family,
            fontWeight = titleWeight,
            fontSize = 45.sp,
            lineHeight = 52.sp
        ),
        displaySmall = TextStyle(
            fontFamily = family,
            fontWeight = titleWeight,
            fontSize = 36.sp,
            lineHeight = 44.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = family,
            fontWeight = titleWeight,
            fontSize = 32.sp,
            lineHeight = 40.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = family,
            fontWeight = subWeight,
            fontSize = 28.sp,
            lineHeight = 36.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = family,
            fontWeight = subWeight,
            fontSize = 24.sp,
            lineHeight = 32.sp
        ),
        titleLarge = TextStyle(
            fontFamily = family,
            fontWeight = titleWeight,
            fontSize = 22.sp,
            lineHeight = 28.sp
        ),
        titleMedium = TextStyle(
            fontFamily = family,
            fontWeight = subWeight,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        titleSmall = TextStyle(
            fontFamily = family,
            fontWeight = subWeight,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = family,
            fontWeight = bodyWeight,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = family,
            fontWeight = bodyWeight,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = family,
            fontWeight = bodyWeight,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),
        labelLarge = TextStyle(
            fontFamily = family,
            fontWeight = subWeight,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = family,
            fontWeight = subWeight,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = family,
            fontWeight = subWeight,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
}

// Default Typography for static usages and preview compatibility
val Typography = getTypography(AppFontStyle.BoldSansSerif)
