package com.onetowncity.app.core.data

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

interface CategoryRepository {
    /**
     * The top-level categories in the server's own order. Sub-categories (those with a parent) are filters inside a
     * category, not directory entries, so they are left out here. Never throws — failures come back as a result.
     */
    suspend fun topLevelCategories(): CategoriesResult
}

internal class NetworkCategoryRepository(private val api: CategoryApi) : CategoryRepository {
    override suspend fun topLevelCategories(): CategoriesResult = try {
        val categories = api.categories()
            .filter { it.parent == null }
            .sortedWith(compareBy({ it.order }, { it.label }))
            .map { it.toDomain() }
        CategoriesResult.Success(categories)
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        CategoriesResult.Failure(LoadFailure.Server)
    } catch (e: SerializationException) {
        CategoriesResult.Failure(LoadFailure.Malformed)
    } catch (e: IllegalArgumentException) {
        // kotlinx.serialization signals some bad input (e.g. a JSON object where an array is expected) this way.
        CategoriesResult.Failure(LoadFailure.Malformed)
    } catch (e: IOException) {
        CategoriesResult.Failure(LoadFailure.Network)
    }
}
