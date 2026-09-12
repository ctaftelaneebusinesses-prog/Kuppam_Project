package com.onetowncity.app

import com.onetowncity.app.cache.NetworkMonitor
import com.onetowncity.app.cache.OfflineCache
import com.onetowncity.app.cache.cacheTargetFor
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Small pieces shared by Business/Property/Project browse screens
 * (BusinessScreens.kt, PropertyScreens.kt, ProjectScreens.kt) — Category is
 * one model reused across every listing type (core/models.py's Category has
 * a `listing_model` field), so one fetch covers all three instead of a
 * per-type copy.
 */

internal data class CategoryOption(val key: String, val label: String, val listingModel: String = "")

private fun parseCategoryOption(json: JSONObject) = CategoryOption(
    key = json.optString("key", ""),
    label = json.optString("label", ""),
    listingModel = json.optString("listing_model", ""),
)

/**
 * GET /api/v1/categories/?listing_model=<key> — public. Pass a blank
 * listingModel to fetch every category (used by HomeScreen's category grid,
 * which routes by each category's own listing_model). Unlike every other
 * endpoint this app calls, core.api.views.categories() returns a bare JSON
 * array (`Response(CategorySerializer(qs, many=True).data)`), not a
 * {"results": [...]} page envelope, so this can't go through httpJson
 * (which parses every response as a JSONObject) — same reasoning as the
 * existing hand-rolled fetchers (fetchEvents, fetchCitySuggestions).
 */
/**
 * Categories are global (no `city=` query param — core.api.views.categories
 * takes no city filter), long-lived, and central to Home's category grid,
 * so they're cached the same way every listings endpoint is via httpJson —
 * except this endpoint returns a bare JSON array, not the
 * {"results": [...]} envelope httpJson/fetchListPage expect, so it needs
 * its own small cache-aware fetch rather than reusing httpJson directly.
 */
internal suspend fun fetchCategories(listingModel: String = ""): List<CategoryOption> {
    val urlString = "$API_BASE_URL/api/v1/categories/" +
        if (listingModel.isNotBlank()) "?listing_model=" + URLEncoder.encode(listingModel, "UTF-8") else ""
    val cacheTarget = cacheTargetFor(urlString)

    suspend fun fetchLive(): String = withContext(Dispatchers.IO) {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("Accept", "application/json")
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) {
                throw IllegalStateException(apiErrorMessage(responseText, "Unable to load categories (HTTP $responseCode)."))
            }
            if (cacheTarget != null) {
                OfflineCache.write(urlString, cacheTarget.citySlug, cacheTarget.entityType, responseText)
            }
            responseText
        } finally {
            connection.disconnect()
        }
    }

    val responseText = if (cacheTarget == null) {
        fetchLive()
    } else if (!NetworkMonitor.isOnlineNow()) {
        OfflineCache.read(urlString)?.json
            ?: throw OfflineNoCacheException("You're offline and categories haven't been loaded yet. Connect to the internet once to load them.")
    } else {
        try {
            fetchLive()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            OfflineCache.read(urlString)?.json ?: throw e
        }
    }

    val array = if (responseText.isBlank()) JSONArray() else JSONArray(responseText)
    val options = mutableListOf<CategoryOption>()
    for (i in 0 until array.length()) {
        options += parseCategoryOption(array.getJSONObject(i))
    }
    return options
}

/** Shared nested-`city` reader — every listing serializer nests city as {"name": "...", "slug": "..."} (core/api/serializers.py's LocationSerializer). */
internal fun JSONObject.cityName(fallback: String): String = optJSONObject("city")?.optString("name")?.takeIf { it.isNotBlank() } ?: fallback

internal fun JSONObject.citySlug(): String = optJSONObject("city")?.optString("slug", "").orEmpty()
