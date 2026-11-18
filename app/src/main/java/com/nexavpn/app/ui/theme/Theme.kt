package com.nexavpn.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Brand palette — deep violet, Proton-inspired but distinct.
val NexaViolet = Color(0xFF6D4AFF)
val NexaVioletSoft = Color(0xFF8A6FFF)
val NexaTeal = Color(0xFF2DD4A7)
val NexaAmber = Color(0xFFF5B759)
val NexaRed = Color(0xFFF86F6F)

// Semantic aliases for status colors usable outside the scheme.
val StatusOk = NexaTeal
val StatusWarn = NexaAmber
val StatusBad = NexaRed

private val DarkColors = darkColorScheme(
    primary = NexaViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF241A45),
    onPrimaryContainer = Color(0xFFE6DEFF),
    secondary = NexaVioletSoft,
    secondaryContainer = Color(0xFF1D2333),
    onSecondaryContainer = Color(0xFFD7DCF0),
    tertiary = NexaTeal,
    onTertiary = Color(0xFF06251C),
    tertiaryContainer = Color(0xFF123A30),
    onTertiaryContainer = Color(0xFFB8F4E3),
    error = NexaRed,
    errorContainer = Color(0xFF3A1A22),
    onErrorContainer = Color(0xFFFFD9DC),
    background = Color(0xFF0C0F16),
    onBackground = Color(0xFFECEEF6),
    surface = Color(0xFF121621),
    onSurface = Color(0xFFECEEF6),
    surfaceVariant = Color(0xFF1B2130),
    onSurfaceVariant = Color(0xFFA7ADC2),
    outline = Color(0xFF39415A),
    surfaceContainer = Color(0xFF171C29),
    surfaceContainerHigh = Color(0xFF1D2333),
    surfaceContainerHighest = Color(0xFF242B3E),
)

private val LightColors = lightColorScheme(
    primary = NexaViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E2FF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = NexaVioletSoft,
    tertiary = Color(0xFF0E9F77),
    error = Color(0xFFDC3644),
    background = Color(0xFFF5F5FA),
    onBackground = Color(0xFF171923),
    surface = Color.White,
    onSurface = Color(0xFF171923),
    surfaceVariant = Color(0xFFE7E8F2),
    onSurfaceVariant = Color(0xFF5B6180),
    outline = Color(0xFFC4C7D6),
)

private val NexaTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.2.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.4.sp,
    ),
)

@Composable
fun NexaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = NexaTypography,
        content = content,
    )
}
