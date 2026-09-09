package com.onetowncity.app

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
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
internal suspend fun fetchCategories(listingModel: String = ""): List<CategoryOption> = withContext(Dispatchers.IO) {
    val url = URL(
        "$API_BASE_URL/api/v1/categories/" +
            if (listingModel.isNotBlank()) "?listing_model=" + URLEncoder.encode(listingModel, "UTF-8") else "",
    )
    val connection = url.openConnection() as HttpURLConnection
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
        val array = if (responseText.isBlank()) JSONArray() else JSONArray(responseText)
        val options = mutableListOf<CategoryOption>()
        for (i in 0 until array.length()) {
            options += parseCategoryOption(array.getJSONObject(i))
        }
        options
    } finally {
        connection.disconnect()
    }
}

/** Shared nested-`city` reader — every listing serializer nests city as {"name": "...", "slug": "..."} (core/api/serializers.py's LocationSerializer). */
internal fun JSONObject.cityName(fallback: String): String = optJSONObject("city")?.optString("name")?.takeIf { it.isNotBlank() } ?: fallback

internal fun JSONObject.citySlug(): String = optJSONObject("city")?.optString("slug", "").orEmpty()
