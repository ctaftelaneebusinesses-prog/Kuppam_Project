package com.onetowncity.app.feature.auth

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.onetowncity.app.core.designsystem.theme.OneTownTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AuthScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test fun signInOffersGoogleAndReportsTheTap() {
        var taps = 0
        compose.setContent { OneTownTheme { SignInScreen(busy = false, error = null, onContinueWithGoogle = { taps++ }) } }
        compose.onNodeWithText("Continue with Google").performClick()
        assertEquals(1, taps)
    }

    @Test fun signInShowsTheErrorAndHidesTheButtonWhileBusy() {
        compose.setContent { OneTownTheme { SignInScreen(busy = true, error = SignInError.Network, onContinueWithGoogle = {}) } }
        compose.onNodeWithText("Signing you in…").assertExists()
        compose.onNodeWithText("Continue with Google").assertDoesNotExist()
        compose.onNodeWithText("We couldn't reach the sign-in service.", substring = true).assertExists()
    }

    @Test fun continueNeedsBothBoxesTicked() {
        var confirmed = 0
        compose.setContent {
            OneTownTheme {
                ConsentScreen(busy = false, saveFailed = false, onConfirm = { confirmed++ }, onReadTerms = {}, onReadPrivacy = {}, onSignOut = {})
            }
        }
        compose.onNodeWithText("Continue").assertIsNotEnabled()
        compose.onNodeWithText("I confirm that I am 18 years of age or older.").performClick()
        compose.onNodeWithText("Continue").assertIsNotEnabled()
        compose.onNodeWithText("I agree to the Terms of Service and Privacy Policy.").performClick()
        compose.onNodeWithText("Continue").assertIsEnabled().performClick()
        assertEquals(1, confirmed)
    }

    @Test fun consentCanBeAbandonedBySigningOut() {
        var signedOut = false
        compose.setContent {
            OneTownTheme {
                ConsentScreen(busy = false, saveFailed = false, onConfirm = {}, onReadTerms = {}, onReadPrivacy = {}, onSignOut = { signedOut = true })
            }
        }
        compose.onNodeWithText("Sign out").performClick()
        assertEquals(true, signedOut)
    }
}
