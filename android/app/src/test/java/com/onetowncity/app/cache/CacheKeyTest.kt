package com.onetowncity.app.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers cacheTargetFor/freshnessFor/maxAgeForEntityType — the pure decision
 * layer behind the offline-first cache (see MainActivity.httpJsonWithFreshness).
 */
class CacheKeyTest {

    @Test
    fun `a business listing url is cacheable with its city slug`() {
        val target = cacheTargetFor("https://onetowncity.com/api/v1/listings/business/?category=retail&city=kuppam&page=1")
        assertEquals(CacheTarget(entityType = "business", citySlug = "kuppam"), target)
    }

    @Test
    fun `a categories url is cacheable as global (no city)`() {
        val target = cacheTargetFor("https://onetowncity.com/api/v1/categories/?listing_model=business")
        assertEquals(CacheTarget(entityType = "category", citySlug = null), target)
    }

    @Test
    fun `a listings url with no city query param has a null city slug`() {
        val target = cacheTargetFor("https://onetowncity.com/api/v1/listings/property/?page=1")
        assertEquals(CacheTarget(entityType = "property", citySlug = null), target)
    }

    @Test
    fun `an auth-scoped endpoint is never cacheable`() {
        assertNull(cacheTargetFor("https://onetowncity.com/api/v1/my/favorites/?page=1"))
        assertNull(cacheTargetFor("https://onetowncity.com/api/v1/auth/me/"))
        assertNull(cacheTargetFor("https://onetowncity.com/api/v1/notifications/?page=1"))
    }

    @Test
    fun `a non-listings public endpoint like reverse-geocode is not cached by this mechanism`() {
        assertNull(cacheTargetFor("https://onetowncity.com/api/v1/locations/reverse-geocode/"))
    }

    @Test
    fun `a malformed url is not cacheable rather than throwing`() {
        assertNull(cacheTargetFor("not a url"))
    }

    @Test
    fun `city slug with url-encoded characters is decoded`() {
        val target = cacheTargetFor("https://onetowncity.com/api/v1/listings/event/?city=san%20jose")
        assertEquals("san jose", target?.citySlug)
    }

    @Test
    fun `entity type comes from the path segment right after listings`() {
        assertEquals("scholarship", cacheTargetFor("https://onetowncity.com/api/v1/listings/scholarship/?page=1")?.entityType)
        assertEquals("lostfound", cacheTargetFor("https://onetowncity.com/api/v1/listings/lostfound/?page=1")?.entityType)
        assertEquals("project", cacheTargetFor("https://onetowncity.com/api/v1/listings/project/?page=1")?.entityType)
    }

    @Test
    fun `categories use a much longer max age than listings`() {
        assert(maxAgeForEntityType("category") > maxAgeForEntityType("business"))
    }

    @Test
    fun `freshness is FRESH within the max age window`() {
        val now = 1_000_000L
        val cachedAt = now - 60_000L
        assertEquals(DataFreshness.CACHED_FRESH, freshnessFor(cachedAt, maxAgeMillis = 120_000L, nowMillis = now))
    }

    @Test
    fun `freshness is STALE once past the max age window`() {
        val now = 1_000_000L
        val cachedAt = now - 200_000L
        assertEquals(DataFreshness.CACHED_STALE, freshnessFor(cachedAt, maxAgeMillis = 120_000L, nowMillis = now))
    }

    @Test
    fun `freshness at exactly the max age boundary is still FRESH`() {
        val now = 1_000_000L
        val cachedAt = now - 120_000L
        assertEquals(DataFreshness.CACHED_FRESH, freshnessFor(cachedAt, maxAgeMillis = 120_000L, nowMillis = now))
    }
}
