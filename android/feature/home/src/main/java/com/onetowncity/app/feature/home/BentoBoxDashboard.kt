package com.onetowncity.app.feature.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onetowncity.app.core.designsystem.categoryIconRes
import com.onetowncity.app.core.designsystem.components.BentoTile
import com.onetowncity.app.core.designsystem.components.DotMatrixHeader
import com.onetowncity.app.core.designsystem.components.OneTownSecondaryButton
import com.onetowncity.app.core.designsystem.components.OneTownText
import com.onetowncity.app.core.designsystem.rememberReducedMotion
import com.onetowncity.app.core.designsystem.theme.OneTownTheme

private val ContentMaxWidth = 640.dp
private val GridSpacing = 12.dp
private val FocalMinHeight = 200.dp
private val CompactHeights = listOf(128.dp, 164.dp)

/**
 * The main screen: every backend category as a modular bento widget in a staggered (masonry) grid. Focal tiles take
 * both columns; compact tiles alternate two minimum heights so the columns interlock instead of forming rows.
 * Content is capped at 640.dp and centred, so tablets and foldables get the same composition rather than
 * stretched tiles.
 */
@Composable
fun BentoBoxDashboard(
    state: HomeUiState,
    cityName: String?,
    onCategoryClick: (CategoryTile) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OneTownTheme.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.widthIn(max = ContentMaxWidth).fillMaxSize()) {
            DashboardHeader(cityName)
            when (state) {
                HomeUiState.Loading -> LoadingGrid()
                HomeUiState.Empty -> Message(stringResource(R.string.home_empty))
                HomeUiState.Failed -> Message(stringResource(R.string.home_error), onRetry)
                is HomeUiState.Content -> TileGrid(state.tiles, onCategoryClick)
            }
        }
    }
}

@Composable
private fun DashboardHeader(cityName: String?) {
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp)) {
        DotMatrixHeader(stringResource(R.string.home_title), color = OneTownTheme.colors.textPrimary)
        if (cityName != null) {
            OneTownText(
                text = cityName,
                modifier = Modifier.padding(top = 4.dp),
                style = OneTownTheme.typography.sansBody,
                color = OneTownTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun TileGrid(tiles: List<CategoryTile>, onCategoryClick: (CategoryTile) -> Unit) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        state = rememberLazyStaggeredGridState(),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalItemSpacing = GridSpacing,
        horizontalArrangement = Arrangement.spacedBy(GridSpacing),
    ) {
        itemsIndexed(
            items = tiles,
            key = { _, tile -> tile.key },
            span = { _, tile ->
                if (tile.span == TileSpan.Focal) StaggeredGridItemSpan.FullLine else StaggeredGridItemSpan.SingleLane
            },
        ) { index, tile ->
            BentoTile(
                title = tile.label,
                iconRes = categoryIconRes(tile.iconKey),
                count = tile.count,
                minHeight = tileMinHeight(tile.span, index),
                onClick = { onCategoryClick(tile) },
            )
        }
    }
}

private fun tileMinHeight(span: TileSpan, index: Int): Dp =
    if (span == TileSpan.Focal) FocalMinHeight else CompactHeights[index % CompactHeights.size]

/** Outlined placeholders in the real layout, so the screen has its final shape while loading (no lone spinner). */
@Composable
private fun LoadingGrid() {
    val description = stringResource(R.string.home_loading)
    val pulse = if (rememberReducedMotion()) 0.6f else {
        val transition = rememberInfiniteTransition(label = "loading")
        val value by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 900), RepeatMode.Reverse),
            label = "loadingPulse",
        )
        value
    }
    val colors = OneTownTheme.colors
    val shape = OneTownTheme.shapes.widget
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(GridSpacing),
    ) {
        Placeholder(pulse, FocalMinHeight, Modifier.fillMaxWidth(), colors.surface, colors.outline, shape)
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(GridSpacing)) {
                Placeholder(pulse, CompactHeights[0], Modifier.weight(1f), colors.surface, colors.outline, shape)
                Placeholder(pulse, CompactHeights[1], Modifier.weight(1f), colors.surface, colors.outline, shape)
            }
        }
    }
}

@Composable
private fun Placeholder(
    alpha: Float,
    minHeight: Dp,
    modifier: Modifier,
    fill: Color,
    line: Color,
    shape: Shape,
) {
    Box(
        modifier
            .heightIn(min = minHeight)
            .alpha(alpha)
            .clip(shape)
            .background(fill)
            .border(OneTownTheme.elevation.hairline, line, shape),
    )
}

@Composable
private fun Message(text: String, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OneTownText(text, style = OneTownTheme.typography.sansBody, color = OneTownTheme.colors.textSecondary)
        if (onRetry != null) {
            OneTownSecondaryButton(
                text = stringResource(R.string.home_retry),
                onClick = onRetry,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}
