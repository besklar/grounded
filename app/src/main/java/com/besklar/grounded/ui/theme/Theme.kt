package com.besklar.grounded.ui.theme

import android.graphics.Typeface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GroundedBlue = Color(0xFF0057D9)
private val GroundedBlueDark = Color(0xFF9BBEFF)
private val GroundedInk = Color(0xFF151922)
private val GroundedCanvas = Color(0xFFF5F7FA)
private val GroundedSurface = Color(0xFFFFFFFF)
private val GroundedRed = Color(0xFFB4232C)
private val GroundedTeal = Color(0xFF00796B)

private val LightColors =
    lightColorScheme(
        primary = GroundedBlue,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDCE8FF),
        onPrimaryContainer = Color(0xFF001A41),
        secondary = Color(0xFF435D82),
        secondaryContainer = Color(0xFFD9E6FF),
        tertiary = GroundedTeal,
        tertiaryContainer = Color(0xFFB8F1E8),
        background = GroundedCanvas,
        onBackground = GroundedInk,
        surface = GroundedSurface,
        onSurface = GroundedInk,
        surfaceVariant = Color(0xFFEEF1F5),
        onSurfaceVariant = Color(0xFF565E6C),
        outline = Color(0xFF747C89),
        outlineVariant = Color(0xFFD5DAE2),
        error = GroundedRed,
        errorContainer = Color(0xFFFFDAD9),
    )

private val DarkColors =
    darkColorScheme(
        primary = GroundedBlueDark,
        onPrimary = Color(0xFF002E6B),
        primaryContainer = Color(0xFF004398),
        onPrimaryContainer = Color(0xFFDCE8FF),
        secondary = Color(0xFFB2C8EC),
        secondaryContainer = Color(0xFF2C4668),
        tertiary = Color(0xFF75D7C7),
        tertiaryContainer = Color(0xFF005046),
        background = Color(0xFF101318),
        onBackground = Color(0xFFE3E6ED),
        surface = Color(0xFF181C22),
        onSurface = Color(0xFFE3E6ED),
        surfaceVariant = Color(0xFF232830),
        onSurfaceVariant = Color(0xFFC3C7D0),
        outline = Color(0xFF8D949F),
        outlineVariant = Color(0xFF3D434C),
        error = Color(0xFFFFB3B3),
        errorContainer = Color(0xFF7F1D25),
    )

private val BaseTypography = Typography()
private val TitleFont = FontFamily(Typeface.create("sans-serif-condensed", Typeface.BOLD))
private val GroundedTypography =
    Typography(
        displayLarge = BaseTypography.displayLarge.copy(fontFamily = TitleFont, fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
        displayMedium = BaseTypography.displayMedium.copy(fontFamily = TitleFont, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
        displaySmall = BaseTypography.displaySmall.copy(fontFamily = TitleFont, fontWeight = FontWeight.ExtraBold),
        headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = TitleFont, fontWeight = FontWeight.ExtraBold),
        headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = TitleFont, fontWeight = FontWeight.ExtraBold),
        headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = TitleFont, fontWeight = FontWeight.Bold),
        titleLarge = BaseTypography.titleLarge.copy(fontFamily = TitleFont, fontWeight = FontWeight.ExtraBold),
        titleMedium = BaseTypography.titleMedium.copy(fontFamily = TitleFont, fontWeight = FontWeight.Bold),
        titleSmall = BaseTypography.titleSmall.copy(fontFamily = TitleFont, fontWeight = FontWeight.Bold),
        bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = FontFamily.SansSerif),
        bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
        bodySmall = BaseTypography.bodySmall.copy(fontFamily = FontFamily.SansSerif),
        labelLarge = BaseTypography.labelLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
        labelMedium = BaseTypography.labelMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
        labelSmall = BaseTypography.labelSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
    )

private val GroundedShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(30.dp),
    )

@Composable
fun GroundedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors =
        when {
            dynamicColor && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
            dynamicColor -> dynamicLightColorScheme(LocalContext.current)
            darkTheme -> DarkColors
            else -> LightColors
        }
    MaterialTheme(
        colorScheme = colors,
        typography = GroundedTypography,
        shapes = GroundedShapes,
        content = content,
    )
}
