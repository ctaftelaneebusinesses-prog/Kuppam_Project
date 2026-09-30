package com.onetowncity.app.core.designsystem

import androidx.annotation.DrawableRes

/**
 * Maps a backend category `icon` value (a Bootstrap Icons class such as `bi-house-door`, the same key the website
 * renders) to its bundled monochrome line-art vector. Unknown or new keys fall back to the generic tag, which is also
 * the backend's own default, so a category added on the server still renders. Icons: Bootstrap Icons, MIT licence.
 */
@DrawableRes
fun categoryIconRes(iconKey: String): Int = when (iconKey.removePrefix("bi-")) {
    "house-door" -> R.drawable.ic_bi_house_door
    "shop" -> R.drawable.ic_bi_shop
    "briefcase" -> R.drawable.ic_bi_briefcase
    "calendar-event" -> R.drawable.ic_bi_calendar_event
    "cup-hot" -> R.drawable.ic_bi_cup_hot
    "hospital" -> R.drawable.ic_bi_hospital
    "mortarboard" -> R.drawable.ic_bi_mortarboard
    "mortarboard-fill" -> R.drawable.ic_bi_mortarboard_fill
    "bus-front" -> R.drawable.ic_bi_bus_front
    "newspaper" -> R.drawable.ic_bi_newspaper
    "cone-striped" -> R.drawable.ic_bi_cone_striped
    "wrench-adjustable" -> R.drawable.ic_bi_wrench_adjustable
    "binoculars" -> R.drawable.ic_bi_binoculars
    "book-half" -> R.drawable.ic_bi_book_half
    "life-preserver" -> R.drawable.ic_bi_life_preserver
    "arrow-left-right" -> R.drawable.ic_bi_arrow_left_right
    "search-heart" -> R.drawable.ic_bi_search_heart
    "broadcast" -> R.drawable.ic_bi_broadcast
    else -> R.drawable.ic_bi_tag
}
