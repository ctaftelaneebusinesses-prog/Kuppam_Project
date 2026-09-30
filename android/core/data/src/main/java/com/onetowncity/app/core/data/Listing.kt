package com.onetowncity.app.core.data

/**
 * One row of a category's listings, reduced to what a list card shows. The backend has a different shape per listing
 * type (a business has a `name`, a job a `job_title`…); [ListingMapper] flattens them so screens never care.
 */
data class Listing(
    val id: Int,
    val modelKey: String,
    val slug: String,
    val title: String,
    /** Second line: address, location, company, source… */
    val subtitle: String?,
    /** The number that matters, already formatted: price, salary range, date. */
    val highlight: String?,
    /** Short label such as "Rent" or the category name. */
    val tag: String?,
    val featured: Boolean,
    /** Backend `placeholder_icon` (a Bootstrap Icons name) used when there is no photo. */
    val iconKey: String,
    /** A real photo URL, or null. The backend's `placehold.co` placeholders are deliberately never returned. */
    val imageUrl: String?,
)

data class ListingPage(val items: List<Listing>, val total: Int, val hasNext: Boolean)

sealed interface ListingsResult {
    data class Success(val page: ListingPage) : ListingsResult
    data class Failure(val reason: LoadFailure) : ListingsResult
}

interface ListingRepository {
    /**
     * One page (1-based) of public listings of [modelKey], narrowed by the server to [categoryKey] using the same rule
     * as the website. Never throws — failures come back as a result.
     */
    suspend fun page(modelKey: String, categoryKey: String?, page: Int): ListingsResult
}
