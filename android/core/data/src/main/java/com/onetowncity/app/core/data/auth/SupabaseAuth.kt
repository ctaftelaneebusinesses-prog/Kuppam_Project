package com.onetowncity.app.core.data.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLDecoder

enum class SignInFailure {
    /** Google or Supabase reported an error (the user declined, or the provider is misconfigured). */
    Provider,

    /** The redirect was not a sign-in we started, or carried no code. */
    InvalidRedirect,

    /** Supabase refused the code exchange. */
    Rejected,

    /** Could not reach Supabase. */
    Network,
}

sealed interface SignInResult {
    data object Success : SignInResult
    data class Failure(val reason: SignInFailure) : SignInResult
}

/**
 * Google sign-in through Supabase, done the way the website does it (the same Supabase project and Google provider),
 * using the browser plus PKCE. The app never sees a Google credential — only the Supabase session that results.
 */
class SupabaseAuth(
    private val config: AuthConfig,
    private val client: OkHttpClient,
    private val store: SessionStore,
    private val nowSeconds: () -> Long = { System.currentTimeMillis() / 1000 },
) : SignInService {
    private val json = Json { ignoreUnknownKeys = true }
    private val refreshLock = Mutex()

    override val isConfigured: Boolean get() = config.isConfigured

    fun hasSession(): Boolean = store.load() != null

    /** Starts a sign-in: remembers a fresh PKCE verifier and returns the URL to open in the browser. */
    override fun beginSignIn(): String {
        val verifier = Pkce.newVerifier()
        store.savePendingVerifier(verifier)
        return "${config.supabaseUrl.trimEnd('/')}/auth/v1/authorize".toHttpUrl().newBuilder()
            .addQueryParameter("provider", "google")
            .addQueryParameter("redirect_to", config.redirectUri)
            .addQueryParameter("code_challenge", Pkce.challenge(verifier))
            .addQueryParameter("code_challenge_method", "s256")
            .build()
            .toString()
    }

    /** Finishes a sign-in from the redirect the browser handed back to the app. */
    override suspend fun completeSignIn(redirect: String): SignInResult {
        if (!redirect.startsWith(config.redirectUri)) return SignInResult.Failure(SignInFailure.InvalidRedirect)
        val params = RedirectParams.parse(redirect)
        if (params["error"] != null) {
            store.clearPendingVerifier()
            return SignInResult.Failure(SignInFailure.Provider)
        }
        val code = params["code"]
        val verifier = store.loadPendingVerifier()
        if (code.isNullOrBlank() || verifier == null) return SignInResult.Failure(SignInFailure.InvalidRedirect)
        // A verifier is single-use: drop it before the network call so a replayed redirect cannot reuse it.
        store.clearPendingVerifier()

        return try {
            val session = tokenRequest(
                "pkce",
                """{"auth_code":${json.encodeToString(code)},"code_verifier":${json.encodeToString(verifier)}}""",
            )
            if (session == null) SignInResult.Failure(SignInFailure.Rejected)
            else {
                store.save(session)
                SignInResult.Success
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            SignInResult.Failure(SignInFailure.Network)
        }
    }

    /**
     * A valid access token, refreshing it first when it has under a minute left. Returns null when there is no session,
     * or Supabase refused the refresh (the session is then cleared: the user must sign in again). A network error keeps
     * the session and returns the current token, since it may still be accepted.
     */
    suspend fun accessToken(): String? = refreshLock.withLock {
        val session = store.load() ?: return null
        if (session.expiresAtEpochSeconds - nowSeconds() > REFRESH_MARGIN_SECONDS) return session.accessToken
        try {
            val refreshed = tokenRequest("refresh_token", """{"refresh_token":${json.encodeToString(session.refreshToken)}}""")
            if (refreshed == null) {
                store.clear()
                null
            } else {
                store.save(refreshed)
                refreshed.accessToken
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            session.accessToken
        }
    }

    override fun signOut() {
        store.clear()
        store.clearPendingVerifier()
    }

    /** Null means Supabase answered with a non-2xx status or an unreadable body. IOException means it was unreachable. */
    private suspend fun tokenRequest(grantType: String, body: String): Session? = withContext(Dispatchers.IO) {
        val url = "${config.supabaseUrl.trimEnd('/')}/auth/v1/token".toHttpUrl().newBuilder()
            .addQueryParameter("grant_type", grantType)
            .build()
        val request = Request.Builder()
            .url(url)
            .header("apikey", config.anonKey)
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            try {
                json.decodeFromString<TokenResponse>(response.body?.string().orEmpty()).toSession(nowSeconds())
            } catch (e: SerializationException) {
                null
            }
        }
    }

    @Serializable
    private data class TokenResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("refresh_token") val refreshToken: String,
        @SerialName("expires_in") val expiresIn: Long = 3600,
        @SerialName("expires_at") val expiresAt: Long? = null,
    ) {
        fun toSession(now: Long) = Session(accessToken, refreshToken, expiresAt ?: (now + expiresIn))
    }

    private companion object {
        const val REFRESH_MARGIN_SECONDS = 60L
    }
}

/** Reads `key=value` pairs from a redirect's query and fragment (Supabase may use either). */
internal object RedirectParams {
    fun parse(url: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        val afterScheme = url.substringAfter("://", url)
        val query = afterScheme.substringAfter('?', "").substringBefore('#')
        val fragment = afterScheme.substringAfter('#', "")
        for (part in listOf(query, fragment)) {
            for (pair in part.split('&')) {
                if (pair.isEmpty()) continue
                val key = URLDecoder.decode(pair.substringBefore('='), "UTF-8")
                val value = URLDecoder.decode(pair.substringAfter('=', ""), "UTF-8")
                out.putIfAbsent(key, value)
            }
        }
        return out
    }
}
