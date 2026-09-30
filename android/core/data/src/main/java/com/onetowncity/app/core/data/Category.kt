package com.onetowncity.app.core.data

/** A directory category as the app uses it. [iconKey] is the backend `icon` value (e.g. `bi-house-door`). */
data class Category(
    val id: Int,
    val key: String,
    val label: String,
    val iconKey: String,
    val order: Int,
    val parentId: Int?,
)

/** Why loading failed — the UI shows the same friendly retry for all of them, but tests and logs can tell them apart. */
enum class LoadFailure {
    /** No connection, DNS failure, timeout. */
    Network,

    /** The server answered with an error status. */
    Server,

    /** The server answered 200 but the body was not the expected JSON. */
    Malformed,
}

sealed interface CategoriesResult {
    data class Success(val categories: List<Category>) : CategoriesResult
    data class Failure(val reason: LoadFailure) : CategoriesResult
}
