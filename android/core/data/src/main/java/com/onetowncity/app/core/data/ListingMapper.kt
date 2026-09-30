package com.onetowncity.app.core.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.DateTimeParseException
import java.util.Locale

/** Flattens the per-type listing JSON into [Listing]. Rows too broken to show (no id or no title) are dropped. */
internal object ListingMapper {
    fun map(o: JsonObject, locale: Locale = Locale.getDefault()): Listing? {
        val id = (o["id"] as? JsonPrimitive)?.intOrNull ?: return null
        val modelKey = o.str("model_key") ?: return null
        val title = o.str("title") ?: o.str("name") ?: o.str("job_title") ?: return null

        val categoryLabel = (o["listing_category"] as? JsonObject)?.str("label")
        val subtitle: String?
        val highlight: String?
        val tag: String?
        when (modelKey) {
            "business" -> {
                subtitle = o.str("address"); highlight = null; tag = categoryLabel
            }
            "property" -> {
                subtitle = o.str("location"); highlight = price(o.str("price"), locale)
                tag = o.str("property_type")?.replaceFirstChar { it.titlecase(locale) }
            }
            "job" -> {
                subtitle = listOfNotNull(o.str("company"), o.str("location")).joinToString(" · ").ifBlank { null }
                highlight = o.str("salary"); tag = categoryLabel
            }
            "event" -> {
                subtitle = o.str("location"); highlight = date(o.str("event_date"), locale); tag = null
            }
            "news" -> {
                subtitle = o.str("source"); highlight = date(o.str("published_date"), locale); tag = null
            }
            else -> {
                subtitle = o.str("location") ?: o.str("provider"); highlight = null; tag = categoryLabel
            }
        }

        val hasPhoto = (o["has_image"] as? JsonPrimitive)?.contentOrNull == "true"
        return Listing(
            id = id,
            modelKey = modelKey,
            slug = o.str("slug").orEmpty(),
            title = title,
            subtitle = subtitle,
            highlight = highlight,
            tag = tag,
            featured = (o["is_featured"] as? JsonPrimitive)?.contentOrNull == "true",
            iconKey = o.str("placeholder_icon").orEmpty(),
            imageUrl = if (hasPhoto) o.str("display_image") else null,
        )
    }

    private fun JsonObject.str(name: String): String? =
        (this[name] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

    /** "10000.00" → "₹10,000", "2500000" → "₹25,00,000" (lakh/crore grouping). Unparseable or negative values are dropped, not shown raw. */
    private fun price(raw: String?, @Suppress("UNUSED_PARAMETER") locale: Locale): String? {
        val amount = raw?.toBigDecimalOrNull()?.takeIf { it.signum() >= 0 }
            ?.setScale(2, RoundingMode.HALF_UP)?.stripTrailingZeros() ?: return null
        val plain = amount.toPlainString()
        val whole = plain.substringBefore('.')
        val fraction = plain.substringAfter('.', "")
        return "₹" + groupIndian(whole) + if (fraction.isNotEmpty()) ".$fraction" else ""
    }

    /** Last three digits, then groups of two: 2500000 → 25,00,000. Done by hand so it never depends on device locale data. */
    private fun groupIndian(digits: String): String {
        if (digits.length <= 3) return digits
        val head = digits.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
        return "$head,${digits.takeLast(3)}"
    }

    private fun date(raw: String?, locale: Locale): String? = try {
        raw?.let { LocalDate.parse(it).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)) }
    } catch (e: DateTimeParseException) {
        null
    }

}
