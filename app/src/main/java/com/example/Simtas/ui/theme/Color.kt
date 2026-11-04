package com.example.Simtas.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Purple palette matching the brand design
val PurplePrimary = Color(0xFF674AD9)
val PurpleSecondary = Color(0xFF6E5BC8)

// Dynamic theme colors that automatically switch between Light and Dark modes
val SplashBackground: Color
    get() = if (ThemeManager.isDark()) Color(0xFF13111C) else Color(0xFFF1EEFF)

val TextDarkPrimary: Color
    get() = if (ThemeManager.isDark()) Color(0xFFF3F1FA) else Color(0xFF1D1731)

val TextSubtitle: Color
    get() = if (ThemeManager.isDark()) Color(0xFFB5A7F2) else Color(0xFF6E5BC8)

val TextMuted: Color
    get() = if (ThemeManager.isDark()) Color(0xFFA29BB8) else Color(0xFF918AA8)

// Decorative element colors
val OuterRingBorder: Color
    get() = if (ThemeManager.isDark()) Color(0xFF332B52) else Color(0xFFE2DBFA)

val InnerCircleBg: Color
    get() = if (ThemeManager.isDark()) Color(0xFF241E38) else Color(0xFFECE6FE)

val InactiveDot: Color
    get() = if (ThemeManager.isDark()) Color(0xFF423963) else Color(0xFFD6CEF6)

// Card & Container surface background
val CardBg: Color
    get() = if (ThemeManager.isDark()) Color(0xFF1E1A30) else Color.White

// Default Material theme fallbacks
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
