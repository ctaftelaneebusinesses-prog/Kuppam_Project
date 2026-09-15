package com.onetowncity.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * buildScholarshipsUrl/buildLostFoundUrl were factored out of
 * fetchScholarships/fetchLostFound so ListingsViewModel's
 * stale-while-revalidate can peek the offline cache for the exact same URL a
 * live fetch would hit (see ListingsViewModel.kt's doc comment). These guard
 * that extraction: same real endpoint, same query/city params as before.
 */
class ListingUrlBuildersTest {
    @Test
    fun `scholarships url targets the real endpoint with query and city`() {
        val url = buildScholarshipsUrl(query = "merit", citySlug = "kuppam", page = 2)

        assertTrue(url.contains("/api/v1/listings/scholarship/"))
        assertTrue(url.contains("q=merit"))
        assertTrue(url.contains("city=kuppam"))
        assertTrue(url.contains("page=2"))
    }

    @Test
    fun `lost and found url targets the real endpoint with query and city`() {
        val url = buildLostFoundUrl(query = "wallet", citySlug = "kuppam", page = 1)

        assertTrue(url.contains("/api/v1/listings/lostfound/"))
        assertTrue(url.contains("q=wallet"))
        assertTrue(url.contains("city=kuppam"))
    }

    @Test
    fun `events url restricts to upcoming by default`() {
        val url = buildEventsUrl(query = "", citySlug = "kuppam", page = 1, pageSize = 10)

        assertTrue(url.contains("upcoming=true"))
    }

    @Test
    fun `events url omits the upcoming filter when the All date filter is requested`() {
        val url = buildEventsUrl(query = "", citySlug = "kuppam", page = 1, pageSize = 10, restrictToUpcoming = false)

        assertFalse(url.contains("upcoming=true"))
        assertTrue(url.contains("/api/v1/listings/event/"))
    }
}
