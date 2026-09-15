package com.onetowncity.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.onetowncity.app.designsystem.OneTownCityBottomNavItem
import com.onetowncity.app.designsystem.OneTownCityBottomNavigation
import com.onetowncity.app.designsystem.OneTownCityTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression coverage for the bottom-navigation label-wrap bug: at large Android font scales,
 * OneTownCityBottomNavigation must keep every label ("Students", "Profile", ...) on a single
 * line -- never wrapped mid-word, clipped, shrunk, or hidden -- while preserving click behavior,
 * selection state, and minimum touch target size.
 */
@RunWith(AndroidJUnit4::class)
class BottomNavigationAccessibilityUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testItems = listOf(
        OneTownCityBottomNavItem("Home", Icons.Filled.Home, Icons.Outlined.Home),
        OneTownCityBottomNavItem("Search", Icons.Filled.Search, Icons.Outlined.Search),
        OneTownCityBottomNavItem("Students", Icons.Outlined.School, Icons.Outlined.School),
        OneTownCityBottomNavItem("Saved", Icons.Filled.Star, Icons.Outlined.Star),
        OneTownCityBottomNavItem("Profile", Icons.Filled.Person, Icons.Outlined.Person),
    )

    private fun setNavContent(
        fontScale: Float,
        widthDp: Int = 360,
        onTabSelected: (Int) -> Unit = {},
        selectedTab: () -> Int = { 0 },
    ) {
        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = fontScale)
            ) {
                OneTownCityTheme(darkTheme = false) {
                    Box(Modifier.width(widthDp.dp)) {
                        OneTownCityBottomNavigation(
                            selectedTab = selectedTab(),
                            onTabSelected = onTabSelected,
                            items = testItems,
                        )
                    }
                }
            }
        }
    }

    private fun assertSingleLineLabel(label: String) {
        val node = composeTestRule.onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode()
        val accessibilityAction = node.config.getOrNull(SemanticsActions.GetTextLayoutResult)
        assertTrue("No text-layout action found for label '$label'", accessibilityAction != null)
        val results = mutableListOf<TextLayoutResult>()
        accessibilityAction!!.action!!.invoke(results)
        assertEquals(
            "Label '$label' wrapped to ${results.first().lineCount} line(s) instead of rendering on one line",
            1,
            results.first().lineCount,
        )
    }

    @Test
    fun labels_render_single_line_at_default_font_scale() {
        setNavContent(fontScale = 1.0f)
        testItems.forEach { assertSingleLineLabel(it.label) }
    }

    @Test
    fun labels_render_single_line_at_115_font_scale() {
        setNavContent(fontScale = 1.15f)
        testItems.forEach { assertSingleLineLabel(it.label) }
    }

    @Test
    fun labels_render_single_line_at_130_font_scale() {
        setNavContent(fontScale = 1.3f)
        testItems.forEach { assertSingleLineLabel(it.label) }
    }

    @Test
    fun labels_render_single_line_at_150_font_scale() {
        setNavContent(fontScale = 1.5f)
        testItems.forEach { assertSingleLineLabel(it.label) }
    }

    @Test
    fun labels_render_single_line_at_200_font_scale() {
        setNavContent(fontScale = 2.0f)
        testItems.forEach { assertSingleLineLabel(it.label) }
    }

    @Test
    fun labels_render_single_line_at_200_font_scale_in_landscape_width() {
        setNavContent(fontScale = 2.0f, widthDp = 800)
        testItems.forEach { assertSingleLineLabel(it.label) }
    }

    @Test
    fun tapping_each_item_selects_it_at_large_font_scale() {
        var selectedTab by mutableIntStateOf(0)
        setNavContent(
            fontScale = 2.0f,
            onTabSelected = { selectedTab = it },
            selectedTab = { selectedTab },
        )
        testItems.indices.forEach { index ->
            composeTestRule.onNodeWithText(testItems[index].label).performClick()
            composeTestRule.waitForIdle()
            assertEquals(index, selectedTab)
        }
    }

    @Test
    fun touch_targets_meet_minimum_size_at_large_font_scale() {
        setNavContent(fontScale = 2.0f)
        testItems.forEach { item ->
            composeTestRule.onNodeWithText(item.label).assertHeightIsAtLeast(48.dp)
        }
    }
}
