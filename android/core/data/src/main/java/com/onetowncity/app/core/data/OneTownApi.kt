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

    fun categoryRepository(baseUrl: String, client: OkHttpClient = defaultClient()): CategoryRepository {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return NetworkCategoryRepository(retrofit.create(CategoryApi::class.java))
    }

    private fun defaultClient() = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    /** For tests: same wiring, pointed at a MockWebServer (which is plain HTTP, so only tests may call this). */
    internal fun categoryRepositoryForTest(baseUrl: String): CategoryRepository = categoryRepository(baseUrl)
}
