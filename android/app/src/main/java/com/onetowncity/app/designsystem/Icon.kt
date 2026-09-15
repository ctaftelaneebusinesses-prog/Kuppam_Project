package com.onetowncity.app.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.vector.ImageVector

object OneTownCityIcons {
    val home = Icons.Outlined.Home
    val homeFilled = Icons.Filled.Home
    val search = Icons.Outlined.Search
    val searchFilled = Icons.Filled.Search
    val favorite = Icons.Outlined.FavoriteBorder
    val location = Icons.Filled.LocationOn
    val profile = Icons.Outlined.Person
    val settings = Icons.Filled.Settings
    val error = Icons.Filled.Error
    val warning = Icons.Filled.Info
    val star = Icons.Filled.Star
    val check = Icons.Filled.Check
    val close = Icons.Filled.Close
    val back = Icons.AutoMirrored.Filled.ArrowBack
    val sad = Icons.Outlined.SentimentDissatisfied
}

/**
 * Meaningful, per-content-type iconography for listing categories — used by
 * OneTownCityListingCard's placeholder image and anywhere else a listing type
 * needs a glyph, instead of every category falling back to the same generic
 * icon (the redesign's "don't use the same generic icon for every category"
 * rule). Keyed by the backend's model_key (core/api/serializers.py).
 */
object OneTownCityCategoryIcons {
    fun forModelKey(modelKey: String): ImageVector = when (modelKey) {
        "business" -> Icons.Outlined.Store
        "property" -> Icons.Outlined.Home
        "job" -> Icons.Outlined.Work
        "event" -> Icons.Outlined.Event
        "news" -> Icons.AutoMirrored.Outlined.Article
        "project" -> Icons.Outlined.Business
        "scholarship" -> Icons.Outlined.EmojiEvents
        "lostfound" -> Icons.Outlined.Search
        "student_services" -> Icons.Outlined.School
        "tuition_center" -> Icons.Outlined.School
        "marketplace" -> Icons.Outlined.ShoppingCart
        else -> Icons.Outlined.Home
    }
}
