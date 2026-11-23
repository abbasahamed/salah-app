package com.salah.designsystem

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Refined Light Palette — Soft neutral warm-white canvas, clean surfaces, charcoal text, deep golden bronze accent
private val Light = lightColorScheme(
    primary = Color(0xFF875F1A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3EBDD),
    onPrimaryContainer = Color(0xFF5A4112),
    secondary = Color(0xFF875F1A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3EBDD),
    onSecondaryContainer = Color(0xFF5A4112),
    background = Color(0xFFF8F7F4),
    onBackground = Color(0xFF1A1A1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1E),
    surfaceVariant = Color(0xFFEFECE6),
    onSurfaceVariant = Color(0xFF71747E),
    surfaceContainer = Color(0xFFF1EFEA),
    surfaceContainerLow = Color(0xFFF6F4F0),
    surfaceContainerHigh = Color(0xFFEBE8E1),
    surfaceContainerHighest = Color(0xFFE4E0D7),
    outline = Color(0xFF71747E),
    outlineVariant = Color(0x121A1A1E),
    error = Color(0xFFBA1A1A),
)

// Refined Dark Palette — Deep Obsidian Canvas, Neutral Slate Surfaces, Champagne Gold strictly for prayer accent
private val Dark = darkColorScheme(
    primary = Color(0xFFD2AE70),
    onPrimary = Color(0xFF17130A),
    primaryContainer = Color(0xFF2B2314),
    onPrimaryContainer = Color(0xFFF0DEB4),
    secondary = Color(0xFFD2AE70),
    onSecondary = Color(0xFF17130A),
    secondaryContainer = Color(0xFF2B2314),
    onSecondaryContainer = Color(0xFFF0DEB4),
    background = Color(0xFF090A0D),
    onBackground = Color(0xFFF5F5F7),
    surface = Color(0xFF16171D),
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = Color(0xFF22242B),
    onSurfaceVariant = Color(0xFF999BA3),
    surfaceContainer = Color(0xFF1C1D22),
    surfaceContainerLow = Color(0xFF121317),
    surfaceContainerHigh = Color(0xFF22242B),
    surfaceContainerHighest = Color(0xFF2B2C35),
    outline = Color(0xFF626670),
    outlineVariant = Color(0x14FFFFFF),
    error = Color(0xFFFFB4A9),
)

@OptIn(ExperimentalTextApi::class)
private fun variableFont(resource: Int, vararg weights: Int): FontFamily =
    if (Build.VERSION.SDK_INT >= 26) {
        FontFamily(weights.map { weight ->
            Font(
                resource,
                weight = FontWeight(weight),
                variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
            )
        })
    } else {
        FontFamily.SansSerif
    }

private val Geist = variableFont(R.font.geist_variable, 300, 400, 500, 600, 700)

private val DefaultLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Bottom,
    trim = LineHeightStyle.Trim.None,
)

// Geist Typography refined to ~80% perceived scale with optically balanced hierarchy
val GeistTypography = Typography(
    // Next-prayer main start time (target: 56-60sp, ~17% reduction)
    displayLarge = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight(300),
        fontSize = 58.sp,
        lineHeight = 62.sp,
        letterSpacing = (-1.2).sp,
        lineHeightStyle = DefaultLineHeightStyle,
        fontFeatureSettings = "tnum",
    ),
    displayMedium = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight(300),
        fontSize = 56.sp,
        lineHeight = 60.sp,
        letterSpacing = (-1.2).sp,
        lineHeightStyle = DefaultLineHeightStyle,
        fontFeatureSettings = "tnum",
    ),
    displaySmall = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight(400),
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.7).sp,
        lineHeightStyle = DefaultLineHeightStyle,
    ),
    // Salah wordmark / Page title (target: 28-30sp, ~17% reduction)
    headlineLarge = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight(600),
        fontSize = 29.sp,
        lineHeight = 35.sp,
        letterSpacing = (-0.5).sp,
        lineHeightStyle = DefaultLineHeightStyle,
    ),
    // Main prayer name (target: 32-34sp, ~17.5% reduction)
    headlineMedium = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight(600),
        fontSize = 33.sp,
        lineHeight = 39.sp,
        letterSpacing = (-0.5).sp,
        lineHeightStyle = DefaultLineHeightStyle,
    ),
    // Section headers / Prayer times heading (target: 20-22sp, ~12.5% reduction)
    headlineSmall = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight(600),
        fontSize = 21.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.3).sp,
        lineHeightStyle = DefaultLineHeightStyle,
    ),
    // Section card titles / Subsection headings
    titleLarge = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight(600),
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp,
        lineHeightStyle = DefaultLineHeightStyle,
    ),
    // Remaining countdown (target: 16-17sp, ~15% reduction)
    titleMedium = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight(500),
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.1).sp,
        lineHeightStyle = DefaultLineHeightStyle,
    ),
    // Location / Subtitle (target: 15-16sp)
    titleSmall = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight(500),
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = DefaultLineHeightStyle,
    ),
    // Prayer list names & times (target: 17-18sp names & times, ~10% reduction)
    bodyLarge = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight(500),
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = DefaultLineHeightStyle,
        fontFeatureSettings = "tnum",
    ),
    // Standard body text (target: 14sp)
    bodyMedium = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight(400),
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = DefaultLineHeightStyle,
    ),
    bodySmall = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight(400),
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = DefaultLineHeightStyle,
    ),
    // Section labels / prominent metadata (target: 13-14sp)
    labelLarge = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight(600),
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = DefaultLineHeightStyle,
        fontFeatureSettings = "tnum",
    ),
    // Eyebrows / Navigation labels / secondary labels (target: 12-13sp)
    labelMedium = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight(500),
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
        lineHeightStyle = DefaultLineHeightStyle,
    ),
    // Fine metadata / subtle tags (target: 11sp)
    labelSmall = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight(500),
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.1.sp,
        lineHeightStyle = DefaultLineHeightStyle,
    ),
)

// Backward compatibility alias for any existing reference
val InstrumentSansTypography = GeistTypography
val DmSansTypography = GeistTypography

enum class SalahTypographyVariant {
    INSTRUMENT_SANS,
    DM_SANS,
    GEIST,
}

var activeTypographyVariant: SalahTypographyVariant = SalahTypographyVariant.GEIST

fun getSalahTypography(variant: SalahTypographyVariant = activeTypographyVariant): Typography =
    GeistTypography

@Composable
fun SalahTheme(
    dark: Boolean,
    typographyVariant: SalahTypographyVariant = activeTypographyVariant,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        typography = GeistTypography,
        shapes = Shapes(
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(18.dp),
            extraLarge = RoundedCornerShape(22.dp),
        ),
        content = content,
    )
}
