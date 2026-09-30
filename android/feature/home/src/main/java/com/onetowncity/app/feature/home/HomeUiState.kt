package com.onetowncity.app.feature.home

import androidx.compose.runtime.Immutable

/** How much room a tile takes in the bento grid. */
enum class TileSpan {
    /** Full width — the headline directories. */
    Focal,

    /** One column — utilities. */
    Compact,
}

/** How many of the first categories (in the server's own `order`) are drawn as focal tiles. */
const val FOCAL_TILE_COUNT = 2

/**
 * Focal vs compact is a presentation choice made from the position the server already ordered the categories in;
 * no category name or key is hard-coded, so adding or reordering a category on the backend just works.
 */
fun spanForPosition(index: Int): TileSpan = if (index < FOCAL_TILE_COUNT) TileSpan.Focal else TileSpan.Compact

/** One category as the dashboard draws it. [iconKey] is the backend `icon` value (e.g. `bi-house-door`). */
@Immutable
data class CategoryTile(
    val key: String,
    val label: String,
    val iconKey: String,
    val span: TileSpan,
    /** Listing count, once the backend exposes one. Null hides the number instead of inventing it. */
    val count: Int? = null,
    /** The listing type behind this category (`business`, `job`…), needed to fetch its listings. */
    val listingModel: String = "",
)

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Empty : HomeUiState
    data object Failed : HomeUiState
    data class Content(val tiles: List<CategoryTile>) : HomeUiState
}
