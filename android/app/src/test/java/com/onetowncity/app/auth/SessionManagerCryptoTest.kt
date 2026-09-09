package com.onetowncity.app.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-function tests for the PKCE (RFC 7636) helpers behind SignInScreen's
 * "Continue with Google" flow. Network calls (SupabaseAuthApi, SessionManager)
 * follow this codebase's existing convention of testing only the pure parsing
 * / crypto layer, not live HTTP.
 */
class SessionManagerCryptoTest {

    @Test
    fun `code verifier is base64url with no padding or unreserved characters`() {
        val verifier = generateCodeVerifier()

        assertTrue("verifier length ${verifier.length} should be within RFC 7636's 43-128 range", verifier.length in 43..128)
        assertFalse("verifier must not contain '+'", verifier.contains('+'))
        assertFalse("verifier must not contain '/'", verifier.contains('/'))
        assertFalse("verifier must not contain '=' padding", verifier.contains('='))
        assertTrue(verifier.matches(Regex("^[A-Za-z0-9_-]+$")))
    }

    @Test
    fun `two generated code verifiers are not equal`() {
        assertFalse(generateCodeVerifier() == generateCodeVerifier())
    }

    @Test
    fun `code challenge is deterministic for the same verifier`() {
        val verifier = "fixed-test-verifier-value-1234567890"

        assertEquals(codeChallengeFor(verifier), codeChallengeFor(verifier))
    }

    @Test
    fun `code challenge differs from its verifier and is base64url with no padding`() {
        val verifier = generateCodeVerifier()
        val challenge = codeChallengeFor(verifier)

        assertFalse(challenge == verifier)
        assertFalse(challenge.contains('='))
        assertTrue(challenge.matches(Regex("^[A-Za-z0-9_-]+$")))
        // SHA-256 digest, base64url-encoded without padding, is always 43 chars.
        assertEquals(43, challenge.length)
    }
}
