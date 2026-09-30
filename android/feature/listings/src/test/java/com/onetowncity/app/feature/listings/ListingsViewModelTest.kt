package com.onetowncity.app.feature.listings

import com.onetowncity.app.core.data.Listing
import com.onetowncity.app.core.data.ListingPage
import com.onetowncity.app.core.data.ListingRepository
import com.onetowncity.app.core.data.ListingsResult
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ListingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun listing(id: Int) = Listing(id, "job", "s$id", "Job $id", null, null, null, false, "bi-briefcase", null)

    private class ScriptedRepository(private vararg val results: ListingsResult) : ListingRepository {
        val requests = mutableListOf<Triple<String, String?, Int>>()
        override suspend fun page(modelKey: String, categoryKey: String?, page: Int): ListingsResult {
            requests += Triple(modelKey, categoryKey, page)
            return results[minOf(requests.size - 1, results.lastIndex)]
        }
    }

    private fun page(vararg ids: Int, total: Int = ids.size, next: Boolean = false) =
        ListingsResult.Success(ListingPage(ids.map(::listing), total, next))

    private fun vm(repo: ScriptedRepository) = ListingsViewModel(repo, "job", "jobs-cat")

    @Test fun startsLoadingThenShowsTheFirstPageForTheCategory() = runTest(dispatcher) {
        val repo = ScriptedRepository(page(1, 2, total = 2))
        val model = vm(repo)
        assertEquals(ListingsUiState.Loading, model.state.value)
        advanceUntilIdle()
        val content = model.state.value as ListingsUiState.Content
        assertEquals(listOf(1, 2), content.items.map { it.id })
        assertEquals(listOf(Triple("job", "jobs-cat", 1)), repo.requests)
    }

    @Test fun noListingsAtAllIsEmptyNotAnError() = runTest(dispatcher) {
        val model = vm(ScriptedRepository(page(total = 0)))
        advanceUntilIdle()
        assertEquals(ListingsUiState.Empty, model.state.value)
    }

    @Test fun aFailureShowsFailedAndRetryRecovers() = runTest(dispatcher) {
        val repo = ScriptedRepository(ListingsResult.Failure(LoadFailure.Network), page(1))
        val model = vm(repo)
        advanceUntilIdle()
        assertEquals(ListingsUiState.Failed, model.state.value)
        model.retry()
        advanceUntilIdle()
        assertTrue(model.state.value is ListingsUiState.Content)
    }

    @Test fun loadMoreAppendsTheNextPageAndDropsDuplicates() = runTest(dispatcher) {
        val repo = ScriptedRepository(page(1, 2, total = 4, next = true), page(2, 3, 4, total = 4, next = false))
        val model = vm(repo)
        advanceUntilIdle()

        model.loadMore()
        advanceUntilIdle()

        val content = model.state.value as ListingsUiState.Content
        assertEquals(listOf(1, 2, 3, 4), content.items.map { it.id })
        assertFalse(content.hasMore)
        assertEquals(2, repo.requests.last().third)
    }

    @Test fun loadMoreDoesNothingOnTheLastPageOrWhileLoading() = runTest(dispatcher) {
        val repo = ScriptedRepository(page(1, total = 1, next = false))
        val model = vm(repo)
        advanceUntilIdle()
        model.loadMore()
        advanceUntilIdle()
        assertEquals(1, repo.requests.size)
    }

    @Test fun aFailedPageKeepsWhatIsLoadedAndCanBeRetried() = runTest(dispatcher) {
        val repo = ScriptedRepository(
            page(1, total = 2, next = true),
            ListingsResult.Failure(LoadFailure.Network),
            page(2, total = 2, next = false),
        )
        val model = vm(repo)
        advanceUntilIdle()

        model.loadMore(); advanceUntilIdle()
        val failed = model.state.value as ListingsUiState.Content
        assertTrue(failed.loadMoreFailed)
        assertEquals(listOf(1), failed.items.map { it.id })

        model.loadMore(); advanceUntilIdle()
        val recovered = model.state.value as ListingsUiState.Content
        assertEquals(listOf(1, 2), recovered.items.map { it.id })
        assertFalse(recovered.loadMoreFailed)
        assertEquals(listOf(1, 2, 2), repo.requests.map { it.third })
    }
}
