package com.example.Simtas.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.Simtas.R

/**
 * Reusable Compose background component that applies the shared educational
 * lavender and purple study-themed background (R.drawable.smartnotes_app_background)
 * proportionally across the app using [ContentScale.Crop].
 *
 * Provides a gentle ambient scrim to ensure all foreground typography, buttons,
 * icons, input cards, and interactive elements retain optimal contrast and readability
 * in both Light and Dark theme modes.
 */
@Composable
fun SmartNotesAppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Shared educational background image, kept proportional without distortion
        Image(
            painter = painterResource(id = R.drawable.smartnotes_app_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Subtle theme-adaptive contrast overlay:
        // In Light Mode: Gentle translucent white/lavender wash ensuring high contrast for dark text.
        // In Dark Mode: Deep violet-tinted translucent overlay ensuring comfortable contrast for light text.
        val isDark = ThemeManager.isDark()
        val scrimBrush = if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF140F24).copy(alpha = 0.70f),
                    Color(0xFF140F24).copy(alpha = 0.60f),
                    Color(0xFF140F24).copy(alpha = 0.75f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.15f),
                    Color.White.copy(alpha = 0.35f)
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimBrush)
        )

        // Screen content rendered directly above the background and scrim
        content()
    }
}
