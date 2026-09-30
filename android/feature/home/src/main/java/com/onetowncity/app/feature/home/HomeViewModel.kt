package com.onetowncity.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.onetowncity.app.core.data.CategoriesResult
import com.onetowncity.app.core.data.Category
import com.onetowncity.app.core.data.CategoryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Loads the categories once on creation and again on [retry]. It only presents what the backend returns — no
 * categories, counts or ordering rules are invented here.
 */
class HomeViewModel(private val repository: CategoryRepository) : ViewModel() {
    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var loading: Job? = null

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        if (loading?.isActive == true) return
        _state.value = HomeUiState.Loading
        loading = viewModelScope.launch {
            _state.value = when (val result = repository.topLevelCategories()) {
                is CategoriesResult.Success ->
                    if (result.categories.isEmpty()) HomeUiState.Empty
                    else HomeUiState.Content(result.categories.mapIndexed(::toTile))
                is CategoriesResult.Failure -> HomeUiState.Failed
            }
        }
    }

    private fun toTile(index: Int, category: Category) = CategoryTile(
        key = category.key,
        label = category.label,
        iconKey = category.iconKey,
        span = spanForPosition(index),
        listingModel = category.listingModel,
    )

    companion object {
        fun factory(repository: CategoryRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(repository) }
        }
    }
}
