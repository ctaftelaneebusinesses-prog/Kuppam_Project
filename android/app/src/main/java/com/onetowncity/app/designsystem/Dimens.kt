package com.onetowncity.app.designsystem

import androidx.compose.ui.unit.dp

/** Coherent spacing scale — every screen should reach for one of these instead of an arbitrary dp literal. */
object OneTownCitySpacing {
    val none = 0.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 40.dp
    val giant = 48.dp
}

/** Corner-radius scale backing both OneTownCityShapes and any component that needs a one-off shape. */
object OneTownCityCornerRadii {
    val none = 0.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val pill = 999.dp
}

/** Tonal-elevation scale — surfaces pick one of these rather than a raw `N.dp` literal. */
object OneTownCityElevation {
    val none = 0.dp
    val low = 2.dp
    val medium = 6.dp
    val high = 12.dp
    val large = 20.dp
}

object OneTownCityBorders {
    val thin = 1.dp
    val medium = 1.5.dp
}

/**
 * Android's accessibility guidance calls for a minimum 48x48dp touch target
 * on every interactive control, even one whose visual content (an icon, a
 * compact chip) is smaller than that. Components in this package apply this
 * either via `defaultMinSize`/`sizeIn` or Material3's own
 * `minimumInteractiveComponentSize()` modifier.
 */
object OneTownCityTouchTarget {
    val minSize = 48.dp
}
