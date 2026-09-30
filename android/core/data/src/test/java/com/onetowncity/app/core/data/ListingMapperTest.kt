package com.onetowncity.app.core.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class ListingMapperTest {
    private val locale = Locale.forLanguageTag("en-IN")

    private fun map(json: String) = ListingMapper.map(Json.parseToJsonElement(json).jsonObject, locale)

    @Test fun businessUsesNameAddressAndCategoryLabel() {
        val listing = map("""{"id":141,"model_key":"business","slug":"car-garage","name":"CAR GARAGE","address":"Palamaner road, Venkatagiri",
            "listing_category":{"key":"repair","label":"Repair Services"},"placeholder_icon":"bi-wrench-adjustable","has_image":false,
            "display_image":"https://placehold.co/600x400?text=Hello+Kuppam","is_featured":false}""")!!
        assertEquals("CAR GARAGE", listing.title)
        assertEquals("Palamaner road, Venkatagiri", listing.subtitle)
        assertEquals("Repair Services", listing.tag)
        assertEquals("bi-wrench-adjustable", listing.iconKey)
        assertNull("placeholder images are never surfaced", listing.imageUrl)
    }

    @Test fun propertyFormatsThePriceWithIndianGroupingAndCapitalisesTheType() {
        fun price(p: String) = map("""{"id":1,"model_key":"property","slug":"s","title":"T","price":"$p","property_type":"rent","location":"L"}""")!!
        assertEquals("₹10,000", price("10000.00").highlight)
        assertEquals("₹25,00,000", price("2500000.00").highlight)
        assertEquals("₹1,234.5", price("1234.50").highlight)
        assertEquals("Rent", price("1").tag)
    }

    @Test fun anUnreadablePriceIsDroppedNotShownRaw() {
        assertNull(map("""{"id":1,"model_key":"property","slug":"s","title":"T","price":"call us"}""")!!.highlight)
    }

    @Test fun jobCombinesCompanyAndLocationAndKeepsTheSalaryText() {
        val listing = map("""{"id":13,"model_key":"job","slug":"s","job_title":"Site Supervisor","company":"kuppam construction",
            "location":"kuppam","salary":"₹20,000–₹30,000","is_featured":true}""")!!
        assertEquals("Site Supervisor", listing.title)
        assertEquals("kuppam construction · kuppam", listing.subtitle)
        assertEquals("₹20,000–₹30,000", listing.highlight)
        assertEquals(true, listing.featured)
    }

    @Test fun jobWithOnlyOneOfCompanyOrLocationHasNoStraySeparator() {
        assertEquals("kuppam", map("""{"id":1,"model_key":"job","slug":"s","job_title":"J","location":"kuppam"}""")!!.subtitle)
        assertNull(map("""{"id":1,"model_key":"job","slug":"s","job_title":"J"}""")!!.subtitle)
    }

    @Test fun eventAndNewsShowAReadableDate() {
        val event = map("""{"id":3,"model_key":"event","slug":"s","title":"Fair","event_date":"2026-11-12","location":"Temple"}""")!!
        val date = event.highlight!!
        assertEquals(true, date.contains("2026") && date.contains("12") && date.contains("Nov"))
        val news = map("""{"id":4,"model_key":"news","slug":"s","title":"Story","published_date":"2026-08-01","source":"Hello Kuppam"}""")!!
        assertEquals("Hello Kuppam", news.subtitle)
        assertEquals(true, news.highlight!!.contains("2026"))
    }

    @Test fun aBadDateIsDropped() {
        assertNull(map("""{"id":3,"model_key":"event","slug":"s","title":"Fair","event_date":"soon"}""")!!.highlight)
    }

    @Test fun otherTypesFallBackToLocationOrProvider() {
        assertEquals("Chittoor", map("""{"id":5,"model_key":"lostfound","slug":"s","title":"Lost bag","location":"Chittoor"}""")!!.subtitle)
        assertEquals("State Govt", map("""{"id":6,"model_key":"scholarship","slug":"s","title":"Merit","provider":"State Govt"}""")!!.subtitle)
    }

    @Test fun aRealPhotoIsKept() {
        val url = "https://cdn.example/x.jpg"
        assertEquals(url, map("""{"id":1,"model_key":"business","slug":"s","name":"N","has_image":true,"display_image":"$url"}""")!!.imageUrl)
    }

    @Test fun rowsWithoutAnIdOrTitleAreDropped() {
        assertNull(map("""{"model_key":"business","name":"No id"}"""))
        assertNull(map("""{"id":1,"model_key":"business"}"""))
        assertNull(map("""{"id":1,"name":"No model"}"""))
    }
}
