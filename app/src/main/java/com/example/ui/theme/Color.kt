package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Premium Dark Palette Design Tokens
val DarkBackground = Color(0xFF111315)
val DarkSurface = Color(0xFF181A1F)
val DarkElevated = Color(0xFF20232A)
val DarkCardBg = Color(0xFF262A31)

val FrictionPrimary = Color(0xFF34C759) // Apple Accent Green
val FrictionSecondary = Color(0xFF16A34A)
val FrictionAccent = Color(0xFFF4C430)  // Gold Yellow
val FrictionError = Color(0xFFFF5A5F)   // Rose Red

val TextPrimary = Color(0xFFF5F7FA)
val TextSecondary = Color(0xFFA1A7B3)
val TextMuted = Color(0xFF6E7480)
val DarkDivider = Color(0x10FFFFFF)

// Legacy compatibility aliases mapped to new premium dark tokens
val FrictionBackground = DarkBackground
val FrictionCards = DarkCardBg
val FrictionText = TextPrimary
val FrictionSecondaryText = TextSecondary


// Material 3 mappings
val md_theme_dark_primary = FrictionPrimary
val md_theme_dark_onPrimary = Color(0xFF111315)
val md_theme_dark_primaryContainer = Color(0xFF181A1F)
val md_theme_dark_onPrimaryContainer = TextPrimary
val md_theme_dark_secondary = FrictionSecondary
val md_theme_dark_onSecondary = Color(0xFF111315)
val md_theme_dark_tertiary = FrictionAccent
val md_theme_dark_onTertiary = Color(0xFF111315)
val md_theme_dark_background = DarkBackground
val md_theme_dark_onBackground = TextPrimary
val md_theme_dark_surface = DarkSurface
val md_theme_dark_onSurface = TextPrimary
val md_theme_dark_error = FrictionError
val md_theme_dark_onError = Color(0xFFFFFFFF)
val md_theme_dark_surfaceVariant = DarkCardBg
val md_theme_dark_onSurfaceVariant = TextSecondary

