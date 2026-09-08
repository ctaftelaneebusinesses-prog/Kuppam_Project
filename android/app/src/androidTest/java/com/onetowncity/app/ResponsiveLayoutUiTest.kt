package com.onetowncity.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.onetowncity.app.designsystem.OneTownCityButton
import com.onetowncity.app.designsystem.OneTownCityButtonVariant
import com.onetowncity.app.designsystem.OneTownCityEmptyState
import com.onetowncity.app.designsystem.OneTownCityErrorState
import com.onetowncity.app.designsystem.OneTownCitySearchBar
import com.onetowncity.app.designsystem.OneTownCityTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResponsiveLayoutUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun responsive_components_do_not_crash_with_long_strings() {
        composeTestRule.setContent {
            OneTownCityTheme(darkTheme = false) {
                MaterialTheme {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    ) {
                        OneTownCitySearchBar(
                            query = "This is a very long city or search query to test overflow behavior",
                            onQueryChange = {},
                            placeholder = "Very long placeholder text designed to test truncation",
                            onClear = {},
                        )
                        OneTownCityButton(
                            text = "This is an extremely long button label intended to test wrapping and truncation",
                            onClick = {},
                            variant = OneTownCityButtonVariant.Primary,
                        )
                        OneTownCityEmptyState(
                            title = "The title is intentionally long enough to test multi-line handling and layout resilience",
                            message = "This is a long message to verify text remains readable without clipping or overlap across width constraints.",
                            action = {
                                OneTownCityButton(
                                    text = "Retry now with a long action label",
                                    onClick = {},
                                    variant = OneTownCityButtonVariant.Outlined,
                                )
                            },
                        )
                        OneTownCityErrorState(
                            title = "A long title that should stay readable under pressure",
                            message = "A longer restoration message that should not overlap the retry button or collapse the layout.",
                            onRetry = {},
                        )
                    }
                }
            }
        }

        composeTestRule.onRoot().assertExists()
    }
}
