package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
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
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val width = configuration.screenWidthDp
    
    // Scale density for small screens to ensure content fits
    val currentDensity = LocalDensity.current
    val scaleFactor = if (width < 360) {
        width.toFloat() / 360f
    } else {
        1f
    }
    
    val newDensity = Density(
        density = currentDensity.density * scaleFactor,
        fontScale = currentDensity.fontScale * scaleFactor
    )
    
    val dimensions = when {
        width < 360 -> ResponsiveDimensions(
            outerPadding = 12.dp,
            cardPadding = 10.dp,
            spacingSmall = 6.dp,
            spacingMedium = 12.dp,
            spacingLarge = 18.dp,
            cardHeightLarge = 144.dp,
            cardHeightMedium = 108.dp,
            cardHeightSmall = 72.dp,
            titleLargeSize = 19.sp,
            displayLargeSize = 28.sp,
            displayMediumSize = 25.sp,
            displaySmallSize = 21.sp,
            iconSizeMedium = 20.dp,
            iconSizeLarge = 28.dp
        )
        width > 411 -> ResponsiveDimensions(
            outerPadding = 24.dp,
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
        else -> ResponsiveDimensions()
    }
    
    val dynamicTypography = androidx.compose.material3.Typography(
        displayLarge = Typography.displayLarge.copy(fontSize = dimensions.displayLargeSize, lineHeight = (dimensions.displayLargeSize.value * 1.25f).sp),
        displayMedium = Typography.displayMedium.copy(fontSize = dimensions.displayMediumSize, lineHeight = (dimensions.displayMediumSize.value * 1.25f).sp),
        displaySmall = Typography.displaySmall.copy(fontSize = dimensions.displaySmallSize, lineHeight = (dimensions.displaySmallSize.value * 1.25f).sp),
        headlineLarge = Typography.headlineLarge.copy(fontSize = dimensions.displayLargeSize, lineHeight = (dimensions.displayLargeSize.value * 1.25f).sp),
        headlineMedium = Typography.headlineMedium.copy(fontSize = dimensions.displayMediumSize, lineHeight = (dimensions.displayMediumSize.value * 1.25f).sp),
        headlineSmall = Typography.headlineSmall.copy(fontSize = dimensions.displaySmallSize, lineHeight = (dimensions.displaySmallSize.value * 1.25f).sp),
        titleLarge = Typography.titleLarge.copy(fontSize = dimensions.titleLargeSize, lineHeight = (dimensions.titleLargeSize.value * 1.25f).sp),
        titleMedium = Typography.titleMedium.copy(fontSize = if (width < 360) 16.sp else 18.sp, lineHeight = if (width < 360) 22.sp else 24.sp),
        titleSmall = Typography.titleSmall.copy(fontSize = if (width < 360) 14.sp else 16.sp, lineHeight = if (width < 360) 20.sp else 22.sp),
        bodyLarge = Typography.bodyLarge.copy(fontSize = if (width < 360) 14.sp else 16.sp, lineHeight = if (width < 360) 20.sp else 24.sp),
        bodyMedium = Typography.bodyMedium.copy(fontSize = if (width < 360) 13.sp else 14.sp, lineHeight = if (width < 360) 18.sp else 20.sp),
        bodySmall = Typography.bodySmall.copy(fontSize = if (width < 360) 11.sp else 12.sp, lineHeight = if (width < 360) 14.sp else 16.sp),
        labelLarge = Typography.labelLarge.copy(fontSize = if (width < 360) 14.sp else 16.sp, lineHeight = if (width < 360) 18.sp else 20.sp),
        labelMedium = Typography.labelMedium.copy(fontSize = if (width < 360) 11.sp else 12.sp, lineHeight = if (width < 360) 14.sp else 16.sp),
        labelSmall = Typography.labelSmall.copy(fontSize = if (width < 360) 9.sp else 10.sp, lineHeight = if (width < 360) 12.sp else 14.sp)
    )

    CompositionLocalProvider(
        LocalResponsiveDimensions provides dimensions,
        LocalDensity provides newDensity
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = dynamicTypography
        ) {
            content()
        }
    }
}
