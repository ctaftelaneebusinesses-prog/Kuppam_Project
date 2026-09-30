package com.onetowncity.app.core.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keeps the session (and the pending PKCE verifier) encrypted with an AES-256-GCM key that lives in the Android Keystore
 * and never leaves the secure hardware/TEE where available. If the key is lost (new lock screen, restored backup) the data
 * simply becomes unreadable and the user signs in again — this class never returns garbage.
 */
class KeystoreSessionStore(context: Context) : SessionStore {
    private val prefs = context.applicationContext.getSharedPreferences("onetowncity_auth", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    override fun load(): Session? = read(KEY_SESSION)?.let {
        runCatching { json.decodeFromString<Stored>(it).toSession() }.getOrNull()
    }

    override fun save(session: Session) =
        write(KEY_SESSION, json.encodeToString(Stored.from(session)))

    override fun clear() {
        prefs.edit().remove(KEY_SESSION).apply()
    }

    override fun loadPendingVerifier(): String? = read(KEY_VERIFIER)

    override fun savePendingVerifier(verifier: String) = write(KEY_VERIFIER, verifier)

    override fun clearPendingVerifier() {
        prefs.edit().remove(KEY_VERIFIER).apply()
    }

    private fun write(name: String, plain: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val blob = cipher.iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        prefs.edit().putString(name, Base64.encodeToString(blob, Base64.NO_WRAP)).apply()
    }

    private fun read(name: String): String? {
        val encoded = prefs.getString(name, null) ?: return null
        return try {
            val blob = Base64.decode(encoded, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, blob, 0, IV_BYTES))
            }
            String(cipher.doFinal(blob, IV_BYTES, blob.size - IV_BYTES), Charsets.UTF_8)
        } catch (e: Exception) {
            prefs.edit().remove(name).apply()
            null
        }
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
        }.generateKey()
    }

    @Serializable
    private data class Stored(
        @SerialName("a") val accessToken: String,
        @SerialName("r") val refreshToken: String,
        @SerialName("e") val expiresAt: Long,
    ) {
        fun toSession() = Session(accessToken, refreshToken, expiresAt)

        companion object {
            fun from(s: Session) = Stored(s.accessToken, s.refreshToken, s.expiresAtEpochSeconds)
        }
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "onetowncity_session_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SESSION = "session"
        const val KEY_VERIFIER = "pending_verifier"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
