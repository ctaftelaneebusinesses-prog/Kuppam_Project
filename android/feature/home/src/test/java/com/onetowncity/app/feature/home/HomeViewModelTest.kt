package com.onetowncity.app.feature.home

import com.onetowncity.app.core.data.CategoriesResult
import com.onetowncity.app.core.data.Category
import com.onetowncity.app.core.data.CategoryRepository
import com.onetowncity.app.core.data.LoadFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun category(i: Int) = Category(i, "key$i", "Label $i", "bi-tag", i, null)

    private class FakeRepository(private vararg val results: CategoriesResult) : CategoryRepository {
        var calls = 0
        override suspend fun topLevelCategories() = results[minOf(calls++, results.lastIndex)]
    }

    @Test fun startsLoadingThenShowsContentWithFirstTwoTilesFocal() = runTest(dispatcher) {
        val vm = HomeViewModel(FakeRepository(CategoriesResult.Success((1..4).map(::category))))
        assertEquals(HomeUiState.Loading, vm.state.value)

        advanceUntilIdle()

        val content = vm.state.value as HomeUiState.Content
        assertEquals(listOf("key1", "key2", "key3", "key4"), content.tiles.map { it.key })
        assertEquals(
            listOf(TileSpan.Focal, TileSpan.Focal, TileSpan.Compact, TileSpan.Compact),
            content.tiles.map { it.span },
        )
        assertTrue(content.tiles.all { it.count == null })
    }

    @Test fun emptyListShowsEmptyState() = runTest(dispatcher) {
        val vm = HomeViewModel(FakeRepository(CategoriesResult.Success(emptyList())))
        advanceUntilIdle()
        assertEquals(HomeUiState.Empty, vm.state.value)
    }

    @Test fun failureShowsFailedAndRetryRecovers() = runTest(dispatcher) {
        val repository = FakeRepository(
            CategoriesResult.Failure(LoadFailure.Network),
            CategoriesResult.Success(listOf(category(1))),
        )
        val vm = HomeViewModel(repository)
        advanceUntilIdle()
        assertEquals(HomeUiState.Failed, vm.state.value)

        vm.retry()
        assertEquals(HomeUiState.Loading, vm.state.value)
        advanceUntilIdle()

        assertTrue(vm.state.value is HomeUiState.Content)
        assertEquals(2, repository.calls)
    }
}
