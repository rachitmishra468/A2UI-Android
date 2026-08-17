package com.example.a2ui_sample.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PremiumColors.Accent,
    secondary = PremiumColors.Gray300,
    tertiary = PremiumColors.Info,
    background = PremiumColors.BackgroundDark,
    surface = PremiumColors.SurfaceDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    error = PremiumColors.Error
)

private val LightColorScheme = lightColorScheme(
    primary = PremiumColors.Primary,
    secondary = PremiumColors.Gray600,
    tertiary = PremiumColors.Accent,
    background = PremiumColors.BackgroundLight,
    surface = PremiumColors.SurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = PremiumColors.Gray900,
    onSurface = PremiumColors.Gray900,
    error = PremiumColors.Error
)

@Composable
fun A2UI_SampleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(
            headlineLarge = PremiumTypography.HeadlineLarge,
            headlineMedium = PremiumTypography.HeadlineMedium,
            bodyLarge = PremiumTypography.BodyLarge,
            labelMedium = PremiumTypography.LabelMedium
        ),
        content = content
    )
}
