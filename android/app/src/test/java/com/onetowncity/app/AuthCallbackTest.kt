package com.onetowncity.app

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers how the onetowncity://auth-callback redirect is classified and how
 * a completeSignIn() failure is turned into a user-facing message — the two
 * pure decision points behind AuthCallbackScreen. Malformed input, a
 * provider-reported error (e.g. the user denies Google consent), server
 * error, network error, and timeout are all exercised here per the
 * "Test:" list in the authentication phase spec.
 */
class AuthCallbackTest {

    @Test
    fun `no redirect uri at all is a missing-redirect outcome`() {
        val outcome = classifyAuthCallback(hasRedirectUri = false, errorDescription = null, code = null)
        assertEquals(AuthCallbackOutcome.MissingRedirect, outcome)
    }

    @Test
    fun `provider error description takes priority over a code`() {
        val outcome = classifyAuthCallback(
            hasRedirectUri = true,
            errorDescription = "access_denied",
            code = "some-code",
        )
        assertEquals(AuthCallbackOutcome.ProviderError("access_denied"), outcome)
    }

    @Test
    fun `blank error description is not treated as a real provider error`() {
        val outcome = classifyAuthCallback(hasRedirectUri = true, errorDescription = "  ", code = "abc")
        assertEquals(AuthCallbackOutcome.HasCode("abc"), outcome)
    }

    @Test
    fun `a real code with no error is the success path`() {
        val outcome = classifyAuthCallback(hasRedirectUri = true, errorDescription = null, code = "auth-code-123")
        assertEquals(AuthCallbackOutcome.HasCode("auth-code-123"), outcome)
    }

    @Test
    fun `redirect with neither error nor code is malformed`() {
        val outcome = classifyAuthCallback(hasRedirectUri = true, errorDescription = null, code = null)
        assertEquals(AuthCallbackOutcome.Malformed, outcome)
    }

    @Test
    fun `blank code with no error is malformed, not a false success`() {
        val outcome = classifyAuthCallback(hasRedirectUri = true, errorDescription = null, code = "   ")
        assertEquals(AuthCallbackOutcome.Malformed, outcome)
    }

    @Test
    fun `network failure gets a clean generic message, not the raw exception text`() {
        val message = authCallbackErrorMessage(UnknownHostException("Unable to resolve host \"xyz.supabase.co\""))
        assertEquals("Couldn't reach OneTownCity. Check your connection and try again.", message)
    }

    @Test
    fun `timeout is treated the same as any other network failure`() {
        val message = authCallbackErrorMessage(SocketTimeoutException("timeout"))
        assertEquals("Couldn't reach OneTownCity. Check your connection and try again.", message)
    }

    @Test
    fun `a generic IOException also gets the clean network message`() {
        val message = authCallbackErrorMessage(IOException("Connection reset"))
        assertEquals("Couldn't reach OneTownCity. Check your connection and try again.", message)
    }

    @Test
    fun `server-provided error message is passed through as-is`() {
        val message = authCallbackErrorMessage(IllegalStateException("Invalid grant"))
        assertEquals("Invalid grant", message)
    }

    @Test
    fun `an exception with no message at all falls back to a generic message`() {
        val message = authCallbackErrorMessage(IllegalStateException())
        assertTrue(message.isNotBlank())
        assertEquals("Sign-in failed. Please try again.", message)
    }

    @Test
    fun `an exception with a blank message falls back to a generic message`() {
        val message = authCallbackErrorMessage(IllegalStateException("   "))
        assertEquals("Sign-in failed. Please try again.", message)
    }
}
