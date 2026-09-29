package com.onetowncity.app.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onetowncity.app.core.designsystem.theme.OneTownTheme

/**
 * One modular "Bento box" widget: hairline-bordered, 24.dp corners, monochrome line icon, sans title and an
 * optional dot-matrix count. Height is a *minimum* so larger system font sizes grow the tile rather than clip it.
 *
 * [media] is an optional photo slot (see [GrayscalePhoto]); it receives `revealed`, which is true while the tile is
 * pressed, focused, or long-press-latched. Long-press is an extra — press and focus reveal too — and TalkBack gets the
 * same toggle as a named action through [onLongClickLabel].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BentoTile(
    title: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 132.dp,
    count: Int? = null,
    onLongClickLabel: String? = null,
    media: (@Composable BoxScope.(revealed: Boolean) -> Unit)? = null,
) {
    val colors = OneTownTheme.colors
    val shape = OneTownTheme.shapes.widget
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    var latched by rememberSaveable { mutableStateOf(false) }
    val revealed = pressed || focused || latched

    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.surface)
            .border(OneTownTheme.elevation.hairline, if (focused) colors.outlineStrong else colors.outline, shape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
                onLongClickLabel = onLongClickLabel,
                onLongClick = if (media != null) ({ latched = !latched }) else null,
            ),
    ) {
        if (media != null) {
            media(revealed)
            // Keeps title and count legible over any photo.
            Box(
                Modifier
                    .matchParentSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    colorFilter = ColorFilter.tint(colors.textPrimary),
                )
                if (count != null) DotMatrixText(count.toString())
            }
            OneTownText(
                text = title,
                modifier = Modifier.padding(top = 16.dp),
                style = OneTownTheme.typography.sansTitle,
            )
        }
    }
}
