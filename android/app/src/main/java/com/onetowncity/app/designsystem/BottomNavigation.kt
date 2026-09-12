package com.onetowncity.app.designsystem

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Measured
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp

data class OneTownCityBottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

// Stock NavigationBarItem applies Modifier.weight(1f) to every item (see
// androidx.compose.material3.NavigationBarKt.NavigationBarItem), which forces all items into an
// equal share of the bar's width no matter how much room a label actually needs. At the default
// font scale that equal share is wide enough for every OneTownCity tab label, but at larger
// accessibility font scales (~1.3x+) "Students" and "Profile" need more width than an equal
// fifth of the screen provides, so Compose's text layout wraps them mid-word to respect that
// width constraint. RowScope.weight is resolved via the receiver in scope at the call site, so
// invoking NavigationBarItem against this no-op RowScope neutralizes that forced equal division
// while leaving every other part of NavigationBarItem (icon, label, indicator, colors, ripple,
// semantics) untouched.
private val NaturalWidthRowScope: RowScope = object : RowScope {
    override fun Modifier.weight(weight: Float, fill: Boolean): Modifier = this
    override fun Modifier.align(alignment: Alignment.Vertical): Modifier = this
    override fun Modifier.alignBy(alignmentLine: HorizontalAlignmentLine): Modifier = this
    override fun Modifier.alignBy(alignmentLineBlock: (Measured) -> Int): Modifier = this
    override fun Modifier.alignByBaseline(): Modifier = this
}

// Extra width reserved beyond a label's own text width, as a safe upper bound on the padding
// NavigationBarItem places around its label -- so a natural-width item never ends up a hair too
// narrow for its own (differently-padded) real rendering.
private val AdaptiveNavLabelPadding = 24.dp

@Composable
fun OneTownCityBottomNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    items: List<OneTownCityBottomNavItem>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = NavigationBarDefaults.Elevation,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(NavigationBarDefaults.windowInsets),
        ) {
            val density = LocalDensity.current
            val textMeasurer = rememberTextMeasurer()
            val labelStyle = MaterialTheme.typography.labelMedium
            val viewportWidthPx = with(density) { maxWidth.roundToPx() }
            val labelPaddingPx = with(density) { AdaptiveNavLabelPadding.roundToPx() }

            // A lightweight standalone measurement of each label string (independent of
            // NavigationBarItem's own composition/layout) decides whether every item fits within
            // an equal share of the available width. Measuring the real NavigationBarItem
            // composables directly (e.g. via SubcomposeLayout intrinsics/probing) was tried and
            // discarded: NavigationBarItemLayout has no safe custom intrinsic-width measurement,
            // and a duplicate "probe" composition of the real item leaves stray, non-interactive
            // but still-semantics-visible nodes behind in the accessibility tree. Measuring just
            // the label text avoids touching NavigationBarItem internals entirely.
            val naturalWidths = remember(items, labelStyle, density) {
                items.map { item ->
                    textMeasurer.measure(text = item.label, style = labelStyle).size.width + labelPaddingPx
                }
            }
            val itemCount = items.size
            val equalShare = if (itemCount > 0 && viewportWidthPx > 0) viewportWidthPx / itemCount else 0
            val fitsEqually = viewportWidthPx > 0 && naturalWidths.all { it <= equalShare }

            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                items.forEachIndexed { index, item ->
                    val itemWidthPx = if (fitsEqually) equalShare else naturalWidths[index]
                    with(NaturalWidthRowScope) {
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { onTabSelected(index) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                )
                            },
                            label = { Text(item.label, style = labelStyle) },
                            alwaysShowLabel = true,
                            modifier = Modifier.width(with(density) { itemWidthPx.toDp() }),
                        )
                    }
                }
            }
        }
    }
}
