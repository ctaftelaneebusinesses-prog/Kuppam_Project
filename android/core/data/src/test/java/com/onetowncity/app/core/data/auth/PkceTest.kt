package com.onetowncity.app.core.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PkceTest {
    /** The worked example from RFC 7636, Appendix B. */
    @Test fun challengeMatchesTheRfcTestVector() {
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            Pkce.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"),
        )
    }

    @Test fun verifiersAreLongEnoughUrlSafeAndUnique() {
        val a = Pkce.newVerifier()
        val b = Pkce.newVerifier()
        assertNotEquals(a, b)
        assertTrue(a.length in 43..128)
        assertTrue(a.all { it.isLetterOrDigit() || it == '-' || it == '_' })
    }
}
