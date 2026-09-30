package com.onetowncity.app.core.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One element of `GET /api/v1/categories/`. Unknown fields are ignored, so the server can add fields without breaking old apps. */
@Serializable
internal data class CategoryDto(
    val id: Int,
    val key: String,
    val label: String,
    val parent: Int? = null,
    @SerialName("listing_model") val listingModel: String = "",
    @SerialName("business_subcategory") val businessSubcategory: String = "",
    val icon: String = "",
    val order: Int = 0,
)

internal fun CategoryDto.toDomain() = Category(
    id = id,
    key = key,
    label = label,
    iconKey = icon,
    order = order,
    parentId = parent,
)
