package com.onetowncity.app.core.data.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** PKCE (RFC 7636): proves the app that finishes a sign-in is the one that started it, even if another app sees the redirect. */
object Pkce {
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    /** 32 random bytes → 43 URL-safe characters, within RFC 7636's 43–128 range. */
    fun newVerifier(random: SecureRandom = SecureRandom()): String =
        encoder.encodeToString(ByteArray(32).also(random::nextBytes))

    /** S256 challenge: BASE64URL(SHA-256(verifier)). */
    fun challenge(verifier: String): String =
        encoder.encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
}
