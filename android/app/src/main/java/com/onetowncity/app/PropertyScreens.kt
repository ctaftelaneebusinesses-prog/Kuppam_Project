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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
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
 * Real Property browse/detail (Phase 4 §4) — a distinct listing type from
 * Marketplace (core/models.py's Property vs. Business category="marketplace"
 * used by the Buy/Sell/Exchange student feature); this file never touches
 * MarketplaceItem/fetchMarketplace*. Unlike Business, Property's `category`
 * query param matches the generic Category.key (core/api/views.py's
 * _list_listings: `qs.filter(listing_category__key=category_param)` for any
 * non-business model_key), so category options come from fetchCategories,
 * not a hardcoded list.
 */
private val propertyTypeLabelByKey = mapOf(
    "sale" to "For Sale",
    "rent" to "For Rent",
    "apartment" to "Apartment / Flat",
    "villa" to "Villa / Independent House",
    "plot" to "Plot / Land",
    "commercial" to "Commercial Space",
    "pg" to "PG / Hostel",
    "other" to "Other",
)

internal data class PropertyItem(
    val id: Int,
    val title: String,
    val propertyTypeLabel: String,
    val price: Double,
    val location: String,
    val contactNumber: String,
    val description: String,
    val mapsLink: String,
    val displayImage: String,
    val cityName: String,
    val avgRating: Double,
    val reviewCount: Int,
    val commentCount: Int,
)

internal val propertyCache = BoundedItemCache<Int, PropertyItem>(200)

private fun formatRupees(amount: Double): String = "₹" + "%,.0f".format(amount)

private fun parseProperty(json: JSONObject): PropertyItem {
    val typeKey = json.optString("property_type", "")
    val item = PropertyItem(
        id = json.optInt("id"),
        title = json.optString("title", "Property"),
        propertyTypeLabel = propertyTypeLabelByKey[typeKey] ?: typeKey.replaceFirstChar { it.titlecase() },
        price = json.optDouble("price", 0.0).let { if (it.isNaN()) 0.0 else it },
        location = json.optString("location", ""),
        contactNumber = json.optString("contact_number", ""),
        description = json.optString("description", "").ifBlank { "No description provided." },
        mapsLink = json.optString("maps_link", ""),
        displayImage = json.optString("display_image", ""),
        cityName = json.cityName(""),
        avgRating = json.optDouble("avg_rating", 0.0).let { if (it.isNaN()) 0.0 else it },
        reviewCount = json.optInt("review_count", 0),
        commentCount = json.optInt("comment_count", 0),
    )
    propertyCache[item.id] = item
    return item
}

/** Exposed separately so ListingsViewModel's stale-while-revalidate can peek the offline cache for the exact same URL before deciding whether a network refresh is needed. */
internal fun buildPropertyListUrl(query: String, categoryKey: String, citySlug: String, page: Int): String = buildString {
    append(API_BASE_URL)
    append("/api/v1/listings/property/?page=")
    append(page)
    append("&page_size=10")
    if (query.isNotBlank()) append("&q=").append(URLEncoder.encode(query, "UTF-8"))
    if (categoryKey.isNotBlank()) append("&category=").append(URLEncoder.encode(categoryKey, "UTF-8"))
    if (citySlug.isNotBlank()) append("&city=").append(URLEncoder.encode(citySlug, "UTF-8"))
}

internal suspend fun fetchProperties(query: String, categoryKey: String, citySlug: String, page: Int): ApiListPage<PropertyItem> =
    fetchListPage(buildPropertyListUrl(query, categoryKey, citySlug, page), page) { parseProperty(it) }

internal suspend fun fetchPropertyDetail(id: Int): PropertyItem =
    parseProperty(httpJson("$API_BASE_URL/api/v1/listings/property/$id/"))

@Composable
internal fun PropertyBrowseScreen(navController: NavController, initialCategoryKey: String? = null) {
    var query by rememberSaveable { mutableStateOf("") }
    var cityQuery by rememberSaveable { mutableStateOf(AppCityState.current.value?.name.orEmpty()) }
    var selectedCity by remember { mutableStateOf(AppCityState.current.value?.let { CitySuggestion(it.slug, it.name) }) }
    var citySuggestions by remember { mutableStateOf<List<CitySuggestion>>(emptyList()) }
    var categories by remember { mutableStateOf<List<CategoryOption>>(emptyList()) }
    var selectedCategoryKey by rememberSaveable { mutableStateOf(initialCategoryKey.orEmpty()) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        categories = try {
            fetchCategories("property")
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    // UI -> ViewModel -> Repository (fetchProperties/OfflineCache) instead of
    // this composable managing the network call itself.
    val viewModel: ListingsViewModel<PropertyItem> = viewModel(
        factory = viewModelFactory {
            initializer { ListingsViewModel(::buildPropertyListUrl, ::fetchProperties, ::parseProperty) }
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
            title = "Properties",
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavigationClick = { navController.popBackStack() },
        )

        OneTownCityTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search properties",
            leadingIcon = Icons.Filled.Home,
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
                    OneTownCityCircularLoading(label = "Loading properties")
                }
            }
            uiState.isOfflineNoCache -> {
                OneTownCityEmptyState(
                    title = "You're offline",
                    message = "Properties haven't been loaded yet on this device. Connect to the internet once to load them.",
                    icon = Icons.Filled.Home,
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
                    title = "Unable to load properties",
                    message = uiState.error ?: "Please try again later.",
                    actionText = "Retry",
                    onRetry = { viewModel.load(query, selectedCategoryKey, selectedCity?.slug ?: cityQuery.trim(), reset = true) },
                )
            }
            uiState.items.isEmpty() -> {
                OneTownCityEmptyState(
                    title = "No properties found",
                    message = "No listings match your current search, category, and city filter.",
                    icon = Icons.Filled.Home,
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
                        PropertyCard(item = item, onClick = { navController.navigate("property/${item.id}") })
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
private fun PropertyCard(item: PropertyItem, onClick: () -> Unit) {
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
                    Icon(imageVector = Icons.Filled.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = item.propertyTypeLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(text = item.title, style = MaterialTheme.typography.titleMedium)
                Text(text = formatRupees(item.price), style = MaterialTheme.typography.titleSmall)
                if (item.location.isNotBlank()) {
                    Text(text = item.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
            }
        }
    }
}

@Composable
internal fun PropertyDetailScreen(navController: NavController, propertyId: Int) {
    var item by remember { mutableStateOf(propertyCache[propertyId]) }
    var isLoading by remember { mutableStateOf(item == null) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    suspend fun load() {
        try {
            item = fetchPropertyDetail(propertyId)
            error = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (item == null) error = e.message ?: "Unable to load this property right now."
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(propertyId) { load() }

    when {
        item == null && isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                OneTownCityCircularLoading(label = "Loading property")
            }
        }
        item == null -> {
            OneTownCityErrorState(
                title = "Unable to load this property",
                message = error ?: "Please try again later.",
                actionText = "Retry",
                onRetry = { isLoading = true; coroutineScope.launch { load() } },
            )
        }
        else -> PropertyDetailContent(navController = navController, item = item!!)
    }
}

@Composable
private fun PropertyDetailContent(navController: NavController, item: PropertyItem) {
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
                        Icon(imageVector = Icons.Filled.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                    }
                }
            }
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = item.title, style = MaterialTheme.typography.headlineSmall)
                        Text(text = item.propertyTypeLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(text = formatRupees(item.price), style = MaterialTheme.typography.titleLarge)
                        Text(text = item.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (item.location.isNotBlank()) DetailRow(icon = Icons.Filled.LocationOn, title = item.location)
                        if (item.contactNumber.isNotBlank()) DetailRow(icon = Icons.Filled.Call, title = item.contactNumber)
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (item.contactNumber.isNotBlank()) {
                        OneTownCityButton(
                            text = "Call",
                            onClick = { activity?.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.contactNumber}"))) },
                            leadingIcon = Icons.Filled.Call,
                            variant = OneTownCityButtonVariant.Primary,
                            modifier = Modifier.weight(1f),
                        )
                    }
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
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                ReviewsSection(
                    modelKey = "property",
                    listingId = item.id,
                    avgRating = item.avgRating,
                    reviewCount = item.reviewCount,
                    navController = navController,
                )
            }
            item {
                CommentsSection(
                    modelKey = "property",
                    listingId = item.id,
                    commentCount = item.commentCount,
                    navController = navController,
                )
            }
        }
    }
}
