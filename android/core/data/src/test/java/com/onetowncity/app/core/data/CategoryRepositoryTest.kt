package com.onetowncity.app.core.data

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CategoryRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: CategoryRepository

    @Before fun setUp() {
        server = MockWebServer().apply { start() }
        repository = OneTownApi.categoryRepositoryForTest(server.url("/").toString())
    }

    @After fun tearDown() = server.shutdown()

    private fun json(body: String) = MockResponse().setHeader("Content-Type", "application/json").setBody(body)

    @Test fun keepsOnlyTopLevelCategoriesInServerOrder() = runTest {
        server.enqueue(json("""
            [
              {"id":24,"key":"bakery","label":"Bakery","parent":3,"listing_model":"business","business_subcategory":"bakery","icon":"bi-cup-straw","order":1},
              {"id":2,"key":"business","label":"Nearby Shops","parent":null,"listing_model":"business","business_subcategory":"","icon":"bi-shop","order":2},
              {"id":1,"key":"property","label":"Property Listing","parent":null,"listing_model":"property","business_subcategory":"","icon":"bi-house-door","order":1}
            ]
        """.trimIndent()))

        val result = repository.topLevelCategories()

        assertTrue(result is CategoriesResult.Success)
        val categories = (result as CategoriesResult.Success).categories
        assertEquals(listOf("property", "business"), categories.map { it.key })
        assertEquals("bi-house-door", categories.first().iconKey)
    }

    @Test fun requestsTheCategoriesEndpoint() = runTest {
        server.enqueue(json("[]"))
        repository.topLevelCategories()
        assertEquals("/api/v1/categories/", server.takeRequest().path)
    }

    @Test fun ignoresFieldsAddedByTheServerLater() = runTest {
        server.enqueue(json("""[{"id":1,"key":"a","label":"A","parent":null,"icon":"bi-tag","order":1,"brand_new_field":{"x":1}}]"""))
        assertTrue(repository.topLevelCategories() is CategoriesResult.Success)
    }

    @Test fun anEmptyListIsSuccessNotFailure() = runTest {
        server.enqueue(json("[]"))
        assertEquals(CategoriesResult.Success(emptyList()), repository.topLevelCategories())
    }

    @Test fun serverErrorIsReportedAsServerFailure() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        assertEquals(CategoriesResult.Failure(LoadFailure.Server), repository.topLevelCategories())
    }

    @Test fun invalidJsonIsMalformed() = runTest {
        server.enqueue(json("<html>maintenance</html>"))
        assertEquals(CategoriesResult.Failure(LoadFailure.Malformed), repository.topLevelCategories())
    }

    @Test fun wrongJsonShapeIsMalformed() = runTest {
        server.enqueue(json("""{"detail":"not a list"}"""))
        assertEquals(CategoriesResult.Failure(LoadFailure.Malformed), repository.topLevelCategories())
    }

    @Test fun droppedConnectionIsANetworkFailure() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        assertEquals(CategoriesResult.Failure(LoadFailure.Network), repository.topLevelCategories())
    }
}
