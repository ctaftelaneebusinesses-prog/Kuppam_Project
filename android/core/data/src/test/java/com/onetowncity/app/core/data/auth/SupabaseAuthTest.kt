package com.onetowncity.app.core.data.auth

import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SupabaseAuthTest {
    private val redirect = "onetowncity://auth/callback"
    private lateinit var server: MockWebServer
    private lateinit var store: FakeSessionStore
    private var now = 1_000_000L
    private lateinit var auth: SupabaseAuth

    @Before fun setUp() {
        server = MockWebServer().apply { start() }
        store = FakeSessionStore()
        auth = SupabaseAuth(
            AuthConfig(server.url("/").toString(), "anon-key", redirect), OkHttpClient(), store, nowSeconds = { now },
        )
    }

    @After fun tearDown() = server.shutdown()

    private fun tokens(access: String = "access-1", refresh: String = "refresh-1", expiresIn: Long = 3600) =
        MockResponse().setHeader("Content-Type", "application/json")
            .setBody("""{"access_token":"$access","refresh_token":"$refresh","expires_in":$expiresIn,"token_type":"bearer","user":{"id":"x"}}""")

    @Test fun beginSignInBuildsAGoogleAuthorizeUrlWithAnS256Challenge() {
        val url = auth.beginSignIn().toHttpUrlForTest()
        assertEquals("/auth/v1/authorize", url.encodedPath)
        assertEquals("google", url.queryParameter("provider"))
        assertEquals(redirect, url.queryParameter("redirect_to"))
        assertEquals("s256", url.queryParameter("code_challenge_method"))
        assertEquals(Pkce.challenge(store.verifier!!), url.queryParameter("code_challenge"))
    }

    @Test fun completeSignInExchangesTheCodeWithTheStoredVerifierAndSavesTheSession() = runTest {
        auth.beginSignIn()
        val verifier = store.verifier!!
        server.enqueue(tokens())

        val result = auth.completeSignIn("$redirect?code=abc123")

        assertEquals(SignInResult.Success, result)
        assertEquals("access-1", store.session?.accessToken)
        assertEquals(now + 3600, store.session?.expiresAtEpochSeconds)
        assertNull("verifier is single-use", store.verifier)
        val request = server.takeRequest()
        assertEquals("/auth/v1/token?grant_type=pkce", request.path)
        assertEquals("anon-key", request.getHeader("apikey"))
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"auth_code\":\"abc123\""))
        assertTrue(body.contains("\"code_verifier\":\"$verifier\""))
    }

    @Test fun aProviderErrorFailsWithoutCallingSupabase() = runTest {
        auth.beginSignIn()
        val result = auth.completeSignIn("$redirect?error=access_denied&error_description=User+cancelled")
        assertEquals(SignInResult.Failure(SignInFailure.Provider), result)
        assertEquals(0, server.requestCount)
        assertNull(store.verifier)
    }

    @Test fun errorsInTheFragmentAreAlsoRecognised() = runTest {
        auth.beginSignIn()
        assertEquals(
            SignInResult.Failure(SignInFailure.Provider),
            auth.completeSignIn("$redirect#error=access_denied"),
        )
    }

    @Test fun aRedirectWeDidNotStartIsRejected() = runTest {
        // No begin: there is no verifier, so a code from anywhere else must not be exchanged.
        assertEquals(SignInResult.Failure(SignInFailure.InvalidRedirect), auth.completeSignIn("$redirect?code=abc"))
        assertEquals(0, server.requestCount)
    }

    @Test fun aForeignRedirectUriIsRejected() = runTest {
        auth.beginSignIn()
        assertEquals(
            SignInResult.Failure(SignInFailure.InvalidRedirect),
            auth.completeSignIn("https://evil.example/cb?code=abc"),
        )
        assertEquals(0, server.requestCount)
    }

    @Test fun aRejectedExchangeIsReportedAndNoSessionIsSaved() = runTest {
        auth.beginSignIn()
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"invalid_grant"}"""))
        assertEquals(SignInResult.Failure(SignInFailure.Rejected), auth.completeSignIn("$redirect?code=bad"))
        assertNull(store.session)
    }

    @Test fun anUnreachableSupabaseIsANetworkFailure() = runTest {
        auth.beginSignIn()
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        assertEquals(SignInResult.Failure(SignInFailure.Network), auth.completeSignIn("$redirect?code=abc"))
    }

    @Test fun aFreshTokenIsReturnedWithoutRefreshing() = runTest {
        store.session = Session("fresh", "r", now + 3600)
        assertEquals("fresh", auth.accessToken())
        assertEquals(0, server.requestCount)
    }

    @Test fun anExpiringTokenIsRefreshedAndTheNewSessionSaved() = runTest {
        store.session = Session("old", "refresh-old", now + 30)
        server.enqueue(tokens(access = "new", refresh = "refresh-new"))

        assertEquals("new", auth.accessToken())

        assertEquals("refresh-new", store.session?.refreshToken)
        val request = server.takeRequest()
        assertEquals("/auth/v1/token?grant_type=refresh_token", request.path)
        assertTrue(request.body.readUtf8().contains("\"refresh_token\":\"refresh-old\""))
    }

    @Test fun aRefusedRefreshSignsTheUserOut() = runTest {
        store.session = Session("old", "revoked", now - 10)
        server.enqueue(MockResponse().setResponseCode(400))
        assertNull(auth.accessToken())
        assertNull(store.session)
    }

    @Test fun aNetworkErrorDuringRefreshKeepsTheSession() = runTest {
        store.session = Session("old", "r", now - 10)
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        assertEquals("old", auth.accessToken())
        assertNotNull(store.session)
    }

    @Test fun signOutClearsEverything() {
        store.session = Session("a", "r", now + 100)
        store.verifier = "v"
        auth.signOut()
        assertFalse(auth.hasSession())
        assertNull(store.verifier)
    }

    @Test fun sessionAndConfigNeverPrintTheirSecrets() {
        val text = Session("SECRET-ACCESS", "SECRET-REFRESH", 5).toString() + AuthConfig("u", "SECRET-ANON", "r")
        assertFalse(text.contains("SECRET"))
    }

    private fun String.toHttpUrlForTest() = toHttpUrl()
}
