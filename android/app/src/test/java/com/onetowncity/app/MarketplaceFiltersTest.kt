package com.onetowncity.app

import org.junit.Assert.assertEquals
import org.junit.Test

class MarketplaceFiltersTest {
    @Test
    fun `marketplace filters match by category and keyword`() {
        val items = listOf(
            MarketplaceItem(
                id = 1,
                title = "Physics textbook",
                description = "Used but in good condition.",
                price = "₹3500",
                condition = "Used",
                transactionType = "sale",
                category = "books",
                location = "Kuppam Main Road",
                imageUrl = "",
                contactNumber = "",
                ownerName = "Student seller",
                ownerId = 101,
            ),
            MarketplaceItem(
                id = 2,
                title = "Laptop",
                description = "For exchange with study tablet.",
                price = "₹20000",
                condition = "Like new",
                transactionType = "exchange",
                category = "electronics",
                location = "Near college",
                imageUrl = "",
                contactNumber = "",
                ownerName = "Asha",
                ownerId = 102,
            ),
            MarketplaceItem(
                id = 3,
                title = "Cycle",
                description = "Affordable transport",
                price = "₹4500",
                condition = "Good",
                transactionType = "sale",
                category = "transport",
                location = "Kuppam",
                imageUrl = "",
                contactNumber = "",
                ownerName = "Ravi",
                ownerId = 103,
            ),
        )

        val filtered = filterMarketplaceItems(
            items = items,
            query = "physics",
            selectedCategory = "books",
            typeFilter = MarketplaceTypeFilter.ALL,
        )

        assertEquals(listOf("Physics textbook"), filtered.map { it.title })
    }
}
