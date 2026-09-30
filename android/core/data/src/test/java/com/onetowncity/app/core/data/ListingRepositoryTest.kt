package com.onetowncity.app.core.data

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ListingRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: ListingRepository

    @Before fun setUp() {
        server = MockWebServer().apply { start() }
        repository = OneTownApi.listingRepositoryForTest(server.url("/").toString())
    }

    @After fun tearDown() = server.shutdown()

    private fun json(body: String, code: Int = 200) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    private val twoJobs = """
        {"count":22,"next":"https://x/api/v1/listings/job/?page=2","previous":null,"page_size":20,"results":[
          {"id":1,"model_key":"job","slug":"a","job_title":"Driver","company":"Acme","location":"Kuppam","salary":"₹10,000"},
          {"id":2,"model_key":"job","slug":"b","job_title":"Cook","company":"Cafe","location":"Kuppam"}
        ]}
    """.trimIndent()

    @Test fun requestsTheModelPageAndCategoryKey() = runTest {
        server.enqueue(json(twoJobs))
        repository.page("job", "jobs-full-time", 2)
        val path = server.takeRequest().path!!
        assertTrue(path, path.startsWith("/api/v1/listings/job/?"))
        assertTrue(path, "category_key=jobs-full-time" in path && "page=2" in path && "page_size=20" in path)
    }

    @Test fun omitsCategoryKeyWhenThereIsNone() = runTest {
        server.enqueue(json(twoJobs))
        repository.page("job", null, 1)
        assertFalse(server.takeRequest().path!!.contains("category_key"))
    }

    @Test fun mapsItemsAndReadsPagingFromTheEnvelope() = runTest {
        server.enqueue(json(twoJobs))
        val page = (repository.page("job", null, 1) as ListingsResult.Success).page
        assertEquals(listOf("Driver", "Cook"), page.items.map { it.title })
        assertEquals(22, page.total)
        assertTrue(page.hasNext)
    }

    @Test fun theLastPageHasNoNext() = runTest {
        server.enqueue(json("""{"count":1,"next":null,"previous":null,"results":[{"id":1,"model_key":"job","slug":"a","job_title":"Driver"}]}"""))
        assertFalse((repository.page("job", null, 1) as ListingsResult.Success).page.hasNext)
    }

    @Test fun anEmptyCategoryIsSuccessWithNoItems() = runTest {
        server.enqueue(json("""{"count":0,"next":null,"previous":null,"results":[]}"""))
        val page = (repository.page("project", "x", 1) as ListingsResult.Success).page
        assertTrue(page.items.isEmpty())
        assertEquals(0, page.total)
    }

    @Test fun brokenRowsAreSkippedButGoodOnesStay() = runTest {
        server.enqueue(json("""{"count":2,"next":null,"results":[{"model_key":"job"},{"id":2,"model_key":"job","slug":"b","job_title":"Cook"}]}"""))
        assertEquals(listOf("Cook"), (repository.page("job", null, 1) as ListingsResult.Success).page.items.map { it.title })
    }

    @Test fun failuresAreReportedNotThrown() = runTest {
        server.enqueue(json("{}", code = 404))
        assertEquals(ListingsResult.Failure(LoadFailure.Server), repository.page("job", "nope", 1))
        server.enqueue(json("<html>maintenance</html>"))
        assertEquals(ListingsResult.Failure(LoadFailure.Malformed), repository.page("job", null, 1))
    }

    @Test fun aDroppedConnectionIsANetworkFailure() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        assertEquals(ListingsResult.Failure(LoadFailure.Network), repository.page("job", null, 1))
    }
}
