package com.onetowncity.app.core.data.auth

/** The part of sign-in the app flow depends on, so it can be replaced by a fake in tests. */
interface SignInService {
    /** False when the build has no Supabase URL / key (see android/local.properties), so sign-in cannot work. */
    val isConfigured: Boolean

    /** Starts a sign-in and returns the URL to open in the browser. */
    fun beginSignIn(): String

    /** Finishes a sign-in from the redirect the browser handed back to the app. */
    suspend fun completeSignIn(redirect: String): SignInResult

    fun signOut()
}
