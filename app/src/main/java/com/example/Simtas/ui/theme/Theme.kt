package com.example.Simtas.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PurplePrimary,
    secondary = PurpleSecondary,
    tertiary = Purple80,
    background = Color(0xFF13111C),
    surface = Color(0xFF1E1A30),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFF3F1FA),
    onSurface = Color(0xFFF3F1FA)
)

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    secondary = PurpleSecondary,
    tertiary = Pink80,
    background = Color(0xFFF1EEFF),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1D1731),
    onSurface = Color(0xFF1D1731)
)

@Composable
fun AIBasedClassNotesAppTheme(
    themeMode: AppThemeMode = ThemeManager.currentThemeMode,
    darkTheme: Boolean = ThemeManager.isDark(),
    fontStyle: AppFontStyle = FontManager.currentFontStyle,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val typography = remember(fontStyle) { getTypography(fontStyle) }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography
    ) {
        val baseTextStyle = LocalTextStyle.current.copy(
            fontFamily = fontStyle.fontFamily
        )
        CompositionLocalProvider(
            LocalTextStyle provides baseTextStyle,
            content = content
        )
    }
}
