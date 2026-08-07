package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant
)

data class ResponsiveDimensions(
    val outerPadding: Dp = 16.dp,
    val cardPadding: Dp = 12.dp,
    val spacingSmall: Dp = 8.dp,
    val spacingMedium: Dp = 16.dp,
    val spacingLarge: Dp = 24.dp,
    val cardHeightLarge: Dp = 160.dp,
    val cardHeightMedium: Dp = 120.dp,
    val cardHeightSmall: Dp = 80.dp,
    val titleLargeSize: TextUnit = 22.sp,
    val displayLargeSize: TextUnit = 32.sp,
    val displayMediumSize: TextUnit = 28.sp,
    val displaySmallSize: TextUnit = 24.sp,
    val iconSizeMedium: Dp = 24.dp,
    val iconSizeLarge: Dp = 32.dp
)

val LocalResponsiveDimensions = staticCompositionLocalOf { ResponsiveDimensions() }

@Composable
fun ProvideResponsiveDimensions(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val width = configuration.screenWidthDp

    val dimensions = when {
        width < 360 -> ResponsiveDimensions(
            outerPadding = 12.dp,       // ~25% reduction
            cardPadding = 10.dp,
            spacingSmall = 6.dp,
            spacingMedium = 12.dp,
            spacingLarge = 18.dp,
            cardHeightLarge = 144.dp,   // 10% reduction
            cardHeightMedium = 108.dp,
            cardHeightSmall = 72.dp,
            titleLargeSize = 19.sp,     // -3sp
            displayLargeSize = 28.sp,   // -4sp
            displayMediumSize = 25.sp,  // -3sp
            displaySmallSize = 21.sp,   // -3sp
            iconSizeMedium = 20.dp,
            iconSizeLarge = 28.dp
        )
        width > 411 -> ResponsiveDimensions(
            outerPadding = 24.dp,       // Increase spacing for larger screens
            cardPadding = 16.dp,
            spacingSmall = 10.dp,
            spacingMedium = 20.dp,
            spacingLarge = 32.dp,
            cardHeightLarge = 180.dp,
            cardHeightMedium = 140.dp,
            cardHeightSmall = 90.dp,
            titleLargeSize = 24.sp,
            displayLargeSize = 34.sp,
            displayMediumSize = 30.sp,
            displaySmallSize = 26.sp,
            iconSizeMedium = 26.dp,
            iconSizeLarge = 36.dp
        )
        else -> ResponsiveDimensions() // Medium default (360-411dp)
    }

    CompositionLocalProvider(LocalResponsiveDimensions provides dimensions) {
        content()
    }
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // We enforce our branded DarkColorScheme to showcase Friction's premium dark design aesthetic
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography
    ) {
        ProvideResponsiveDimensions {
            content()
        }
    }
}

