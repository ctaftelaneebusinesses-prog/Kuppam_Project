package com.onetowncity.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Build
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.onetowncity.app.designsystem.OneTownCityButton
import com.onetowncity.app.designsystem.OneTownCityButtonVariant
import com.onetowncity.app.designsystem.OneTownCityCacheStatusBanner
import com.onetowncity.app.designsystem.OneTownCityChipGroup
import com.onetowncity.app.designsystem.OneTownCityCircularLoading
import com.onetowncity.app.designsystem.OneTownCityEmptyState
import com.onetowncity.app.designsystem.OneTownCityErrorState
import com.onetowncity.app.designsystem.OneTownCityTextField
import com.onetowncity.app.designsystem.OneTownCityTopAppBar
import java.net.URLEncoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject

/** Real Project browse/detail (Phase 4 §5), using the existing generic listing API (model_key = "project"). */
private val projectStatusLabelByKey = mapOf(
    "planned" to "Planned",
    "ongoing" to "Ongoing",
    "completed" to "Completed",
)

internal data class ProjectItem(
    val id: Int,
    val title: String,
    val statusLabel: String,
    val location: String,
    val expectedCompletion: String,
    val department: String,
    val description: String,
    val mapsLink: String,
    val displayImage: String,
    val cityName: String,
    val avgRating: Double,
    val reviewCount: Int,
    val commentCount: Int,
)

internal val projectCache = BoundedItemCache<Int, ProjectItem>(200)

private fun parseProject(json: JSONObject): ProjectItem {
    val statusKey = json.optString("project_status", "")
    val item = ProjectItem(
        id = json.optInt("id"),
        title = json.optString("title", "Project"),
        statusLabel = projectStatusLabelByKey[statusKey] ?: statusKey.replaceFirstChar { it.titlecase() },
        location = json.optString("location", ""),
        expectedCompletion = json.optString("expected_completion", ""),
        department = json.optString("department", ""),
        description = json.optString("description", "").ifBlank { "No description provided." },
        mapsLink = json.optString("maps_link", ""),
        displayImage = json.optString("display_image", ""),
        cityName = json.cityName(""),
        avgRating = json.optDouble("avg_rating", 0.0).let { if (it.isNaN()) 0.0 else it },
        reviewCount = json.optInt("review_count", 0),
        commentCount = json.optInt("comment_count", 0),
    )
    projectCache[item.id] = item
    return item
}

/** Exposed separately so ListingsViewModel's stale-while-revalidate can peek the offline cache for the exact same URL before deciding whether a network refresh is needed. */
internal fun buildProjectListUrl(query: String, categoryKey: String, citySlug: String, page: Int): String = buildString {
    append(API_BASE_URL)
    append("/api/v1/listings/project/?page=")
    append(page)
    append("&page_size=10")
    if (query.isNotBlank()) append("&q=").append(URLEncoder.encode(query, "UTF-8"))
    if (categoryKey.isNotBlank()) append("&category=").append(URLEncoder.encode(categoryKey, "UTF-8"))
    if (citySlug.isNotBlank()) append("&city=").append(URLEncoder.encode(citySlug, "UTF-8"))
}

internal suspend fun fetchProjects(query: String, categoryKey: String, citySlug: String, page: Int): ApiListPage<ProjectItem> =
    fetchListPage(buildProjectListUrl(query, categoryKey, citySlug, page), page) { parseProject(it) }

internal suspend fun fetchProjectDetail(id: Int): ProjectItem =
    parseProject(httpJson("$API_BASE_URL/api/v1/listings/project/$id/"))

@Composable
internal fun ProjectBrowseScreen(navController: NavController, initialCategoryKey: String? = null) {
    var query by rememberSaveable { mutableStateOf("") }
    var cityQuery by rememberSaveable { mutableStateOf(AppCityState.current.value?.name.orEmpty()) }
    var selectedCity by remember { mutableStateOf(AppCityState.current.value?.let { CitySuggestion(it.slug, it.name) }) }
    var citySuggestions by remember { mutableStateOf<List<CitySuggestion>>(emptyList()) }
    var categories by remember { mutableStateOf<List<CategoryOption>>(emptyList()) }
    var selectedCategoryKey by rememberSaveable { mutableStateOf(initialCategoryKey.orEmpty()) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        categories = try {
            fetchCategories("project")
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    // UI -> ViewModel -> Repository (fetchProjects/OfflineCache) instead of
    // this composable managing the network call itself.
    val viewModel: ListingsViewModel<ProjectItem> = viewModel(
        factory = viewModelFactory {
            initializer { ListingsViewModel(::buildProjectListUrl, ::fetchProjects, ::parseProject) }
        },
    )
    val uiState by viewModel.state.collectAsState()

    LaunchedEffect(query, selectedCategoryKey, selectedCity?.slug ?: cityQuery) {
        kotlinx.coroutines.delay(300)
        viewModel.load(query, selectedCategoryKey, selectedCity?.slug ?: cityQuery.trim(), reset = true)
    }

    LaunchedEffect(cityQuery) {
        kotlinx.coroutines.delay(300)
        citySuggestions = if (cityQuery.isBlank()) emptyList() else fetchCitySuggestionsSafely(cityQuery)
    }

    LaunchedEffect(listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index) {
        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        if (!uiState.isLoading && !uiState.isLoadingMore && uiState.hasMore && lastVisibleIndex >= uiState.items.size - 3) {
            viewModel.load(query, selectedCategoryKey, selectedCity?.slug ?: cityQuery.trim(), reset = false)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OneTownCityTopAppBar(
            title = "Projects",
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavigationClick = { navController.popBackStack() },
        )

        OneTownCityTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search projects",
            leadingIcon = Icons.Outlined.Build,
        )

        OneTownCityTextField(
            value = cityQuery,
            onValueChange = { cityQuery = it; selectedCity = null },
            placeholder = "City or area (optional)",
            leadingIcon = Icons.Filled.LocationOn,
        )
        if (citySuggestions.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                citySuggestions.take(3).forEach { suggestion ->
                    OneTownCityButton(
                        text = suggestion.name,
                        onClick = { selectedCity = suggestion; cityQuery = suggestion.name; citySuggestions = emptyList() },
                        variant = OneTownCityButtonVariant.Outlined,
                    )
                }
            }
        }

        if (categories.isNotEmpty()) {
            val labelByKey = categories.associate { it.key to it.label }
            val keyByLabel = categories.associate { it.label to it.key }
            val selectedLabel = labelByKey[selectedCategoryKey] ?: "All"
            OneTownCityChipGroup(
                items = listOf("All") + categories.map { it.label },
                selected = setOf(selectedLabel),
                onSelected = { label -> selectedCategoryKey = if (label == "All") "" else keyByLabel[label].orEmpty() },
            )
        }

        if (uiState.isShowingCachedData) {
            OneTownCityCacheStatusBanner(
                message = if (uiState.isRefreshing) "Showing saved results — refreshing…" else "You're offline — showing saved results",
                isOffline = !uiState.isRefreshing,
            )
        }

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    OneTownCityCircularLoading(label = "Loading projects")
                }
            }
            uiState.isOfflineNoCache -> {
                OneTownCityEmptyState(
                    title = "You're offline",
                    message = "Projects haven't been loaded yet on this device. Connect to the internet once to load them.",
                    icon = Icons.Outlined.Build,
                    action = {
                        OneTownCityButton(
                            text = "Retry",
                            onClick = { viewModel.load(query, selectedCategoryKey, selectedCity?.slug ?: cityQuery.trim(), reset = true) },
                            variant = OneTownCityButtonVariant.Outlined,
                        )
                    },
                )
            }
            uiState.error != null -> {
                OneTownCityErrorState(
                    title = "Unable to load projects",
                    message = uiState.error ?: "Please try again later.",
                    actionText = "Retry",
                    onRetry = { viewModel.load(query, selectedCategoryKey, selectedCity?.slug ?: cityQuery.trim(), reset = true) },
                )
            }
            uiState.items.isEmpty() -> {
                OneTownCityEmptyState(
                    title = "No projects found",
                    message = "No civic or infrastructure projects match your current search, category, and city filter.",
                    icon = Icons.Outlined.Build,
                    action = {
                        OneTownCityButton(
                            text = "Reset filters",
                            onClick = { query = ""; selectedCity = null; cityQuery = ""; selectedCategoryKey = "" },
                            variant = OneTownCityButtonVariant.Outlined,
                        )
                    },
                )
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    itemsIndexed(uiState.items, key = { _, item -> item.id }) { _, item ->
                        ProjectCard(item = item, onClick = { navController.navigate("project/${item.id}") })
                    }
                    if (uiState.isLoadingMore) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                OneTownCityCircularLoading(label = "Loading more")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(item: ProjectItem, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (item.displayImage.isNotBlank()) {
                AsyncImage(
                    model = rememberOptimizedImageRequest(item.displayImage),
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
                    Icon(imageVector = Icons.Outlined.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = item.statusLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(text = item.title, style = MaterialTheme.typography.titleMedium)
                if (item.location.isNotBlank()) {
                    Text(text = item.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
            }
        }
    }
}

@Composable
internal fun ProjectDetailScreen(navController: NavController, projectId: Int) {
    var item by remember { mutableStateOf(projectCache[projectId]) }
    var isLoading by remember { mutableStateOf(item == null) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    suspend fun load() {
        try {
            item = fetchProjectDetail(projectId)
            error = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (item == null) error = e.message ?: "Unable to load this project right now."
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(projectId) { load() }

    when {
        item == null && isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                OneTownCityCircularLoading(label = "Loading project")
            }
        }
        item == null -> {
            OneTownCityErrorState(
                title = "Unable to load this project",
                message = error ?: "Please try again later.",
                actionText = "Retry",
                onRetry = { isLoading = true; coroutineScope.launch { load() } },
            )
        }
        else -> ProjectDetailContent(navController = navController, item = item!!)
    }
}

@Composable
private fun ProjectDetailContent(navController: NavController, item: ProjectItem) {
    val context = LocalContext.current
    val activity = context as? Activity

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        OneTownCityTopAppBar(
            title = item.title,
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavigationClick = { navController.popBackStack() },
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        ) {
            item {
                if (item.displayImage.isNotBlank()) {
                    AsyncImage(
                        model = rememberOptimizedImageRequest(item.displayImage),
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
                        Icon(imageVector = Icons.Outlined.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                    }
                }
            }
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = item.title, style = MaterialTheme.typography.headlineSmall)
                        Text(text = item.statusLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(text = item.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (item.location.isNotBlank()) DetailRow(icon = Icons.Filled.LocationOn, title = item.location)
                        if (item.expectedCompletion.isNotBlank()) DetailRow(icon = Icons.Filled.DateRange, title = "Expected completion: ${item.expectedCompletion}")
                        if (item.department.isNotBlank()) DetailRow(icon = Icons.Outlined.Build, title = item.department)
                    }
                }
            }
            item {
                val mapsUri = safeWebUri(item.mapsLink)
                    ?: Uri.parse("https://www.google.com/maps/search/?api=1&query=${URLEncoder.encode(item.title, "UTF-8")}")
                OneTownCityButton(
                    text = "Get directions",
                    onClick = {
                        try {
                            activity?.startActivity(Intent(Intent.ACTION_VIEW, mapsUri))
                        } catch (_: ActivityNotFoundException) { }
                    },
                    leadingIcon = Icons.Filled.LocationOn,
                    variant = OneTownCityButtonVariant.Outlined,
                )
            }
            item {
                ReviewsSection(
                    modelKey = "project",
                    listingId = item.id,
                    avgRating = item.avgRating,
                    reviewCount = item.reviewCount,
                    navController = navController,
                )
            }
            item {
                CommentsSection(
                    modelKey = "project",
                    listingId = item.id,
                    commentCount = item.commentCount,
                    navController = navController,
                )
            }
        }
    }
}
