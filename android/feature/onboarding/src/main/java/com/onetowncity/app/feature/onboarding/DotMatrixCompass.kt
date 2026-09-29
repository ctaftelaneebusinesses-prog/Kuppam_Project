package com.onetowncity.app.feature.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.onetowncity.app.core.designsystem.rememberReducedMotion
import com.onetowncity.app.core.designsystem.theme.OneTownTheme
import kotlin.math.cos
import kotlin.math.sin

private const val OuterDots = 72
private const val InnerDots = 40
private const val TrailDegrees = 120f
private const val StillSweepDegrees = 40f
private const val SweepMillis = 6_000

/**
 * A compass built entirely from dots: an outer ring with a radar-style sweep trailing behind a needle, a dimmer inner
 * ring and a crosshair. Purely decorative (hidden from screen readers) and monochrome — the accent red is kept for
 * actions. With system animations off it draws one still frame.
 */
@Composable
fun DotMatrixCompass(modifier: Modifier = Modifier) {
    val bright = OneTownTheme.colors.textPrimary
    val dim = OneTownTheme.colors.textSecondary
    val sweep = if (rememberReducedMotion()) {
        StillSweepDegrees
    } else {
        val transition = rememberInfiniteTransition(label = "compass")
        val angle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(SweepMillis, easing = LinearEasing)),
            label = "compassSweep",
        )
        angle
    }

    Canvas(modifier.aspectRatio(1f).clearAndSetSemantics { }) {
        val radius = size.minDimension / 2f - 10.dp.toPx()
        val dot = 2.5.dp.toPx()

        // Outer ring — brightest just behind the sweeping needle, fading over TrailDegrees.
        for (i in 0 until OuterDots) {
            val angle = i * 360f / OuterDots
            val behind = ((sweep - angle) % 360f + 360f) % 360f
            val alpha = if (behind <= TrailDegrees) 1f - 0.85f * behind / TrailDegrees else 0.15f
            val cardinal = i % (OuterDots / 4) == 0
            dotAt(angle, radius, if (cardinal) dot * 1.7f else dot, bright.copy(alpha = if (cardinal) maxOf(alpha, 0.6f) else alpha))
        }
        // Inner ring, static.
        for (i in 0 until InnerDots) {
            dotAt(i * 360f / InnerDots, radius * 0.62f, dot * 0.8f, dim.copy(alpha = 0.55f))
        }
        // Crosshair along the four axes.
        for (axis in 0 until 4) {
            for (step in 1..5) {
                dotAt(axis * 90f, radius * (0.18f + 0.08f * step), dot * 0.7f, dim.copy(alpha = 0.7f))
            }
        }
        // Needle following the sweep.
        for (step in 1..7) {
            dotAt(sweep, radius * 0.08f * step, dot * (0.7f + 0.06f * step), bright)
        }
        drawCircle(bright, radius = dot * 1.4f, center = center)
    }
}

/** Draws a dot [distance] px from the centre at [degrees] clockwise from 12 o'clock. */
private fun DrawScope.dotAt(degrees: Float, distance: Float, radius: Float, color: Color) {
    val radians = Math.toRadians(degrees.toDouble())
    drawCircle(
        color = color,
        radius = radius,
        center = Offset(center.x + distance * sin(radians).toFloat(), center.y - distance * cos(radians).toFloat()),
    )
}
