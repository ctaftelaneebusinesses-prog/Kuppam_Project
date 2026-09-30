package com.onetowncity.app.core.data

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.Serializable
import retrofit2.HttpException
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException

@Serializable
internal data class ListingPageDto(
    val count: Int = 0,
    val next: String? = null,
    val results: List<JsonObject> = emptyList(),
)

internal interface ListingApi {
    /** Public, read-only. `category_key` uses the website's own rule for what belongs in a category. */
    @GET("api/v1/listings/{model}/")
    suspend fun listings(
        @Path("model") model: String,
        @Query("category_key") categoryKey: String?,
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int = PAGE_SIZE,
    ): ListingPageDto

    companion object {
        const val PAGE_SIZE = 20
    }
}

internal class NetworkListingRepository(private val api: ListingApi) : ListingRepository {
    override suspend fun page(modelKey: String, categoryKey: String?, page: Int): ListingsResult = try {
        val dto = api.listings(modelKey, categoryKey, page)
        ListingsResult.Success(
            ListingPage(
                items = dto.results.mapNotNull { ListingMapper.map(it) },
                total = dto.count,
                hasNext = dto.next != null,
            ),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        ListingsResult.Failure(LoadFailure.Server)
    } catch (e: SerializationException) {
        ListingsResult.Failure(LoadFailure.Malformed)
    } catch (e: IllegalArgumentException) {
        ListingsResult.Failure(LoadFailure.Malformed)
    } catch (e: IOException) {
        ListingsResult.Failure(LoadFailure.Network)
    }
}
