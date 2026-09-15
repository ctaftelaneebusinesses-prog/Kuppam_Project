package com.onetowncity.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Buy / Sell / Exchange has no separate backend model (see
 * BusinessScreens.kt's MarketplaceFeatureScreen doc comment) — it is the
 * real `category=marketplace` value on Business.CATEGORY_CHOICES
 * (core/models.py), served by the same /api/v1/listings/business/ endpoint
 * as every other Business category. These tests guard against silently
 * regressing back to a disconnected/placeholder marketplace screen.
 */
class MarketplaceCategoryTest {

    @Test
    fun `marketplace list url targets the real business endpoint with the marketplace category filter`() {
        val url = buildBusinessListUrl(query = "", categoryKey = "marketplace", citySlug = "kuppam", page = 1)

        assertTrue(url.contains("/api/v1/listings/business/"))
        assertTrue(url.contains("category=marketplace"))
        assertTrue(url.contains("city=kuppam"))
    }

    @Test
    fun `marketplace list url carries a search query through to the shared business endpoint`() {
        val url = buildBusinessListUrl(query = "cycle", categoryKey = "marketplace", citySlug = "", page = 1)

        assertTrue(url.contains("q=cycle"))
        assertTrue(url.contains("category=marketplace"))
    }

    @Test
    fun `businesses expose a real marketplace category choice labeled Buy Sell Exchange`() {
        val marketplace = businessCategoryOptions.first { it.key == "marketplace" }
        assertEquals("Buy / Sell / Exchange", marketplace.label)
    }
}
