package com.onetowncity.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onetowncity.app.cache.NetworkMonitor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

internal data class ListingsUiState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val isShowingCachedData: Boolean = false,
    val isOfflineNoCache: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = false,
    val page: Int = 1,
)

/**
 * Generic UI -> ViewModel -> Repository layer shared by the Business/
 * Property/Project browse screens (see BusinessBrowseScreen etc.) — one
 * class instead of three hand-rolled, near-identical LaunchedEffect blocks,
 * satisfying "do not allow Compose screens to directly manage network
 * calls" from the offline-first spec.
 *
 * Implements real stale-while-revalidate: [load] shows whatever's already
 * in the offline cache for the current request instantly (no spinner) via
 * [peekCachedListPage], while a live [fetchPage] call runs in the
 * background and silently replaces the data once it lands. That's a
 * genuine improvement over the plain httpJson layer every other screen
 * still uses, which only falls back to cache when a live request fails —
 * it never shows cached content up front while quietly refreshing.
 */
internal class ListingsViewModel<T>(
    private val buildUrl: (query: String, categoryKey: String, citySlug: String, page: Int) -> String,
    private val fetchPage: suspend (query: String, categoryKey: String, citySlug: String, page: Int) -> ApiListPage<T>,
    private val parseItem: (JSONObject) -> T,
) : ViewModel() {
    private val _state = MutableStateFlow(ListingsUiState<T>())
    val state: StateFlow<ListingsUiState<T>> = _state.asStateFlow()

    private var loadJob: Job? = null

    fun load(query: String, categoryKey: String, citySlug: String, reset: Boolean = true) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val pageToLoad: Int
            if (reset) {
                _state.update { ListingsUiState(isLoading = true) }
                val cachedPage = peekCachedListPage(buildUrl(query, categoryKey, citySlug, 1), parseItem)
                if (cachedPage != null) {
                    _state.update {
                        it.copy(
                            items = cachedPage.items,
                            hasMore = cachedPage.nextPage != null,
                            page = cachedPage.nextPage ?: 2,
                            isLoading = false,
                            isShowingCachedData = true,
                            isRefreshing = NetworkMonitor.isOnlineNow(),
                        )
                    }
                }
                pageToLoad = 1
                if (cachedPage != null && !NetworkMonitor.isOnlineNow()) {
                    // Offline with a cached result already shown — no
                    // network attempt to make; httpJson would only rethrow
                    // the same cached data anyway, so skip straight to done.
                    return@launch
                }
            } else {
                if (!_state.value.hasMore || _state.value.isLoadingMore) return@launch
                pageToLoad = _state.value.page
                _state.update { it.copy(isLoadingMore = true) }
            }

            try {
                val result = fetchPage(query, categoryKey, citySlug, pageToLoad)
                _state.update { current ->
                    current.copy(
                        items = if (reset) result.items else current.items + result.items,
                        hasMore = result.nextPage != null,
                        page = result.nextPage ?: (pageToLoad + 1),
                        isLoading = false,
                        isLoadingMore = false,
                        isRefreshing = false,
                        isShowingCachedData = !NetworkMonitor.isOnlineNow(),
                        isOfflineNoCache = false,
                        error = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: OfflineNoCacheException) {
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        isRefreshing = false,
                        isOfflineNoCache = current.items.isEmpty(),
                    )
                }
            } catch (e: Exception) {
                _state.update { current ->
                    if (reset && current.items.isEmpty()) {
                        current.copy(isLoading = false, isLoadingMore = false, isRefreshing = false, error = e.message ?: "Unable to load right now.")
                    } else {
                        // Cached items are already on screen (from the peek
                        // above, or from a prior successful page) — a failed
                        // background refresh or "load more" must never blank
                        // the screen or show a hard error over real content.
                        current.copy(isLoading = false, isLoadingMore = false, isRefreshing = false)
                    }
                }
            }
        }
    }
}
