package com.nexora.app.ui.theme

import androidx.compose.ui.graphics.Color

val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val PrimaryTeal = Color(0xFF00BFA5)
val SecondaryTeal = Color(0xFF00897B)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB0B0B0)
val ErrorRed = Color(0xFFCF6679)

val DarkColorScheme = androidx.compose.material3.darkColorScheme(
    primary = PrimaryTeal,
    secondary = SecondaryTeal,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = ErrorRed
)
