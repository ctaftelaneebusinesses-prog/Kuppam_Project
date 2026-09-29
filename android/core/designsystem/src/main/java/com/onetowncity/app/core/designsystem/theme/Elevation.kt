package com.onetowncity.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Depth comes from line thickness, never from drop shadows. */
@Immutable
class OneTownElevation(
    val none: Dp = 0.dp,
    /** Widget borders and dividers. */
    val hairline: Dp = 1.dp,
    /** Keyboard/TV focus rings. */
    val focusRing: Dp = 2.dp,
)

val LocalOneTownElevation = staticCompositionLocalOf { OneTownElevation() }

/** Bento widgets are aggressively rounded; dividers are completely sharp. */
@Immutable
class OneTownShapes(
    val widget: Shape = RoundedCornerShape(24.dp),
    val pill: Shape = RoundedCornerShape(percent = 50),
    val sharp: Shape = RectangleShape,
)

val LocalOneTownShapes = staticCompositionLocalOf { OneTownShapes() }
