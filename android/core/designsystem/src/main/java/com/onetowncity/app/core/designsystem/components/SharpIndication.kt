package com.onetowncity.app.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Replaces the Material ripple: a hard, high-contrast white flash while pressed, and a crisp 2.dp
 * white ring while focused (keyboard / D-pad / switch access). Apply `Modifier.clip(shape)` before
 * `clickable` so the flash follows the widget's corners.
 */
object SharpIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        SharpIndicationNode(interactionSource)

    override fun hashCode(): Int = -1
    override fun equals(other: Any?): Boolean = other === this
}

private const val PressedFlashAlpha = 0.22f

private class SharpIndicationNode(
    private val interactionSource: InteractionSource,
) : Modifier.Node(), DrawModifierNode {

    private val flash = Animatable(0f)
    private var focused by mutableStateOf(false)

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> launch { flash.animateTo(PressedFlashAlpha, tween(durationMillis = 50)) }
                    is PressInteraction.Release, is PressInteraction.Cancel ->
                        launch { flash.animateTo(0f, tween(durationMillis = 160)) }
                    is FocusInteraction.Focus -> focused = true
                    is FocusInteraction.Unfocus -> focused = false
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (flash.value > 0f) drawRect(Color.White, alpha = flash.value)
        if (focused) {
            val stroke = 2.dp.toPx()
            drawRect(
                color = Color.White,
                topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke),
            )
        }
    }
}
