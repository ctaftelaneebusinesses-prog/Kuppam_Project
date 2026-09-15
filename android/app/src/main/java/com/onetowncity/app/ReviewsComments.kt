package com.onetowncity.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.onetowncity.app.designsystem.OneTownCityButton
import com.onetowncity.app.designsystem.OneTownCityButtonVariant
import com.onetowncity.app.designsystem.OneTownCityTextField
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Reviews and comments are generic across every listing type — one API
 * (core/api/views.py's `reviews`/`comments`, GET+POST on
 * /api/v1/listings/<model_key>/<pk>/reviews|comments/) and one Django model
 * pair (Review/Comment, generic-FK'd to any listing) back Business, Property,
 * and Project alike, so one set of composables here covers all three detail
 * screens instead of one copy per listing type.
 */

internal data class ReviewAuthor(val name: String, val avatarUrl: String)

internal data class ReviewItem(
    val id: Int,
    val author: ReviewAuthor,
    val rating: Int,
    val body: String,
    val createdAt: String,
)

internal data class CommentItem(
    val id: Int,
    val author: ReviewAuthor,
    val body: String,
    val parentId: Int?,
    val createdAt: String,
    val replies: List<CommentItem>,
)

private fun parseAuthor(json: JSONObject?): ReviewAuthor = ReviewAuthor(
    name = json?.optString("full_name", "")?.ifBlank { "OneTownCity user" } ?: "OneTownCity user",
    avatarUrl = json?.optString("display_photo", "").orEmpty(),
)

private fun parseReview(json: JSONObject) = ReviewItem(
    id = json.optInt("id"),
    author = parseAuthor(json.optJSONObject("user")),
    rating = json.optInt("rating"),
    body = json.optString("body", ""),
    createdAt = json.optString("created_at", ""),
)

private fun parseComment(json: JSONObject): CommentItem {
    val repliesJson = json.optJSONArray("replies") ?: JSONArray()
    val replies = mutableListOf<CommentItem>()
    for (i in 0 until repliesJson.length()) {
        replies += parseComment(repliesJson.getJSONObject(i))
    }
    return CommentItem(
        id = json.optInt("id"),
        author = parseAuthor(json.optJSONObject("user")),
        body = json.optString("body", ""),
        parentId = if (json.isNull("parent_id")) null else json.optInt("parent_id"),
        createdAt = json.optString("created_at", ""),
        replies = replies,
    )
}

private fun reviewsUrl(modelKey: String, listingId: Int, page: Int) =
    "$API_BASE_URL/api/v1/listings/$modelKey/$listingId/reviews/?page=$page&page_size=10"

private fun commentsUrl(modelKey: String, listingId: Int, page: Int) =
    "$API_BASE_URL/api/v1/listings/$modelKey/$listingId/comments/?page=$page&page_size=20"

internal suspend fun fetchReviews(modelKey: String, listingId: Int, page: Int): ApiListPage<ReviewItem> =
    fetchListPage(reviewsUrl(modelKey, listingId, page), page) { parseReview(it) }

internal suspend fun fetchComments(modelKey: String, listingId: Int, page: Int): ApiListPage<CommentItem> =
    fetchListPage(commentsUrl(modelKey, listingId, page), page) { parseComment(it) }

/** POST — rating 1-5, upserts server-side if the caller already reviewed this listing (one review per user per listing). */
internal suspend fun submitReview(modelKey: String, listingId: Int, rating: Int, body: String): ReviewItem {
    val payload = JSONObject().apply {
        put("rating", rating)
        if (body.isNotBlank()) put("body", body)
    }
    return parseReview(
        httpJson(
            "$API_BASE_URL/api/v1/listings/$modelKey/$listingId/reviews/",
            method = "POST",
            jsonBody = payload.toString(),
            requiresAuth = true,
        ),
    )
}

/** POST — parentId != null posts a reply (single level of threading, matching Comment.parent). */
internal suspend fun submitComment(modelKey: String, listingId: Int, body: String, parentId: Int? = null): CommentItem {
    val payload = JSONObject().apply {
        put("body", body)
        if (parentId != null) put("parent_id", parentId)
    }
    return parseComment(
        httpJson(
            "$API_BASE_URL/api/v1/listings/$modelKey/$listingId/comments/",
            method = "POST",
            jsonBody = payload.toString(),
            requiresAuth = true,
        ),
    )
}

@Composable
private fun StarRatingInput(rating: Int, onRatingChange: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (star in 1..5) {
            Icon(
                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = "$star star${if (star == 1) "" else "s"}",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp).clickable { onRatingChange(star) },
            )
        }
    }
}

@Composable
private fun StarRatingDisplay(rating: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (star in 1..5) {
            Icon(
                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
internal fun ReviewsSection(
    modelKey: String,
    listingId: Int,
    avgRating: Double,
    reviewCount: Int,
    navController: NavController,
) {
    var reviews by remember { mutableStateOf<List<ReviewItem>>(emptyList()) }
    var page by remember { mutableStateOf(1) }
    var hasMore by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var draftRating by remember { mutableStateOf(0) }
    var draftBody by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var requiresSignIn by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    suspend fun load(reset: Boolean) {
        if (reset) {
            isLoading = true
            error = null
        }
        try {
            val target = if (reset) 1 else page
            val result = fetchReviews(modelKey, listingId, target)
            reviews = if (reset) result.items else reviews + result.items
            hasMore = result.nextPage != null
            page = result.nextPage ?: target
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Unable to load reviews right now."
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(modelKey, listingId) { load(reset = true) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Reviews", style = MaterialTheme.typography.titleMedium)
            if (reviewCount > 0) {
                StarRatingDisplay(rating = Math.round(avgRating).toInt())
                Text(
                    text = "%.1f (%d)".format(avgRating, reviewCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        when {
            requiresSignIn -> SignInRequiredState(message = "Sign in to write a review.", navController = navController)
            else -> {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Write a review", style = MaterialTheme.typography.titleSmall)
                        StarRatingInput(rating = draftRating, onRatingChange = { draftRating = it })
                        OneTownCityTextField(
                            value = draftBody,
                            onValueChange = { draftBody = it },
                            placeholder = "Share your experience (optional)",
                            singleLine = false,
                        )
                        if (submitError != null) {
                            Text(submitError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        OneTownCityButton(
                            text = if (isSubmitting) "Submitting…" else "Submit review",
                            enabled = draftRating in 1..5 && !isSubmitting,
                            onClick = {
                                coroutineScope.launch {
                                    isSubmitting = true
                                    submitError = null
                                    try {
                                        submitReview(modelKey, listingId, draftRating, draftBody)
                                        draftRating = 0
                                        draftBody = ""
                                        load(reset = true)
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: AuthRequiredException) {
                                        requiresSignIn = true
                                    } catch (e: Exception) {
                                        submitError = e.message ?: "Unable to submit your review right now."
                                    } finally {
                                        isSubmitting = false
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }

        when {
            isLoading -> Text("Loading reviews…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            error != null -> {
                Text(error.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                OneTownCityButton(text = "Retry", onClick = { coroutineScope.launch { load(reset = true) } }, variant = OneTownCityButtonVariant.Text)
            }
            reviews.isEmpty() -> Text("No reviews yet. Be the first to share your experience.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    reviews.forEach { review ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(review.author.name, style = MaterialTheme.typography.titleSmall)
                                StarRatingDisplay(rating = review.rating)
                            }
                            if (review.body.isNotBlank()) {
                                Text(review.body, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Divider()
                    }
                    if (hasMore) {
                        OneTownCityButton(text = "Load more reviews", onClick = { coroutineScope.launch { load(reset = false) } }, variant = OneTownCityButtonVariant.Text)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentRow(
    comment: CommentItem,
    isReply: Boolean,
    replyTargetId: Int?,
    replyBody: String,
    onReplyBodyChange: (String) -> Unit,
    onStartReply: (Int) -> Unit,
    onCancelReply: () -> Unit,
    onSubmitReply: (Int) -> Unit,
    isSubmittingReply: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = if (isReply) 24.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(comment.author.name, style = MaterialTheme.typography.titleSmall)
        Text(comment.body, style = MaterialTheme.typography.bodyMedium)
        if (!isReply) {
            OneTownCityButton(
                text = "Reply",
                onClick = { if (replyTargetId == comment.id) onCancelReply() else onStartReply(comment.id) },
                variant = OneTownCityButtonVariant.Text,
            )
        }
        if (replyTargetId == comment.id) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OneTownCityTextField(
                    value = replyBody,
                    onValueChange = onReplyBodyChange,
                    placeholder = "Write a reply",
                    singleLine = false,
                )
                OneTownCityButton(
                    text = if (isSubmittingReply) "Posting…" else "Post reply",
                    enabled = replyBody.isNotBlank() && !isSubmittingReply,
                    onClick = { onSubmitReply(comment.id) },
                )
            }
        }
        comment.replies.forEach { reply ->
            CommentRow(
                comment = reply,
                isReply = true,
                replyTargetId = replyTargetId,
                replyBody = replyBody,
                onReplyBodyChange = onReplyBodyChange,
                onStartReply = onStartReply,
                onCancelReply = onCancelReply,
                onSubmitReply = onSubmitReply,
                isSubmittingReply = isSubmittingReply,
            )
        }
    }
}

@Composable
internal fun CommentsSection(
    modelKey: String,
    listingId: Int,
    commentCount: Int,
    navController: NavController,
) {
    var comments by remember { mutableStateOf<List<CommentItem>>(emptyList()) }
    var page by remember { mutableStateOf(1) }
    var hasMore by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var draftBody by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var requiresSignIn by remember { mutableStateOf(false) }
    var replyTargetId by remember { mutableStateOf<Int?>(null) }
    var replyBody by remember { mutableStateOf("") }
    var isSubmittingReply by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    suspend fun load(reset: Boolean) {
        if (reset) {
            isLoading = true
            error = null
        }
        try {
            val target = if (reset) 1 else page
            val result = fetchComments(modelKey, listingId, target)
            comments = if (reset) result.items else comments + result.items
            hasMore = result.nextPage != null
            page = result.nextPage ?: target
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Unable to load comments right now."
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(modelKey, listingId) { load(reset = true) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text("Comments${if (commentCount > 0) " ($commentCount)" else ""}", style = MaterialTheme.typography.titleMedium)

        if (requiresSignIn) {
            SignInRequiredState(message = "Sign in to post a comment.", navController = navController)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OneTownCityTextField(
                    value = draftBody,
                    onValueChange = { draftBody = it },
                    placeholder = "Add a comment",
                    singleLine = false,
                )
                if (submitError != null) {
                    Text(submitError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                OneTownCityButton(
                    text = if (isSubmitting) "Posting…" else "Post comment",
                    enabled = draftBody.isNotBlank() && !isSubmitting,
                    onClick = {
                        coroutineScope.launch {
                            isSubmitting = true
                            submitError = null
                            try {
                                submitComment(modelKey, listingId, draftBody)
                                draftBody = ""
                                load(reset = true)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: AuthRequiredException) {
                                requiresSignIn = true
                            } catch (e: Exception) {
                                submitError = e.message ?: "Unable to post your comment right now."
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                )
            }
        }

        when {
            isLoading -> Text("Loading comments…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            error != null -> {
                Text(error.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                OneTownCityButton(text = "Retry", onClick = { coroutineScope.launch { load(reset = true) } }, variant = OneTownCityButtonVariant.Text)
            }
            comments.isEmpty() -> Text("No comments yet. Start the conversation.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    comments.forEach { comment ->
                        CommentRow(
                            comment = comment,
                            isReply = false,
                            replyTargetId = replyTargetId,
                            replyBody = replyBody,
                            onReplyBodyChange = { replyBody = it },
                            onStartReply = { replyTargetId = it; replyBody = "" },
                            onCancelReply = { replyTargetId = null; replyBody = "" },
                            onSubmitReply = { parentId ->
                                coroutineScope.launch {
                                    isSubmittingReply = true
                                    try {
                                        submitComment(modelKey, listingId, replyBody, parentId)
                                        replyTargetId = null
                                        replyBody = ""
                                        load(reset = true)
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: AuthRequiredException) {
                                        requiresSignIn = true
                                    } catch (e: Exception) {
                                        submitError = e.message ?: "Unable to post your reply right now."
                                    } finally {
                                        isSubmittingReply = false
                                    }
                                }
                            },
                            isSubmittingReply = isSubmittingReply,
                        )
                        Divider()
                    }
                    if (hasMore) {
                        OneTownCityButton(text = "Load more comments", onClick = { coroutineScope.launch { load(reset = false) } }, variant = OneTownCityButtonVariant.Text)
                    }
                }
            }
        }
    }
}
