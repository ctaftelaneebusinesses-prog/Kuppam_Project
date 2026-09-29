package com.onetowncity.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colour tokens. 95% of the UI is black, white or gray; [accent] is the single exception and is
 * reserved for active states, the "Enable location" button and notification badges. Nothing here
 * is mapped onto Material colour roles — this design system has no Material dependency.
 */
@Immutable
class OneTownColors(
    /** True black — "the void". */
    val background: Color,
    /** Cards, sheets and dialogs. */
    val surface: Color,
    /** A step above [surface]: pressed/raised containers and disabled controls. */
    val surfaceRaised: Color,
    /** Hairline separators and widget borders. */
    val outline: Color,
    /** Focus rings and high-emphasis borders. */
    val outlineStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val onAccent: Color,
)

val DefaultOneTownColors = OneTownColors(
    background = Color(0xFF000000),
    surface = Color(0xFF111111),
    surfaceRaised = Color(0xFF1A1A1A),
    outline = Color(0xFF3A3A3A),
    outlineStrong = Color(0xFFFFFFFF),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF9A9A9A),
    accent = Color(0xFFEA2F2F),
    onAccent = Color(0xFFFFFFFF),
)

val LocalOneTownColors = staticCompositionLocalOf { DefaultOneTownColors }
