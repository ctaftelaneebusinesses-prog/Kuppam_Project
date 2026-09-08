package com.onetowncity.app.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

class DesignSystemThemeTest {
    @Test
    fun `light and dark themes expose design tokens`() {
        assertEquals(OneTownCityColors.Light.primary, OneTownCityColors.Light.primary)
        assertEquals(OneTownCityColors.Dark.primary, OneTownCityColors.Dark.primary)
        assertEquals(OneTownCityTypography.default, OneTownCityTypography.default)
    }

    @Test
    fun `light and dark content colors meet normal text contrast`() {
        listOf(OneTownCityColors.Light, OneTownCityColors.Dark).forEach { scheme ->
            assertTrue(contrastRatio(scheme.primary, scheme.onPrimary) >= 4.5f)
            assertTrue(contrastRatio(scheme.secondary, scheme.onSecondary) >= 4.5f)
            assertTrue(contrastRatio(scheme.tertiary, scheme.onTertiary) >= 4.5f)
            assertTrue(contrastRatio(scheme.error, scheme.onError) >= 4.5f)
            assertTrue(contrastRatio(scheme.surface, scheme.onSurface) >= 4.5f)
            assertTrue(contrastRatio(scheme.surfaceVariant, scheme.onSurfaceVariant) >= 4.5f)
        }
    }

    private fun contrastRatio(first: Color, second: Color): Float {
        val firstLuminance = first.luminance()
        val secondLuminance = second.luminance()
        val lighter = maxOf(firstLuminance, secondLuminance)
        val darker = minOf(firstLuminance, secondLuminance)
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
