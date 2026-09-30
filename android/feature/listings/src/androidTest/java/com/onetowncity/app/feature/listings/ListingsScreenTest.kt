package com.onetowncity.app.feature.listings

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.onetowncity.app.core.data.Listing
import com.onetowncity.app.core.designsystem.theme.OneTownTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ListingsScreenTest {
    @get:Rule val compose = createComposeRule()

    private val items = listOf(
        Listing(1, "property", "a", "Plot near bus stand", "Kuppam", "₹25,00,000", "Sale", true, "bi-house-door", null),
        Listing(2, "property", "b", "Two bedroom house", "Palamaner", null, "Rent", false, "bi-house-door", null),
    )

    private fun show(state: ListingsUiState, onBack: () -> Unit = {}, onRetry: () -> Unit = {}, onMore: () -> Unit = {}) =
        compose.setContent {
            OneTownTheme { ListingsScreen("Property Listing", state, onBack, onRetry, onMore) }
        }

    @Test fun showsTheListingsTheirDetailsAndTheTotal() {
        show(ListingsUiState.Content(items, total = 2, hasMore = false))
        compose.onNodeWithText("Plot near bus stand").assertExists()
        compose.onNodeWithText("₹25,00,000").assertExists()
        compose.onNodeWithText("Featured").assertExists()
        compose.onNodeWithText("2 listings").assertExists()
    }

    @Test fun backIsReachableAndAnnounced() {
        var back = 0
        show(ListingsUiState.Empty, onBack = { back++ })
        compose.onNodeWithContentDescription("Back").assertHasClickAction().performClick()
        assertEquals(1, back)
    }

    @Test fun emptyAndFailedStatesAreExplainedAndFailedCanRetry() {
        var retried = 0
        show(ListingsUiState.Failed, onRetry = { retried++ })
        compose.onNodeWithText("We couldn't load these listings.").assertExists()
        compose.onNodeWithText("Try again").performClick()
        assertEquals(1, retried)
    }

    @Test fun reachingTheEndAsksForTheNextPage() {
        var asked = 0
        show(ListingsUiState.Content(items, total = 40, hasMore = true), onMore = { asked++ })
        compose.waitForIdle()
        assertEquals(true, asked >= 1)
    }
}
