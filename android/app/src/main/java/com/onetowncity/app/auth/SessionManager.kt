package com.onetowncity.app.auth

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.onetowncity.app.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Android's half of the *same* Supabase-Google sign-in the web app uses (see
 * core/supabase_auth.py + templates/signin.html's `supabase.auth.signInWithOAuth`)
 * — not a second auth system. Web hands the whole OAuth dance to supabase-js
 * running in a browser; Android gets there by opening that same
 * Supabase-hosted `/auth/v1/authorize` flow in a Custom Tab (see
 * auth/AuthScreens.kt's SignInScreen) using PKCE, so no separate Google Cloud
 * client / SHA-1 fingerprint registration is needed on the Android side.
 *
 * Whatever access_token this produces is verified the exact same way
 * core/api/authentication.py's SupabaseTokenAuthentication already verifies
 * the web's tokens — Django remains the single source of truth for identity
 * and authorization; this file only gets Android a valid bearer token to send.
 */

internal data class SupabaseSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
    val userId: String,
    val email: String,
    val fullName: String,
    val avatarUrl: String,
)

internal sealed class AuthState {
    object SignedOut : AuthState()
    data class SignedIn(
        val userId: String,
        val email: String,
        val fullName: String,
        val avatarUrl: String,
    ) : AuthState()
}

internal class SupabaseAuthException(message: String) : Exception(message)

// android.util.Base64 needs a real device/emulator (or Robolectric, which
// this project doesn't depend on) and java.util.Base64 needs API 26+ without
// core library desugaring (minSdk here is 23) — so PKCE's base64url encoding
// is hand-rolled, matching this codebase's existing no-external-library style
// (see MainActivity.kt's hand-rolled JSON/HTTP), and runs in a plain JVM unit
// test with no Android framework dependency at all.
private const val BASE64_URL_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

private fun base64UrlEncodeNoPadding(bytes: ByteArray): String {
    val builder = StringBuilder((bytes.size * 4 + 2) / 3)
    var i = 0
    while (i < bytes.size) {
        val b0 = bytes[i].toInt() and 0xFF
        val b1 = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 0xFF else 0
        val b2 = if (i + 2 < bytes.size) bytes[i + 2].toInt() and 0xFF else 0
        val triple = (b0 shl 16) or (b1 shl 8) or b2
        builder.append(BASE64_URL_ALPHABET[(triple shr 18) and 0x3F])
        builder.append(BASE64_URL_ALPHABET[(triple shr 12) and 0x3F])
        if (i + 1 < bytes.size) builder.append(BASE64_URL_ALPHABET[(triple shr 6) and 0x3F])
        if (i + 2 < bytes.size) builder.append(BASE64_URL_ALPHABET[triple and 0x3F])
        i += 3
    }
    return builder.toString()
}

/** Random PKCE code_verifier (RFC 7636 4.1: 43-128 chars of unreserved base64url). */
internal fun generateCodeVerifier(): String {
    val bytes = ByteArray(64)
    SecureRandom().nextBytes(bytes)
    return base64UrlEncodeNoPadding(bytes)
}

/** code_challenge = BASE64URL-ENCODE(SHA256(code_verifier)) (RFC 7636 4.2, method S256). */
internal fun codeChallengeFor(verifier: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
    return base64UrlEncodeNoPadding(digest)
}

/**
 * Direct REST calls to Supabase's own Auth (GoTrue) API — never to Django.
 * Uses only the public SUPABASE_URL / anon key (BuildConfig, sourced from
 * local.properties — see app/build.gradle), the same two values
 * templates/signin.html already sends to every browser.
 */
internal object SupabaseAuthApi {
    private val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY

    const val REDIRECT_URI = "onetowncity://auth-callback"

    fun buildAuthorizeUri(codeChallenge: String): Uri =
        Uri.parse("$baseUrl/auth/v1/authorize").buildUpon()
            .appendQueryParameter("provider", "google")
            .appendQueryParameter("redirect_to", REDIRECT_URI)
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter("code_challenge_method", "s256")
            .build()

    suspend fun exchangeCodeForSession(code: String, codeVerifier: String): SupabaseSession =
        withContext(Dispatchers.IO) {
            callToken(
                grantType = "pkce",
                body = JSONObject().apply {
                    put("auth_code", code)
                    put("code_verifier", codeVerifier)
                },
            )
        }

    suspend fun refreshSession(refreshToken: String): SupabaseSession =
        withContext(Dispatchers.IO) {
            callToken(
                grantType = "refresh_token",
                body = JSONObject().apply { put("refresh_token", refreshToken) },
            )
        }

    /** Best-effort: revokes the refresh token server-side. Local sign-out proceeds either way. */
    suspend fun signOut(accessToken: String): Unit = withContext(Dispatchers.IO) {
        try {
            val connection = URL("$baseUrl/auth/v1/logout").openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.setRequestProperty("apikey", anonKey)
                connection.setRequestProperty("Authorization", "Bearer $accessToken")
                connection.doOutput = true
                connection.outputStream.use { it.write(ByteArray(0)) }
                connection.responseCode
            } finally {
                connection.disconnect()
            }
            Unit
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Unit
        }
    }

    private fun callToken(grantType: String, body: JSONObject): SupabaseSession {
        val connection = URL("$baseUrl/auth/v1/token?grant_type=$grantType").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("apikey", anonKey)
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) {
                val parsed = runCatching { JSONObject(responseText) }.getOrNull()
                val message = parsed?.optString("error_description")?.takeIf { it.isNotBlank() }
                    ?: parsed?.optString("msg")?.takeIf { it.isNotBlank() }
                    ?: "Sign-in failed (HTTP $responseCode)."
                throw SupabaseAuthException(message)
            }

            val json = JSONObject(responseText)
            val user = json.optJSONObject("user")
            val metadata = user?.optJSONObject("user_metadata")
            val expiresIn = json.optLong("expires_in", 3600L)
            return SupabaseSession(
                accessToken = json.getString("access_token"),
                refreshToken = json.getString("refresh_token"),
                expiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + expiresIn,
                userId = user?.optString("id").orEmpty(),
                email = user?.optString("email").orEmpty(),
                fullName = metadata?.optString("full_name")?.takeIf { it.isNotBlank() }
                    ?: metadata?.optString("name").orEmpty(),
                avatarUrl = metadata?.optString("avatar_url")?.takeIf { it.isNotBlank() }
                    ?: metadata?.optString("picture").orEmpty(),
            )
        } finally {
            connection.disconnect()
        }
    }
}

/** Encrypted at-rest storage for the access/refresh token pair — session persistence across app restarts. */
private object TokenStore {
    private const val PREFS_NAME = "one_town_city_auth_session"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_EXPIRES_AT = "expires_at"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_EMAIL = "email"
    private const val KEY_FULL_NAME = "full_name"
    private const val KEY_AVATAR_URL = "avatar_url"

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun save(context: Context, session: SupabaseSession) {
        prefs(context).edit {
            putString(KEY_ACCESS_TOKEN, session.accessToken)
            putString(KEY_REFRESH_TOKEN, session.refreshToken)
            putLong(KEY_EXPIRES_AT, session.expiresAtEpochSeconds)
            putString(KEY_USER_ID, session.userId)
            putString(KEY_EMAIL, session.email)
            putString(KEY_FULL_NAME, session.fullName)
            putString(KEY_AVATAR_URL, session.avatarUrl)
        }
    }

    fun load(context: Context): SupabaseSession? {
        val p = prefs(context)
        val accessToken = p.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refreshToken = p.getString(KEY_REFRESH_TOKEN, null) ?: return null
        return SupabaseSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtEpochSeconds = p.getLong(KEY_EXPIRES_AT, 0L),
            userId = p.getString(KEY_USER_ID, "").orEmpty(),
            email = p.getString(KEY_EMAIL, "").orEmpty(),
            fullName = p.getString(KEY_FULL_NAME, "").orEmpty(),
            avatarUrl = p.getString(KEY_AVATAR_URL, "").orEmpty(),
        )
    }

    fun clear(context: Context) {
        prefs(context).edit { clear() }
    }
}

/**
 * Single app-wide session source of truth. `httpJson` in MainActivity.kt
 * calls [ensureFreshAccessToken] for every `requiresAuth = true` request, so
 * every existing and new authenticated call (favorites, notifications,
 * reviews, comments, profile...) transparently gets a valid, auto-refreshed
 * token with zero per-call-site changes.
 */
internal object SessionManager {
    // Refresh a bit before actual expiry so an in-flight request never races
    // a token that goes stale mid-call.
    private const val REFRESH_BUFFER_SECONDS = 60L

    private lateinit var appContext: Context
    private var initialized = false
    private var session: SupabaseSession? = null
    private var pendingCodeVerifier: String? = null

    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext
        session = runCatching { TokenStore.load(appContext) }.getOrNull()
        _authState.value = session?.toAuthState() ?: AuthState.SignedOut
    }

    /** Opens the Supabase-hosted Google sign-in page; returns the URL to launch in a Custom Tab. */
    fun beginSignIn(): Uri {
        val verifier = generateCodeVerifier()
        pendingCodeVerifier = verifier
        return SupabaseAuthApi.buildAuthorizeUri(codeChallengeFor(verifier))
    }

    /** Call once the Custom Tab redirects back to onetowncity://auth-callback?code=... */
    suspend fun completeSignIn(code: String): Result<Unit> {
        val verifier = pendingCodeVerifier
            ?: return Result.failure(SupabaseAuthException("Sign-in session expired. Please try again."))
        return try {
            val newSession = SupabaseAuthApi.exchangeCodeForSession(code, verifier)
            adopt(newSession)
            pendingCodeVerifier = null
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Returns a valid access token, refreshing it first if it's missing/expiring; null when signed out or refresh fails. */
    suspend fun ensureFreshAccessToken(): String? {
        val current = session ?: return null
        val now = System.currentTimeMillis() / 1000L
        if (current.expiresAtEpochSeconds - now > REFRESH_BUFFER_SECONDS) return current.accessToken
        return try {
            val refreshed = SupabaseAuthApi.refreshSession(current.refreshToken)
            adopt(refreshed)
            refreshed.accessToken
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Refresh token itself is invalid/expired (revoked, too old) — the
            // session is genuinely over; drop it so the UI falls back to
            // sign-in-required instead of retrying a dead token forever.
            signOutLocally()
            null
        }
    }

    suspend fun signOut() {
        session?.let { runCatching { SupabaseAuthApi.signOut(it.accessToken) } }
        signOutLocally()
    }

    private fun adopt(newSession: SupabaseSession) {
        session = newSession
        if (::appContext.isInitialized) {
            runCatching { TokenStore.save(appContext, newSession) }
        }
        _authState.value = newSession.toAuthState()
    }

    private fun signOutLocally() {
        session = null
        pendingCodeVerifier = null
        if (::appContext.isInitialized) {
            runCatching { TokenStore.clear(appContext) }
        }
        _authState.value = AuthState.SignedOut
    }

    private fun SupabaseSession.toAuthState() =
        AuthState.SignedIn(userId = userId, email = email, fullName = fullName, avatarUrl = avatarUrl)
}
