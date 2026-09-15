package com.onetowncity.app.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/**
 * Formalizes OneTownCityCornerRadii as a Material3 Shapes set, wired into
 * OneTownCityTheme so any component that doesn't specify its own shape
 * (default Card, AlertDialog, etc.) still picks up OneTownCity's rounded
 * corner language instead of Material's stock shapes.
 */
val OneTownCityShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(OneTownCityCornerRadii.xs),
    small = RoundedCornerShape(OneTownCityCornerRadii.sm),
    medium = RoundedCornerShape(OneTownCityCornerRadii.md),
    large = RoundedCornerShape(OneTownCityCornerRadii.lg),
    extraLarge = RoundedCornerShape(OneTownCityCornerRadii.xl),
)
