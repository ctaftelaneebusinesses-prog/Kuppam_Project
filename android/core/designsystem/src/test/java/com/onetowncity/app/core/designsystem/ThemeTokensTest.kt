package com.onetowncity.app.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.onetowncity.app.core.designsystem.theme.DefaultOneTownColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** WCAG 2.x contrast checks for every text/background pair the design system ships. */
class ThemeTokensTest {
    private val c = DefaultOneTownColors

    private fun contrast(a: Color, b: Color): Double {
        val l1 = a.luminance().toDouble()
        val l2 = b.luminance().toDouble()
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    @Test fun backgroundIsTrueBlack() = assertEquals(Color(0xFF000000), c.background)

    @Test fun bodyTextMeetsAaOnEverySurface() {
        for (surface in listOf(c.background, c.surface, c.surfaceRaised)) {
            assertTrue("primary on $surface", contrast(c.textPrimary, surface) >= 4.5)
            assertTrue("secondary on $surface", contrast(c.textSecondary, surface) >= 4.5)
        }
    }

    /** Red is used for large/bold text and UI components only, where 3:1 is the WCAG requirement. */
    @Test fun accentMeetsLargeTextAndComponentContrast() {
        assertTrue(contrast(c.accent, c.background) >= 3.0)
        assertTrue(contrast(c.onAccent, c.accent) >= 3.0)
    }

    @Test fun bordersAreVisibleAgainstSurfaces() {
        assertTrue(contrast(c.outline, c.background) >= 1.5)
        assertTrue(contrast(c.outlineStrong, c.background) >= 3.0)
    }
}
