package com.onetowncity.app.cache

import java.net.URL
import java.net.URLDecoder

internal enum class DataFreshness { LIVE, CACHED_FRESH, CACHED_STALE }

internal data class CacheTarget(val entityType: String, val citySlug: String?)

private val CACHEABLE_PREFIXES = listOf("/api/v1/listings/", "/api/v1/categories/")

/**
 * Decides whether a GET response for this URL is worth caching at all, and
 * if so what entity type / city it belongs to — pure and URL-string-driven
 * so it's unit-testable with no Android framework or network dependency.
 * Only public discovery content (listings + categories) is ever eligible;
 * everything auth-scoped (favorites, notifications, profile, account
 * mutations) is excluded before this is even consulted, since httpJson only
 * routes GET+!requiresAuth calls through the cache layer at all — see
 * "USER-SPECIFIC DATA" in the offline-first spec.
 */
internal fun cacheTargetFor(urlString: String): CacheTarget? {
    val url = runCatching { URL(urlString) }.getOrNull() ?: return null
    val path = url.path
    if (CACHEABLE_PREFIXES.none { path.startsWith(it) }) return null
    val entityType = if (path.startsWith("/api/v1/categories/")) {
        "category"
    } else {
        path.removePrefix("/api/v1/listings/").trim('/').substringBefore('/').ifBlank { "listing" }
    }
    val citySlug = url.query
        ?.split("&")
        ?.firstNotNullOfOrNull { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2 && parts[0] == "city") runCatching { URLDecoder.decode(parts[1], "UTF-8") }.getOrNull() else null
        }
        ?.takeIf { it.isNotBlank() }
    return CacheTarget(entityType = entityType, citySlug = citySlug)
}

/** Listings churn faster than categories, so they get a shorter freshness window before a background revalidation is worth doing. */
internal const val LISTING_CACHE_MAX_AGE_MS = 30 * 60 * 1000L
internal const val CATEGORY_CACHE_MAX_AGE_MS = 24 * 60 * 60 * 1000L

internal fun maxAgeForEntityType(entityType: String): Long =
    if (entityType == "category") CATEGORY_CACHE_MAX_AGE_MS else LISTING_CACHE_MAX_AGE_MS

internal fun freshnessFor(cachedAtMillis: Long, maxAgeMillis: Long, nowMillis: Long = System.currentTimeMillis()): DataFreshness =
    if (nowMillis - cachedAtMillis <= maxAgeMillis) DataFreshness.CACHED_FRESH else DataFreshness.CACHED_STALE
