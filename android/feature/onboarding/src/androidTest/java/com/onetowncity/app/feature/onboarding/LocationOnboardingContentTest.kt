package com.onetowncity.app.feature.onboarding

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.onetowncity.app.core.designsystem.theme.OneTownTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LocationOnboardingContentTest {
    @get:Rule val compose = createComposeRule()

    private fun show(stage: LocationStage, onEnable: () -> Unit = {}, onManual: () -> Unit = {}) =
        compose.setContent {
            OneTownTheme { LocationOnboardingContent(stage, onEnable, onManual, onOpenSettings = {}) }
        }

    @Test fun rationaleOffersBothActionsAndTheReason() {
        show(LocationStage.Rationale)
        compose.onNodeWithText("SIGNAL REQUIRED").assertExists()
        compose.onNodeWithText("Enable location").assertHasClickAction()
        compose.onNodeWithText("Choose city manually").assertHasClickAction()
        compose.onNodeWithText("Open settings").assertDoesNotExist()
    }

    @Test fun deniedNeverDeadEndsTheUser() {
        var manual = false
        show(LocationStage.Denied, onManual = { manual = true })
        compose.onNodeWithText("Open settings").assertExists()
        compose.onNodeWithText("Choose city manually").performClick()
        assertEquals(true, manual)
    }

    @Test fun enableButtonReportsTheTap() {
        var enabled = false
        show(LocationStage.Rationale, onEnable = { enabled = true })
        compose.onNodeWithText("Enable location").performClick()
        assertEquals(true, enabled)
    }
}
