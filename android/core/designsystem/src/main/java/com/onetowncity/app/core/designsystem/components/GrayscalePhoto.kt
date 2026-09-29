package com.onetowncity.app.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale

/**
 * A photo that rests in grayscale and animates to full colour while [revealed]. Callers decide what
 * reveals it — press, focus, long-press, or an accessibility action — so colour is never gated
 * behind a gesture that only some people can perform. Load the [painter] with whatever image
 * loader the screen uses; this component stays loader-agnostic.
 */
@Composable
fun GrayscalePhoto(
    painter: Painter,
    contentDescription: String?,
    revealed: Boolean,
    modifier: Modifier = Modifier,
) {
    val saturation by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "photoSaturation",
    )
    val filter = remember(saturation) {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(saturation) })
    }
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        colorFilter = filter,
    )
}
