package com.onetowncity.app.feature.home

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.onetowncity.app.core.designsystem.theme.OneTownTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BentoBoxDashboardTest {
    @get:Rule val compose = createComposeRule()

    private val tiles = listOf(
        CategoryTile("a", "First", "bi-house-door", TileSpan.Focal),
        CategoryTile("b", "Second", "bi-shop", TileSpan.Compact, count = 12),
    )

    @Test fun tilesAreButtonsAndReportClicks() {
        var clicked: CategoryTile? = null
        compose.setContent {
            OneTownTheme {
                BentoBoxDashboard(HomeUiState.Content(tiles), "Somewhere", onCategoryClick = { clicked = it }, onRetry = {})
            }
        }
        compose.onNodeWithText("First").assertHasClickAction().performClick()
        assertEquals("a", clicked?.key)
    }

    @Test fun countOnlyAppearsWhenTheBackendProvidedOne() {
        compose.setContent {
            OneTownTheme { BentoBoxDashboard(HomeUiState.Content(tiles), null, onCategoryClick = {}, onRetry = {}) }
        }
        compose.onNodeWithText("12").assertExists()
    }

    @Test fun failedStateOffersRetry() {
        var retried = false
        compose.setContent {
            OneTownTheme { BentoBoxDashboard(HomeUiState.Failed, null, onCategoryClick = {}, onRetry = { retried = true }) }
        }
        compose.onNodeWithText("Try again").performClick()
        assertEquals(true, retried)
    }
}
