package com.onetowncity.app.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items as lazyColumnItems
import androidx.compose.foundation.lazy.grid.items as lazyGridItems
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * The loading/error/empty/content states every browse screen re-implements
 * today via its own ad-hoc `when {}` (see the Phase 1 audit's "current
 * problems" #3) — [OneTownCityStatefulColumn]/[OneTownCityStatefulGrid] wrap
 * this once so a screen only supplies data + an item renderer.
 */
sealed class OneTownCityListState<out T> {
    object Loading : OneTownCityListState<Nothing>()
    data class Error(val message: String) : OneTownCityListState<Nothing>()
    data class Content<T>(val items: List<T>, val isLoadingMore: Boolean = false) : OneTownCityListState<T>()
}

private const val LOAD_MORE_THRESHOLD = 3

@Composable
fun <T> OneTownCityStatefulColumn(
    state: OneTownCityListState<T>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(OneTownCitySpacing.lg),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(OneTownCitySpacing.md),
    emptyTitle: String = "Nothing here yet",
    emptyMessage: String = "Check back soon.",
    onRetry: (() -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    itemKey: ((T) -> Any)? = null,
    itemContent: @Composable (T) -> Unit,
) {
    when (state) {
        is OneTownCityListState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                OneTownCityCircularLoading()
            }
        }
        is OneTownCityListState.Error -> {
            OneTownCityErrorState(message = state.message, onRetry = onRetry, modifier = modifier)
        }
        is OneTownCityListState.Content -> {
            if (state.items.isEmpty()) {
                OneTownCityEmptyState(title = emptyTitle, message = emptyMessage, modifier = modifier)
            } else {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    verticalArrangement = verticalArrangement,
                ) {
                    lazyColumnItems(state.items, key = itemKey) { item -> itemContent(item) }
                    if (state.isLoadingMore) {
                        item(key = "__loading_more__") {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = OneTownCitySpacing.lg), contentAlignment = Alignment.Center) {
                                OneTownCityCircularLoading()
                            }
                        }
                    }
                }
                if (onLoadMore != null) {
                    val shouldLoadMore by remember {
                        derivedStateOf {
                            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                            lastVisible >= state.items.size - LOAD_MORE_THRESHOLD
                        }
                    }
                    LaunchedEffect(shouldLoadMore, state.isLoadingMore) {
                        if (shouldLoadMore && !state.isLoadingMore) onLoadMore()
                    }
                }
            }
        }
    }
}

@Composable
fun <T> OneTownCityStatefulGrid(
    state: OneTownCityListState<T>,
    minItemWidth: Dp,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(OneTownCitySpacing.lg),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(OneTownCitySpacing.md),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(OneTownCitySpacing.md),
    emptyTitle: String = "Nothing here yet",
    emptyMessage: String = "Check back soon.",
    onRetry: (() -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    itemKey: ((T) -> Any)? = null,
    itemContent: @Composable (T) -> Unit,
) {
    when (state) {
        is OneTownCityListState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                OneTownCityCircularLoading()
            }
        }
        is OneTownCityListState.Error -> {
            OneTownCityErrorState(message = state.message, onRetry = onRetry, modifier = modifier)
        }
        is OneTownCityListState.Content -> {
            if (state.items.isEmpty()) {
                OneTownCityEmptyState(title = emptyTitle, message = emptyMessage, modifier = modifier)
            } else {
                val gridState = rememberLazyGridState()
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = minItemWidth),
                    state = gridState,
                    modifier = modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    horizontalArrangement = horizontalArrangement,
                    verticalArrangement = verticalArrangement,
                ) {
                    lazyGridItems(state.items, key = itemKey) { item -> itemContent(item) }
                }
                if (onLoadMore != null) {
                    val shouldLoadMore by remember {
                        derivedStateOf {
                            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                            lastVisible >= state.items.size - LOAD_MORE_THRESHOLD
                        }
                    }
                    LaunchedEffect(shouldLoadMore, state.isLoadingMore) {
                        if (shouldLoadMore && !state.isLoadingMore) onLoadMore()
                    }
                }
            }
        }
    }
}
