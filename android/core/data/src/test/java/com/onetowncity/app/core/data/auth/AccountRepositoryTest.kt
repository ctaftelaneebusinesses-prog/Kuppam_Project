package com.onetowncity.app.core.data.auth

import com.onetowncity.app.core.data.LoadFailure
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AccountRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var store: FakeSessionStore
    private lateinit var repository: AccountRepository

    @Before fun setUp() {
        server = MockWebServer().apply { start() }
        store = FakeSessionStore(Session("token", "refresh", Long.MAX_VALUE / 2))
        val auth = SupabaseAuth(AuthConfig(server.url("/").toString(), "anon", "onetowncity://auth/callback"), OkHttpClient(), store)
        val api = Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
            .build().create(AccountApi::class.java)
        repository = NetworkAccountRepository(api, auth)
    }

    @After fun tearDown() = server.shutdown()

    private fun json(body: String, code: Int = 200) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    @Test fun noSessionMeansSignedOutWithoutAnyRequest() = runTest {
        store.session = null
        assertEquals(AccountStatus.SignedOut, repository.status())
        assertEquals(0, server.requestCount)
    }

    @Test fun aConfirmedAccountIsReady() = runTest {
        server.enqueue(json("""{"full_name":"Asha","username":"asha","consent_confirmed":true,"role":"user"}"""))
        assertEquals(AccountStatus.Ready("Asha"), repository.status())
        assertEquals("/api/v1/auth/me/", server.takeRequest().path)
    }

    @Test fun anUnconfirmedAccountNeedsConsent() = runTest {
        server.enqueue(json("""{"full_name":"","username":"asha","consent_confirmed":false}"""))
        assertEquals(AccountStatus.NeedsConsent, repository.status())
    }

    @Test fun confirmingSendsBothFlagsThenReportsTheServersView() = runTest {
        server.enqueue(json("""{"consent_confirmed":true}"""))
        server.enqueue(json("""{"full_name":"Asha","username":"asha","consent_confirmed":true}"""))

        assertEquals(AccountStatus.Ready("Asha"), repository.confirmConsent())

        val post = server.takeRequest()
        assertEquals("POST", post.method)
        assertEquals("/api/v1/auth/age-confirmation/", post.path)
        val body = post.body.readUtf8()
        assertEquals(true, body.contains("\"confirm_adult\":true") && body.contains("\"accept_terms\":true"))
    }

    @Test fun aRejectedTokenSignsTheUserOut() = runTest {
        server.enqueue(json("""{"error":{"code":"authentication_required"}}""", code = 401))
        assertEquals(AccountStatus.SignedOut, repository.status())
        assertNull(store.session)
    }

    @Test fun aBlockedAccountIsReported() = runTest {
        server.enqueue(json("""{"error":{"code":"permission_denied"}}""", code = 403))
        assertEquals(AccountStatus.Blocked, repository.status())
    }

    @Test fun aServerErrorKeepsTheSession() = runTest {
        server.enqueue(json("{}", code = 500))
        assertEquals(AccountStatus.Failed(LoadFailure.Server), repository.status())
        assertEquals("token", store.session?.accessToken)
    }

    @Test fun aNetworkFailureKeepsTheSession() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        assertEquals(AccountStatus.Failed(LoadFailure.Network), repository.status())
        assertEquals("token", store.session?.accessToken)
    }
}
