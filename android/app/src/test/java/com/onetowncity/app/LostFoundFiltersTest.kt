package com.onetowncity.app

import org.junit.Assert.assertEquals
import org.junit.Test

class LostFoundFiltersTest {
    @Test
    fun `lost and found filters respect the requested item type and query`() {
        val items = listOf(
            LostFoundItem(
                id = 1,
                title = "Blue water bottle",
                category = "Lost",
                description = "Blue insulated bottle near the library entrance.",
                location = "Library, Kuppam",
                date = "2026-09-01",
                imageUrl = "",
                contact = "",
                reportUrl = "",
                cityName = "Kuppam",
            ),
            LostFoundItem(
                id = 2,
                title = "College ID card",
                category = "Found",
                description = "Found near auditorium after class.",
                location = "Auditorium, Kuppam",
                date = "2026-08-31",
                imageUrl = "",
                contact = "",
                reportUrl = "",
                cityName = "Kuppam",
            ),
        )

        val filtered = filterLostFound(
            items = items,
            query = "water",
            selectedCategory = "Lost",
        )

        assertEquals(listOf("Blue water bottle"), filtered.map { it.title })
    }
}
