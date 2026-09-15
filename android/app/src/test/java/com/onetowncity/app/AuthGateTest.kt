package com.onetowncity.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers the mandatory auth gate's routing decision (OneTownCityAppShell's
 * "do not show Home to an unauthenticated user" invariant) as a pure
 * function of (isSignedIn, currentRoute) — see authGateDestination.
 */
class AuthGateTest {

    @Test
    fun `signed out on a normal screen is routed to login`() {
        assertEquals(AuthGateTarget.LOGIN, authGateDestination(isSignedIn = false, currentRoute = "home"))
    }

    @Test
    fun `signed out on saved tab is routed to login`() {
        assertEquals(AuthGateTarget.LOGIN, authGateDestination(isSignedIn = false, currentRoute = "saved"))
    }

    @Test
    fun `signed in on the login screen is routed home`() {
        assertEquals(AuthGateTarget.HOME, authGateDestination(isSignedIn = true, currentRoute = "login"))
    }

    @Test
    fun `signed in on a normal screen stays put`() {
        assertNull(authGateDestination(isSignedIn = true, currentRoute = "home"))
    }

    @Test
    fun `signed out already on login stays put`() {
        assertNull(authGateDestination(isSignedIn = false, currentRoute = "login"))
    }

    @Test
    fun `signed out on the auth callback screen is not yanked away mid-flow`() {
        assertNull(authGateDestination(isSignedIn = false, currentRoute = "auth-callback"))
    }

    @Test
    fun `signing in completed via a cold-start deep link (no login screen in the stack) still reaches home`() {
        // Process was killed while the user was in the OAuth Custom Tab, then
        // relaunched straight into onetowncity://auth-callback — "login" was
        // never actually visited, so the gate must not require it.
        assertEquals(AuthGateTarget.HOME, authGateDestination(isSignedIn = true, currentRoute = "auth-callback"))
    }

    @Test
    fun `no current destination yet is not routed`() {
        assertNull(authGateDestination(isSignedIn = false, currentRoute = null))
        assertNull(authGateDestination(isSignedIn = true, currentRoute = null))
    }

    @Test
    fun `logging out or an expired session while browsing any tab returns to login`() {
        listOf("home", "search", "students", "saved", "profile", "business/12", "sign-in").forEach { route ->
            assertEquals(
                "expected route '$route' to redirect to login once signed out",
                AuthGateTarget.LOGIN,
                authGateDestination(isSignedIn = false, currentRoute = route),
            )
        }
    }
}
