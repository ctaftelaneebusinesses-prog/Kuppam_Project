package com.onetowncity.app.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * OneTownCity's color system. Primary is reserved for primary actions, active
 * navigation/selection, and focus — every other surface stays neutral so the
 * accent reads as deliberate rather than decorative (see the redesign's
 * "don't make every element the accent color" rule).
 */
object OneTownCityColors {
    val Light: ColorScheme = lightColorScheme(
        primary = BrandNavy,
        onPrimary = Color.White,
        primaryContainer = PaleBlue,
        onPrimaryContainer = BrandNavy,
        secondary = BrandAmber,
        onSecondary = BrandNavy,
        secondaryContainer = WarmSand,
        onSecondaryContainer = BrandNavy,
        tertiary = BrandOrange,
        onTertiary = BrandNavy,
        background = AppBackground,
        onBackground = Ink,
        surface = SurfaceColor,
        onSurface = Ink,
        surfaceVariant = PanelSurface,
        onSurfaceVariant = InkSoft,
        outline = BorderSoft,
        outlineVariant = BorderStrong,
        error = ErrorRed,
        onError = Color.White,
        errorContainer = ErrorContainer,
        onErrorContainer = ErrorText,
        scrim = Color.Black.copy(alpha = 0.45f),
    )

    val Dark: ColorScheme = darkColorScheme(
        primary = BrandAmber,
        onPrimary = BrandNavy,
        primaryContainer = BrandNavy,
        onPrimaryContainer = Color.White,
        secondary = BrandBlue,
        onSecondary = Color.White,
        secondaryContainer = SlateBlue,
        onSecondaryContainer = Color.White,
        tertiary = BrandOrange,
        onTertiary = BrandNavy,
        background = DarkBackground,
        onBackground = Color.White,
        surface = DarkSurface,
        onSurface = Color.White,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = SoftGray,
        outline = DarkBorder,
        outlineVariant = DarkBorderStrong,
        error = ErrorRed,
        onError = Color.White,
        errorContainer = ErrorContainerDark,
        onErrorContainer = Color(0xFFFFDAD6),
        scrim = Color.Black.copy(alpha = 0.60f),
    )

    val BrandGradient: Brush = Brush.linearGradient(
        colors = listOf(BrandOrange, BrandAmber, BrandOrangeDark),
    )
}

private val BrandNavy = Color(0xFF020617)
private val BrandAmber = Color(0xFFF0A93A)
private val BrandOrange = Color(0xFFF97316)
private val BrandOrangeDark = Color(0xFFEA580C)
private val BrandBlue = Color(0xFF1F63A6)
private val AppBackground = Color(0xFFF7F8FB)
private val SurfaceColor = Color(0xFFFFFFFF)
private val PanelSurface = Color(0xFFF2F5FA)
private val Ink = Color(0xFF14172A)
private val InkSoft = Color(0xFF5B607A)
private val BorderSoft = Color(0xFFE2E5EF)
private val BorderStrong = Color(0xFFCBD0DF)
private val WarmSand = Color(0xFFFDF1DD)
private val PaleBlue = Color(0xFFDCE8F6)
private val ErrorRed = Color(0xFFDC3545)
private val ErrorContainer = Color(0xFFF8D7DA)
private val ErrorText = Color(0xFF7F1D1D)
private val DarkBackground = Color(0xFF020617)
private val DarkSurface = Color(0xFF12131B)
private val DarkSurfaceVariant = Color(0xFF191B26)
private val DarkBorder = Color(0xFF2A2E3B)
private val DarkBorderStrong = Color(0xFF3E4258)
private val SlateBlue = Color(0xFF1E293B)
private val SoftGray = Color(0xFFB9C0D3)
private val ErrorContainerDark = Color(0xFF5D1F26)
