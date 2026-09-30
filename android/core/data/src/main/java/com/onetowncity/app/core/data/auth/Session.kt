package com.onetowncity.app.core.data.auth

/** A Supabase session. Both tokens are secrets: never log them or put them in crash reports. */
data class Session(
    val accessToken: String,
    val refreshToken: String,
    /** Seconds since the epoch when [accessToken] stops being valid. */
    val expiresAtEpochSeconds: Long,
) {
    override fun toString() = "Session(expiresAt=$expiresAtEpochSeconds)"
}

/** Where the session lives between launches, and the one-time PKCE verifier while the user is away in the browser. */
interface SessionStore {
    fun load(): Session?
    fun save(session: Session)
    fun clear()

    fun loadPendingVerifier(): String?
    fun savePendingVerifier(verifier: String)
    fun clearPendingVerifier()
}

/** What the app needs to reach Supabase. [redirectUri] must be in the project's Auth → URL Configuration allow-list. */
data class AuthConfig(
    val supabaseUrl: String,
    val anonKey: String,
    val redirectUri: String,
) {
    val isConfigured: Boolean get() = supabaseUrl.isNotBlank() && anonKey.isNotBlank()
    override fun toString() = "AuthConfig(supabaseUrl=$supabaseUrl, redirectUri=$redirectUri)"
}
