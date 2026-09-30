package com.onetowncity.app.feature.listings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.onetowncity.app.core.data.Listing
import com.onetowncity.app.core.data.ListingRepository
import com.onetowncity.app.core.data.ListingsResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ListingsUiState {
    data object Loading : ListingsUiState
    data object Empty : ListingsUiState
    data object Failed : ListingsUiState

    @Immutable
    data class Content(
        val items: List<Listing>,
        /** How many listings the server says exist in total (not just those loaded so far). */
        val total: Int,
        val hasMore: Boolean,
        val loadingMore: Boolean = false,
        val loadMoreFailed: Boolean = false,
    ) : ListingsUiState
}

/**
 * The listings of one category, loaded a page at a time. Which listings belong to the category is decided by the
 * server ([categoryKey] is sent as `category_key`); this class only pages and presents.
 */
class ListingsViewModel(
    private val repository: ListingRepository,
    private val modelKey: String,
    private val categoryKey: String?,
) : ViewModel() {
    private val _state = MutableStateFlow<ListingsUiState>(ListingsUiState.Loading)
    val state: StateFlow<ListingsUiState> = _state.asStateFlow()

    private var nextPage = 1
    private var job: Job? = null

    init {
        loadFirstPage()
    }

    fun retry() = loadFirstPage()

    /** Called when the user scrolls near the end, and by "Try again" after a failed page. */
    fun loadMore() {
        val current = _state.value as? ListingsUiState.Content ?: return
        if (!current.hasMore || current.loadingMore || job?.isActive == true) return
        // Build every later state from this one, not from `current`, which still carries the previous failure flag.
        val loading = current.copy(loadingMore = true, loadMoreFailed = false)
        _state.value = loading
        job = viewModelScope.launch {
            when (val result = repository.page(modelKey, categoryKey, nextPage)) {
                is ListingsResult.Success -> {
                    nextPage++
                    // A listing approved between two page loads can shift rows; never show the same one twice.
                    val seen = loading.items.mapTo(HashSet()) { it.id }
                    val added = result.page.items.filter { seen.add(it.id) }
                    _state.value = loading.copy(
                        items = loading.items + added,
                        total = result.page.total,
                        hasMore = result.page.hasNext,
                        loadingMore = false,
                    )
                }
                is ListingsResult.Failure -> _state.value = loading.copy(loadingMore = false, loadMoreFailed = true)
            }
        }
    }

    private fun loadFirstPage() {
        job?.cancel()
        nextPage = 1
        _state.value = ListingsUiState.Loading
        job = viewModelScope.launch {
            _state.value = when (val result = repository.page(modelKey, categoryKey, 1)) {
                is ListingsResult.Success -> {
                    nextPage = 2
                    val page = result.page
                    if (page.items.isEmpty() && !page.hasNext) ListingsUiState.Empty
                    else ListingsUiState.Content(page.items, page.total, page.hasNext)
                }
                is ListingsResult.Failure -> ListingsUiState.Failed
            }
        }
    }

    companion object {
        fun factory(repository: ListingRepository, modelKey: String, categoryKey: String?): ViewModelProvider.Factory =
            viewModelFactory { initializer { ListingsViewModel(repository, modelKey, categoryKey) } }
    }
}
