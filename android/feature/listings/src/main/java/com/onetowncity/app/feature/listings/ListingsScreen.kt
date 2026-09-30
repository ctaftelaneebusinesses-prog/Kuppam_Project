package com.onetowncity.app.feature.listings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onetowncity.app.core.data.Listing
import com.onetowncity.app.core.designsystem.categoryIconRes
import com.onetowncity.app.core.designsystem.R as DsR
import com.onetowncity.app.core.designsystem.components.DotMatrixHeader
import com.onetowncity.app.core.designsystem.components.DotMatrixText
import com.onetowncity.app.core.designsystem.components.OneTownIconButton
import com.onetowncity.app.core.designsystem.components.OneTownSecondaryButton
import com.onetowncity.app.core.designsystem.components.OneTownText
import com.onetowncity.app.core.designsystem.theme.OneTownTheme

private val ContentMaxWidth = 640.dp

/**
 * The listings of one category. [title] is the category's own label. Cards are only tappable when [onListingClick] is
 * given (the detail screen does not exist yet, so nothing pretends to be a button).
 */
@Composable
fun ListingsScreen(
    title: String,
    state: ListingsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    onListingClick: ((Listing) -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OneTownTheme.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.widthIn(max = ContentMaxWidth).fillMaxSize()) {
            Header(title, state, onBack)
            when (state) {
                ListingsUiState.Loading -> LoadingList()
                ListingsUiState.Empty -> Message(stringResource(R.string.listings_empty))
                ListingsUiState.Failed -> Message(stringResource(R.string.listings_error), onRetry)
                is ListingsUiState.Content -> ListingList(state, onLoadMore, onListingClick)
            }
        }
    }
}

@Composable
private fun Header(title: String, state: ListingsUiState, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 12.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OneTownIconButton(DsR.drawable.ic_bi_arrow_left, stringResource(R.string.listings_back), onBack)
            DotMatrixHeader(title, color = OneTownTheme.colors.textPrimary, modifier = Modifier.weight(1f))
        }
        if (state is ListingsUiState.Content) {
            OneTownText(
                text = pluralStringResource(R.plurals.listings_count, state.total, state.total),
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                style = OneTownTheme.typography.sansLabel,
                color = OneTownTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun ListingList(
    state: ListingsUiState.Content,
    onLoadMore: () -> Unit,
    onListingClick: ((Listing) -> Unit)?,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(state.items, key = { "${it.modelKey}-${it.id}" }) { listing ->
            ListingCard(listing, onListingClick?.let { click -> { click(listing) } })
        }
        if (state.hasMore) {
            item(key = "footer") {
                if (state.loadMoreFailed) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        OneTownText(
                            stringResource(R.string.listings_more_error),
                            style = OneTownTheme.typography.sansLabel,
                            color = OneTownTheme.colors.textSecondary,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                        OneTownSecondaryButton(stringResource(R.string.listings_retry), onLoadMore, Modifier.padding(top = 8.dp))
                    }
                } else {
                    // Composing this row means the user reached the end of what is loaded: fetch the next page.
                    LaunchedEffect(state.items.size) { onLoadMore() }
                    Placeholder(Modifier.fillMaxWidth().alpha(0.5f))
                }
            }
        }
    }
}

@Composable
private fun ListingCard(listing: Listing, onClick: (() -> Unit)?) {
    val colors = OneTownTheme.colors
    val shape = OneTownTheme.shapes.widget
    val base = Modifier
        .fillMaxWidth()
        .clip(shape)
        .background(colors.surface)
        .border(OneTownTheme.elevation.hairline, colors.outline, shape)
    Row(
        modifier = (if (onClick != null) base.clickable(role = Role.Button, onClick = onClick) else base)
            .semantics(mergeDescendants = true) {}
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(shape).border(OneTownTheme.elevation.hairline, colors.outline, shape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(categoryIconRes(listing.iconKey)),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(colors.textPrimary),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OneTownText(listing.title, style = OneTownTheme.typography.sansTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            listing.subtitle?.let {
                OneTownText(it, style = OneTownTheme.typography.sansBody, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            listing.highlight?.let { DotMatrixText(it, style = OneTownTheme.typography.dotMatrixValue) }
            if (listing.tag != null || listing.featured) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    if (listing.featured) Chip(stringResource(R.string.listings_featured))
                    listing.tag?.let { Chip(it) }
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String) {
    val shape = OneTownTheme.shapes.pill
    OneTownText(
        text = text,
        modifier = Modifier
            .border(OneTownTheme.elevation.hairline, OneTownTheme.colors.outlineStrong, shape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = OneTownTheme.typography.sansLabel,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun LoadingList() {
    val description = stringResource(R.string.listings_loading)
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 4.dp).semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(5) { Placeholder(Modifier.fillMaxWidth().alpha(0.5f)) }
    }
}

@Composable
private fun Placeholder(modifier: Modifier) {
    val shape = OneTownTheme.shapes.widget
    Box(
        modifier
            .heightIn(min = 96.dp)
            .clip(shape)
            .background(OneTownTheme.colors.surface)
            .border(OneTownTheme.elevation.hairline, OneTownTheme.colors.outline, shape),
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
            OneTownSecondaryButton(stringResource(R.string.listings_retry), onRetry, Modifier.padding(top = 16.dp))
        }
    }
}
