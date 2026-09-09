package com.onetowncity.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.onetowncity.app.designsystem.OneTownCityButton
import com.onetowncity.app.designsystem.OneTownCityButtonVariant
import com.onetowncity.app.designsystem.OneTownCityCircularLoading
import com.onetowncity.app.designsystem.OneTownCityEmptyState
import com.onetowncity.app.designsystem.OneTownCityErrorState
import com.onetowncity.app.designsystem.OneTownCitySectionHeader
import java.net.URLEncoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Real Home tab (Phase 4 §2), replacing PlaceholderShellScreen. Composed
 * entirely from existing endpoints — there is no /home or /feed API (confirmed
 * absent from core/api/urls.py) — mirroring the sections templates/home.html
 * actually renders: a category grid (GET /api/v1/categories/), "Today in
 * Your Town" (events + village-happenings news), Places to Visit (business
 * category=tourism), and a Projects preview. The web home() view also
 * computes featured_businesses/properties/jobs/news, but grep confirms none
 * of those four are referenced in home.html, and there's no visitor-count API
 * either — so neither is reproduced here rather than inventing numbers.
 */

private data class HomeData(
    val categories: List<CategoryOption>,
    val today: List<ListingSummary>,
    val placesToVisit: List<ListingSummary>,
    val projects: List<ListingSummary>,
)

private suspend fun loadHomeData(citySlug: String): HomeData = coroutineScope {
    val citySuffix = if (citySlug.isNotBlank()) "&city=" + URLEncoder.encode(citySlug, "UTF-8") else ""

    val categoriesDeferred = async { fetchCategories() }
    val eventsDeferred = async {
        runCatching {
            fetchListPage("$API_BASE_URL/api/v1/listings/event/?upcoming=true&page_size=5$citySuffix", 1) { parseListingSummary(it) }.items
        }.getOrDefault(emptyList())
    }
    val newsDeferred = async {
        runCatching {
            fetchListPage("$API_BASE_URL/api/v1/listings/news/?category=village-happenings&ordering=-created_at&page_size=5$citySuffix", 1) { parseListingSummary(it) }.items
        }.getOrDefault(emptyList())
    }
    val placesDeferred = async {
        runCatching {
            fetchListPage(buildBusinessCategoryUrl("tourism", "", citySlug, 1, pageSize = 6), 1) { parseListingSummary(it) }.items
        }.getOrDefault(emptyList())
    }
    val projectsDeferred = async {
        runCatching {
            fetchListPage("$API_BASE_URL/api/v1/listings/project/?ordering=-created_at&page_size=5$citySuffix", 1) { parseListingSummary(it) }.items
        }.getOrDefault(emptyList())
    }

    // Categories are the navigational backbone of Home — if this specific
    // fetch fails, the caller treats the whole screen as failed (full-page
    // retry). Empty/failed supplementary sections (today/places/projects)
    // just degrade to "section not shown" rather than blocking the screen.
    HomeData(
        categories = categoriesDeferred.await(),
        today = (eventsDeferred.await() + newsDeferred.await()).take(6),
        placesToVisit = placesDeferred.await(),
        projects = projectsDeferred.await(),
    )
}

@Composable
internal fun HomeScreen(navController: NavController, city: CitySelection?) {
    var data by remember { mutableStateOf<HomeData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun load() {
        coroutineScope.launch {
            isLoading = true
            error = null
            try {
                data = loadHomeData(city?.slug.orEmpty())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "Unable to load your home feed right now."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(city?.slug) { load() }

    when {
        isLoading && data == null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                OneTownCityCircularLoading(label = "Loading OneTownCity")
            }
        }
        error != null && data == null -> {
            OneTownCityErrorState(
                title = "Unable to load Home",
                message = error ?: "Please try again later.",
                actionText = "Retry",
                onRetry = { load() },
            )
        }
        data != null -> {
            val current = data!!
            val isEmpty = current.categories.isEmpty() && current.today.isEmpty() && current.placesToVisit.isEmpty() && current.projects.isEmpty()
            if (isEmpty) {
                OneTownCityEmptyState(
                    title = "Nothing to show yet",
                    message = if (city != null) "No listings found for ${city.name} yet." else "Select a city to see what's happening nearby.",
                    icon = Icons.Outlined.Home,
                    action = {
                        OneTownCityButton(text = "Refresh", onClick = { load() }, variant = OneTownCityButtonVariant.Outlined)
                    },
                )
            } else {
                HomeContent(navController = navController, city = city, data = current)
            }
        }
    }
}

@Composable
private fun HomeContent(navController: NavController, city: CitySelection?, data: HomeData) {
    // Only categories with a real native (or existing) destination are
    // shown — job/news have no Android browse/detail screen in this phase
    // ("Do not implement unrelated features"), so surfacing them as tappable
    // would be a dead end.
    val routableCategories = data.categories.filter {
        it.listingModel in setOf("business", "property", "project", "event", "scholarship", "lostfound")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    ) {
        if (routableCategories.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OneTownCitySectionHeader(title = "Browse")
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth().height((((routableCategories.size + 2) / 3) * 96).dp),
                    ) {
                        items(routableCategories, key = { it.key }) { category ->
                            HomeCategoryTile(category = category, onClick = { navigateForCategory(navController, category) })
                        }
                    }
                }
            }
        }

        if (data.today.isNotEmpty()) {
            item {
                HomePreviewRow(
                    title = if (city != null) "Today in ${city.name}" else "Today in Your Town",
                    items = data.today,
                    icon = Icons.Outlined.Event,
                )
            }
        }

        if (data.placesToVisit.isNotEmpty()) {
            item {
                HomePreviewRow(
                    title = "Places to Visit",
                    items = data.placesToVisit,
                    icon = Icons.Outlined.Search,
                    onSeeAll = { navController.navigate("businesses?category=tourism") },
                )
            }
        }

        if (data.projects.isNotEmpty()) {
            item {
                HomePreviewRow(
                    title = "Local Projects",
                    items = data.projects,
                    icon = Icons.Outlined.Business,
                    onSeeAll = { navController.navigate("projects") },
                )
            }
        }
    }
}

private fun navigateForCategory(navController: NavController, category: CategoryOption) {
    when (category.listingModel) {
        "business" -> navController.navigate("businesses?category=${category.key}")
        "property" -> navController.navigate("properties?category=${category.key}")
        "project" -> navController.navigate("projects?category=${category.key}")
        "event" -> navController.navigate("students/events")
        "scholarship" -> navController.navigate("students/scholarships")
        "lostfound" -> navController.navigate("students/lost-found")
    }
}

@Composable
private fun HomeCategoryTile(category: CategoryOption, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Outlined.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            Text(
                text = category.label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomePreviewRow(
    title: String,
    items: List<ListingSummary>,
    icon: ImageVector,
    onSeeAll: (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OneTownCitySectionHeader(
            title = title,
            trailing = if (onSeeAll != null) {
                { OneTownCityButton(text = "See all", onClick = onSeeAll, variant = OneTownCityButtonVariant.Text) }
            } else null,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items, key = { "${it.modelKey}-${it.id}" }) { listing ->
                HomePreviewCard(item = listing, icon = icon)
            }
        }
    }
}

@Composable
private fun HomePreviewCard(item: ListingSummary, icon: ImageVector) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.width(200.dp).clickable {
            safeWebUri(API_BASE_URL + item.url)?.let { uri ->
                try {
                    activity?.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                } catch (_: android.content.ActivityNotFoundException) { }
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (item.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = rememberOptimizedImageRequest(item.imageUrl),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                }
            }
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = item.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (item.subtitle.isNotBlank()) {
                    Text(text = item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
