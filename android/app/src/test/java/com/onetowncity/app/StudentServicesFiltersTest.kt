package com.onetowncity.app

import org.junit.Assert.assertEquals
import org.junit.Test

class StudentServicesFiltersTest {
    @Test
    fun `student service category filters by actual business category values`() {
        val items = listOf(
            StudentServiceItem(
                id = 1,
                name = "Kuppam Coaching Centre",
                category = "school",
                description = "Tuition and exam prep",
                address = "Main Road",
                phoneNumber = "9876543210",
                website = "",
                mapsLink = "",
                imageUrl = "",
                cityName = "Kuppam",
                citySlug = "kuppam",
            ),
            StudentServiceItem(
                id = 2,
                name = "City Pharmacy",
                category = "pharmacy",
                description = "Medicines and first aid",
                address = "Market Road",
                phoneNumber = "9876543211",
                website = "",
                mapsLink = "",
                imageUrl = "",
                cityName = "Kuppam",
                citySlug = "kuppam",
            ),
        )

        val filtered = filterStudentServices(
            items = items,
            query = "coaching",
            selectedCategory = "school",
        )

        assertEquals(listOf("Kuppam Coaching Centre"), filtered.map { it.name })
    }
}

class ScholarshipsFiltersTest {
    @Test
    fun `scholarship query and category filters respect verified data only`() {
        val items = listOf(
            ScholarshipItem(
                id = 1,
                title = "Post-matric scholarship",
                description = "Official government support for eligible students.",
                category = "Government",
                deadline = "",
                eligibility = "",
                officialUrl = "https://example.gov/scholarship",
                sourceLabel = "Official government portal",
                sourceType = "official",
            ),
            ScholarshipItem(
                id = 2,
                title = "Community learning grant",
                description = "Local program for educational support.",
                category = "Education",
                deadline = "",
                eligibility = "",
                officialUrl = "",
                sourceLabel = "OneTownCity",
                sourceType = "local",
            ),
        )

        val filtered = filterScholarships(
            items = items,
            query = "post-matric",
            selectedCategory = "Government",
        )

        assertEquals(listOf("Post-matric scholarship"), filtered.map { it.title })
    }
}
