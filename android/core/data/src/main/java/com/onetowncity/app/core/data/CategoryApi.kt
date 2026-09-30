package com.onetowncity.app.core.data

import retrofit2.http.GET

internal interface CategoryApi {
    /** Public, read-only. Returns every active category (top-level and sub-categories) as a plain JSON array. */
    @GET("api/v1/categories/")
    suspend fun categories(): List<CategoryDto>
}
