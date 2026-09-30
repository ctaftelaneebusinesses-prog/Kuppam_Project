package com.onetowncity.app.core.data

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds the app's connection to the Django backend. [baseUrl] must be HTTPS and end with `/`; the app module supplies
 * it so debug and release builds can point at different servers without touching this code.
 */
object OneTownApi {
    private val json = Json { ignoreUnknownKeys = true }

    fun categoryRepository(baseUrl: String, client: OkHttpClient = defaultClient()): CategoryRepository =
        NetworkCategoryRepository(retrofit(baseUrl, client).create(CategoryApi::class.java))

    fun listingRepository(baseUrl: String, client: OkHttpClient = defaultClient()): ListingRepository =
        NetworkListingRepository(retrofit(baseUrl, client).create(ListingApi::class.java))

    private fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private fun defaultClient() = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    /** For tests: same wiring, pointed at a MockWebServer (which is plain HTTP, so only tests may call this). */
    internal fun categoryRepositoryForTest(baseUrl: String): CategoryRepository = categoryRepository(baseUrl)

    internal fun listingRepositoryForTest(baseUrl: String): ListingRepository = listingRepository(baseUrl)
}

/** Wires sign-in and the signed-in account call. Kept apart from [OneTownApi] so the public catalog client never carries a token. */
object OneTownAccount {
    /** [apiBaseUrl] is the Django backend (HTTPS, trailing `/`); [auth] talks to Supabase. */
    fun create(
        apiBaseUrl: String,
        authConfig: com.onetowncity.app.core.data.auth.AuthConfig,
        store: com.onetowncity.app.core.data.auth.SessionStore,
    ): Pair<com.onetowncity.app.core.data.auth.SignInService, com.onetowncity.app.core.data.auth.AccountRepository> {
        val base = okhttp3.OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
        val auth = com.onetowncity.app.core.data.auth.SupabaseAuth(authConfig, base, store)
        val apiClient = base.newBuilder()
            .addInterceptor { chain ->
                // Blocking is fine here: OkHttp runs interceptors on its own worker thread.
                val token = kotlinx.coroutines.runBlocking { auth.accessToken() }
                val request = if (token == null) chain.request()
                else chain.request().newBuilder().header("Authorization", "Bearer $token").build()
                chain.proceed(request)
            }
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(apiBaseUrl)
            .client(apiClient)
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
            .build()
        val repository = com.onetowncity.app.core.data.auth.NetworkAccountRepository(
            retrofit.create(com.onetowncity.app.core.data.auth.AccountApi::class.java), auth,
        )
        return auth to repository
    }
}
