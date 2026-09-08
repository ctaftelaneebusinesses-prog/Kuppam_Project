package com.onetowncity.app

import org.junit.Assert.assertEquals
import org.junit.Test

class PlacesToVisitFiltersTest {
    @Test
    fun `places filter searches and scopes by city and category`() {
        val items = listOf(
            PlaceItem(
                id = 1,
                title = "Kuppam River Park",
                category = "Nature",
                description = "A quiet park for evening walks and rest.",
                cityName = "Kuppam",
                address = "Main Road",
                imageUrl = "",
                mapsLink = "",
                distanceLabel = "1.2 km away",
            ),
            PlaceItem(
                id = 2,
                title = "Town Heritage Hall",
                category = "Heritage",
                description = "Historic civic landmark.",
                cityName = "Kuppam",
                address = "Old Town",
                imageUrl = "",
                mapsLink = "",
                distanceLabel = "3.5 km away",
            ),
        )

        val filtered = filterPlacesToVisit(
            items = items,
            query = "river",
            selectedCity = "Kuppam",
            selectedCategory = "Nature",
        )

        assertEquals(listOf("Kuppam River Park"), filtered.map { it.title })
    }
}
