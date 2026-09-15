package com.onetowncity.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import coil.compose.AsyncImage
import com.onetowncity.app.cache.NetworkMonitor
import com.onetowncity.app.designsystem.OneTownCityButton
import com.onetowncity.app.designsystem.OneTownCityButtonVariant
import com.onetowncity.app.designsystem.OneTownCityCacheStatusBanner
import com.onetowncity.app.designsystem.OneTownCityCategoryIcons
import com.onetowncity.app.designsystem.OneTownCityCircularLoading
import com.onetowncity.app.designsystem.OneTownCityCornerRadii
import com.onetowncity.app.designsystem.OneTownCityElevation
import com.onetowncity.app.designsystem.OneTownCityEmptyState
import com.onetowncity.app.designsystem.OneTownCityErrorState
import com.onetowncity.app.designsystem.OneTownCityIcons
import com.onetowncity.app.designsystem.OneTownCityListingCard
import com.onetowncity.app.designsystem.OneTownCitySectionHeader
import com.onetowncity.app.designsystem.OneTownCitySpacing
import com.onetowncity.app.designsystem.OneTownCityTouchTarget
import java.net.URLEncoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Real Home tab (Phase 4 §2, redesigned Phase 6), replacing PlaceholderShellScreen.
 * Composed entirely from existing endpoints — there is no /home or /feed API
 * (confirmed absent from core/api/urls.py) — mirroring the sections
 * templates/home.html actually renders: a category grid (GET
 * /api/v1/categories/), "Today in Your Town" (events + village-happenings
 * news), Places to Visit (business category=tourism), and a Projects
 * preview. The web home() view also computes featured_businesses/
 * properties/jobs/news, but grep confirms none of those four are
 * referenced in home.html, and there's no visitor-count API either — so
 * neither is reproduced here rather than inventing numbers.
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
    var isOfflineNoCache by remember { mutableStateOf(false) }
    val isOnline by NetworkMonitor.isOnline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    fun load() {
        coroutineScope.launch {
            isLoading = true
            error = null
            isOfflineNoCache = false
            try {
                data = loadHomeData(city?.slug.orEmpty())
            } catch (e: CancellationException) {
                throw e
            } catch (e: OfflineNoCacheException) {
                isOfflineNoCache = true
            } catch (e: Exception) {
                error = e.message ?: "Unable to load your home feed right now."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(city?.slug) { load() }

    // "The UI must recover when connectivity returns" — once we're back
    // online, silently retry whatever previously failed for lack of a
    // connection (an offline-empty-state or a network error), no user
    // action required.
    LaunchedEffect(isOnline) {
        if (isOnline && (isOfflineNoCache || (error != null && data == null))) {
            load()
        }
    }

    when {
        isLoading && data == null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                OneTownCityCircularLoading(label = "Loading OneTownCity")
            }
        }
        isOfflineNoCache -> {
            OneTownCityEmptyState(
                title = "You're offline",
                message = "Home hasn't been loaded yet on this device. Connect to the internet once to load it — after that it'll be available offline too.",
                icon = Icons.Outlined.Home,
                action = {
                    OneTownCityButton(text = "Retry", onClick = { load() }, variant = OneTownCityButtonVariant.Outlined)
                },
            )
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
                Column(modifier = Modifier.fillMaxSize()) {
                    if (!isOnline) {
                        OneTownCityCacheStatusBanner(
                            message = "You're offline — showing saved results",
                            isOffline = true,
                            modifier = Modifier.padding(horizontal = OneTownCitySpacing.xl, vertical = OneTownCitySpacing.sm),
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        HomeContent(navController = navController, city = city, data = current)
                    }
                }
            }
        }
    }
}

/** Mirrors the bottom nav's own tab-switch semantics (single top, save/restore state) so tapping into Search or Students from Home behaves exactly like tapping the tab itself. */
private fun NavController.navigateToTabRoute(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
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
        verticalArrangement = Arrangement.spacedBy(OneTownCitySpacing.xxl),
        contentPadding = PaddingValues(horizontal = OneTownCitySpacing.xl, vertical = OneTownCitySpacing.lg),
    ) {
        // Search is a first-class capability, not buried in a tab — Home
        // leads with a tappable entry point straight into the real Search
        // experience rather than duplicating a second text field here.
        item {
            HomeSearchEntry(onClick = { navController.navigateToTabRoute("search") })
        }

        if (routableCategories.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(OneTownCitySpacing.md)) {
                    OneTownCitySectionHeader(title = "Browse")
                    HomeCategoryGrid(
                        categories = routableCategories,
                        onCategoryClick = { navigateForCategory(navController, it) },
                    )
                }
            }
        }

        if (data.today.isNotEmpty()) {
            item {
                HomeListingRow(
                    title = if (city != null) "Today in ${city.name}" else "Today in Your Town",
                    items = data.today,
                    cardWidth = 240.dp,
                )
            }
        }

        if (data.placesToVisit.isNotEmpty()) {
            item {
                HomeListingRow(
                    title = "Places to Visit",
                    items = data.placesToVisit,
                    onSeeAll = { navController.navigate("businesses?category=tourism") },
                )
            }
        }

        if (data.projects.isNotEmpty()) {
            item {
                HomeListingRow(
                    title = "Local Projects",
                    items = data.projects,
                    onSeeAll = { navController.navigate("projects") },
                )
            }
        }

        // Student resources are secondary to the core discovery content
        // above but real and always navigable (pure shortcuts, no network
        // call of their own) — "student resources where appropriate."
        item {
            HomeStudentResourcesSection(
                onCategoryClick = { key -> navController.navigate("students/$key") },
                onSeeAll = { navController.navigateToTabRoute("students") },
            )
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

/** A tappable, non-editable affordance styled like the real search field — tapping it opens the actual Search tab rather than duplicating a second live text input on Home. */
@Composable
private fun HomeSearchEntry(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(OneTownCityCornerRadii.xl),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = OneTownCityElevation.low,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = OneTownCityTouchTarget.minSize)
                .padding(horizontal = OneTownCitySpacing.lg, vertical = OneTownCitySpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OneTownCitySpacing.sm),
        ) {
            Icon(imageVector = OneTownCityIcons.search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "Search businesses, properties, events…",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Chunked manual grid instead of LazyVerticalGrid-with-a-guessed-height nested in a LazyColumn — sizes itself to real (possibly multi-line, unellipsized) label content instead of assuming a fixed row height. Category counts here are always small (routable listing models only), so this never needs to be lazy itself. */
@Composable
private fun HomeCategoryGrid(categories: List<CategoryOption>, onCategoryClick: (CategoryOption) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(OneTownCitySpacing.sm)) {
        categories.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(OneTownCitySpacing.sm),
            ) {
                row.forEach { category ->
                    HomeCategoryTile(
                        category = category,
                        onClick = { onCategoryClick(category) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HomeCategoryTile(category: CategoryOption, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(OneTownCityCornerRadii.md),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = OneTownCityElevation.low,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(OneTownCitySpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OneTownCitySpacing.sm),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = OneTownCityCategoryIcons.forModelKey(category.listingModel),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            // No maxLines/ellipsis — a category name wraps in full rather
            // than being truncated ("no ellipsis for important category
            // names when wrapping is possible").
            Text(
                text = category.label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HomeListingRow(
    title: String,
    items: List<ListingSummary>,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 200.dp,
    onSeeAll: (() -> Unit)? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(OneTownCitySpacing.md)) {
        OneTownCitySectionHeader(
            title = title,
            trailing = if (onSeeAll != null) {
                { OneTownCityButton(text = "See all", onClick = onSeeAll, variant = OneTownCityButtonVariant.Text) }
            } else null,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(OneTownCitySpacing.md)) {
            items(items, key = { "${it.modelKey}-${it.id}" }) { listing ->
                HomeListingCard(item = listing, width = cardWidth)
            }
        }
    }
}

@Composable
private fun HomeListingCard(item: ListingSummary, width: Dp) {
    val context = LocalContext.current
    val activity = context as? Activity
    OneTownCityListingCard(
        title = item.title,
        subtitle = item.subtitle.takeIf { it.isNotBlank() },
        icon = OneTownCityCategoryIcons.forModelKey(item.modelKey),
        imageContent = if (item.imageUrl.isNotBlank()) {
            {
                AsyncImage(
                    model = rememberOptimizedImageRequest(item.imageUrl),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        } else {
            null
        },
        onClick = {
            safeWebUri(API_BASE_URL + item.url)?.let { uri ->
                try {
                    activity?.startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (_: ActivityNotFoundException) { }
            }
        },
        modifier = Modifier.width(width),
    )
}

@Composable
private fun HomeStudentResourcesSection(onCategoryClick: (String) -> Unit, onSeeAll: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(OneTownCitySpacing.md)) {
        OneTownCitySectionHeader(
            title = "Student Resources",
            trailing = { OneTownCityButton(text = "See all", onClick = onSeeAll, variant = OneTownCityButtonVariant.Text) },
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(OneTownCitySpacing.sm)) {
            items(studentsCategories, key = { it.key }) { category ->
                HomeQuickLinkCard(
                    title = category.title,
                    icon = category.icon,
                    onClick = { onCategoryClick(category.key) },
                )
            }
        }
    }
}

@Composable
private fun HomeQuickLinkCard(title: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(112.dp),
        shape = RoundedCornerShape(OneTownCityCornerRadii.md),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = OneTownCityElevation.low,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(OneTownCitySpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OneTownCitySpacing.sm),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}
