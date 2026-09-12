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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Store
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

/**
 * Real Business browse/detail (Phase 4 §3). Business.CATEGORY_CHOICES
 * (core/models.py:723) is a fixed CharField choice list, not the generic
 * Category table, and core/api/views.py's _list_listings filters business by
 * `category=<this raw choice value>` — so the filter chips below use the
 * literal choice list rather than fetchCategories (which reads the generic
 * Category model, correct for Property/Project but wrong for Business).
 */
internal val businessCategoryOptions = listOf(
    CategoryOption("retail", "Retail Shop"),
    CategoryOption("grocery", "Grocery Store"),
    CategoryOption("restaurant", "Restaurant & Food"),
    CategoryOption("electronics", "Electronics"),
    CategoryOption("clothing", "Clothing & Fashion"),
    CategoryOption("pharmacy", "Pharmacy"),
    CategoryOption("hardware", "Hardware & Building Materials"),
    CategoryOption("bakery", "Bakery & Sweets"),
    CategoryOption("salon", "Salon & Spa"),
    CategoryOption("automobile", "Automobile & Repair"),
    CategoryOption("stationery", "Stationery & Books"),
    CategoryOption("jewellery", "Jewellery"),
    CategoryOption("hospital", "Hospital"),
    CategoryOption("school", "School"),
    CategoryOption("college", "College"),
    CategoryOption("transport", "Transport"),
    CategoryOption("repair", "Repair Services"),
    CategoryOption("tourism", "Places to Visit"),
    CategoryOption("tuition_center", "Tuition & Coaching Centers"),
    CategoryOption("marketplace", "Buy / Sell / Exchange"),
    CategoryOption("student_services", "Student Services"),
    CategoryOption("scholarship_scheme", "Scholarships & Govt. Schemes"),
    CategoryOption("other", "Other"),
)
private val businessCategoryLabelByKey = businessCategoryOptions.associate { it.key to it.label }
private val businessCategoryKeyByLabel = businessCategoryOptions.associate { it.label to it.key }

internal data class BusinessItem(
    val id: Int,
    val name: String,
    val categoryLabel: String,
    val address: String,
    val phoneNumber: String,
    val description: String,
    val website: String,
    val mapsLink: String,
    val displayImage: String,
    val cityName: String,
    val avgRating: Double,
    val reviewCount: Int,
    val commentCount: Int,
)

internal val businessCache = BoundedItemCache<Int, BusinessItem>(200)

private fun parseBusiness(json: JSONObject): BusinessItem {
    val categoryKey = json.optString("category", "")
    val item = BusinessItem(
        id = json.optInt("id"),
        name = json.optString("name", "Business"),
        categoryLabel = businessCategoryLabelByKey[categoryKey] ?: categoryKey.replaceFirstChar { it.titlecase() },
        address = json.optString("address", ""),
        phoneNumber = json.optString("phone_number", ""),
        description = json.optString("description", "").ifBlank { "No description provided." },
        website = json.optString("website", ""),
        mapsLink = json.optString("maps_link", ""),
        displayImage = json.optString("display_image", ""),
        cityName = json.cityName(""),
        avgRating = json.optDouble("avg_rating", 0.0).let { if (it.isNaN()) 0.0 else it },
        reviewCount = json.optInt("review_count", 0),
        commentCount = json.optInt("comment_count", 0),
    )
    businessCache[item.id] = item
    return item
}

/** Exposed separately (not just inlined in fetchBusinesses) so ListingsViewModel's stale-while-revalidate can peek the offline cache for the exact same URL before deciding whether a network refresh is needed. */
internal fun buildBusinessListUrl(query: String, categoryKey: String, citySlug: String, page: Int): String = buildString {
    append(API_BASE_URL)
    append("/api/v1/listings/business/?page=")
    append(page)
    append("&page_size=10")
    if (query.isNotBlank()) append("&q=").append(URLEncoder.encode(query, "UTF-8"))
    if (categoryKey.isNotBlank()) append("&category=").append(URLEncoder.encode(categoryKey, "UTF-8"))
    if (citySlug.isNotBlank()) append("&city=").append(URLEncoder.encode(citySlug, "UTF-8"))
}

internal suspend fun fetchBusinesses(query: String, categoryKey: String, citySlug: String, page: Int): ApiListPage<BusinessItem> =
    fetchListPage(buildBusinessListUrl(query, categoryKey, citySlug, page), page) { parseBusiness(it) }

/** GET /api/v1/listings/business/<id>/ — public for approved+active listings; used as a deep-link-safe fallback when businessCache misses. */
internal suspend fun fetchBusinessDetail(id: Int): BusinessItem =
    parseBusiness(httpJson("$API_BASE_URL/api/v1/listings/business/$id/"))

@Composable
internal fun BusinessBrowseScreen(navController: NavController, initialCategoryKey: String? = null) {
    var query by rememberSaveable { mutableStateOf("") }
    var cityQuery by rememberSaveable { mutableStateOf(AppCityState.current.value?.name.orEmpty()) }
    var selectedCity by remember { mutableStateOf(AppCityState.current.value?.let { CitySuggestion(it.slug, it.name) }) }
    var citySuggestions by remember { mutableStateOf<List<CitySuggestion>>(emptyList()) }
    var selectedCategoryKey by rememberSaveable { mutableStateOf(initialCategoryKey.orEmpty()) }
    val listState = rememberLazyListState()

    // UI -> ViewModel -> Repository (fetchBusinesses/OfflineCache) instead of
    // this composable managing the network call itself — see
    // ListingsViewModel's doc comment for the stale-while-revalidate design.
    val viewModel: ListingsViewModel<BusinessItem> = viewModel(
        factory = viewModelFactory {
            initializer { ListingsViewModel(::buildBusinessListUrl, ::fetchBusinesses, ::parseBusiness) }
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
            title = "Businesses",
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavigationClick = { navController.popBackStack() },
        )

        OneTownCityTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search businesses",
            leadingIcon = Icons.Filled.Store,
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

        val selectedLabel = businessCategoryLabelByKey[selectedCategoryKey] ?: "All"
        OneTownCityChipGroup(
            items = listOf("All") + businessCategoryOptions.map { it.label },
            selected = setOf(selectedLabel),
            onSelected = { label -> selectedCategoryKey = if (label == "All") "" else businessCategoryKeyByLabel[label].orEmpty() },
        )

        if (uiState.isShowingCachedData) {
            OneTownCityCacheStatusBanner(
                message = if (uiState.isRefreshing) "Showing saved results — refreshing…" else "You're offline — showing saved results",
                isOffline = !uiState.isRefreshing,
            )
        }

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    OneTownCityCircularLoading(label = "Loading businesses")
                }
            }
            uiState.isOfflineNoCache -> {
                OneTownCityEmptyState(
                    title = "You're offline",
                    message = "Businesses haven't been loaded yet on this device. Connect to the internet once to load them.",
                    icon = Icons.Filled.Store,
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
                    title = "Unable to load businesses",
                    message = uiState.error ?: "Please try again later.",
                    actionText = "Retry",
                    onRetry = { viewModel.load(query, selectedCategoryKey, selectedCity?.slug ?: cityQuery.trim(), reset = true) },
                )
            }
            uiState.items.isEmpty() -> {
                OneTownCityEmptyState(
                    title = "No businesses found",
                    message = "No listings match your current search, category, and city filter.",
                    icon = Icons.Filled.Store,
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
                        BusinessCard(item = item, onClick = { navController.navigate("business/${item.id}") })
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
private fun BusinessCard(item: BusinessItem, onClick: () -> Unit) {
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
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = Icons.Filled.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = item.categoryLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                if (item.address.isNotBlank()) {
                    Text(text = item.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
                if (item.reviewCount > 0) {
                    Text(
                        text = "%.1f ★ (%d reviews)".format(item.avgRating, item.reviewCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun BusinessDetailScreen(navController: NavController, businessId: Int) {
    var item by remember { mutableStateOf(businessCache[businessId]) }
    var isLoading by remember { mutableStateOf(item == null) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    suspend fun load() {
        try {
            item = fetchBusinessDetail(businessId)
            error = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (item == null) error = e.message ?: "Unable to load this business right now."
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(businessId) { load() }

    when {
        item == null && isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                OneTownCityCircularLoading(label = "Loading business")
            }
        }
        item == null -> {
            OneTownCityErrorState(
                title = "Unable to load this business",
                message = error ?: "Please try again later.",
                actionText = "Retry",
                onRetry = { isLoading = true; coroutineScope.launch { load() } },
            )
        }
        else -> BusinessDetailContent(navController = navController, item = item!!)
    }
}

@Composable
private fun BusinessDetailContent(navController: NavController, item: BusinessItem) {
    val context = LocalContext.current
    val activity = context as? Activity

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        OneTownCityTopAppBar(
            title = item.name,
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
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(imageVector = Icons.Filled.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                    }
                }
            }
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = item.name, style = MaterialTheme.typography.headlineSmall)
                        Text(text = item.categoryLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(text = item.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (item.address.isNotBlank()) DetailRow(icon = Icons.Filled.LocationOn, title = item.address)
                        if (item.phoneNumber.isNotBlank()) DetailRow(icon = Icons.Filled.Call, title = item.phoneNumber)
                        if (item.website.isNotBlank()) DetailRow(icon = Icons.Filled.Language, title = item.website)
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (item.phoneNumber.isNotBlank()) {
                        OneTownCityButton(
                            text = "Call",
                            onClick = { activity?.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.phoneNumber}"))) },
                            leadingIcon = Icons.Filled.Call,
                            variant = OneTownCityButtonVariant.Primary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (item.website.isNotBlank()) {
                        OneTownCityButton(
                            text = "Website",
                            onClick = {
                                safeWebUri(item.website)?.let { uri ->
                                    try {
                                        activity?.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    } catch (_: ActivityNotFoundException) { }
                                }
                            },
                            leadingIcon = Icons.Filled.Language,
                            variant = OneTownCityButtonVariant.Secondary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            item {
                val mapsUri = safeWebUri(item.mapsLink)
                    ?: Uri.parse("https://www.google.com/maps/search/?api=1&query=${URLEncoder.encode(item.name, "UTF-8")}")
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
                    modelKey = "business",
                    listingId = item.id,
                    avgRating = item.avgRating,
                    reviewCount = item.reviewCount,
                    navController = navController,
                )
            }
            item {
                CommentsSection(
                    modelKey = "business",
                    listingId = item.id,
                    commentCount = item.commentCount,
                    navController = navController,
                )
            }
        }
    }
}
