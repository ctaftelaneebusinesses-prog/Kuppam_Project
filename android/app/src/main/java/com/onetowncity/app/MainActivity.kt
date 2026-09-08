package com.onetowncity.app

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.onetowncity.app.designsystem.OneTownCityBottomNavItem
import com.onetowncity.app.designsystem.OneTownCityBottomNavigation
import com.onetowncity.app.designsystem.OneTownCityButton
import com.onetowncity.app.designsystem.OneTownCityButtonVariant
import com.onetowncity.app.designsystem.OneTownCityCircularLoading
import com.onetowncity.app.designsystem.OneTownCityEmptyState
import com.onetowncity.app.designsystem.OneTownCityErrorState
import com.onetowncity.app.designsystem.OneTownCitySearchBar
import com.onetowncity.app.designsystem.OneTownCityTextField
import com.onetowncity.app.designsystem.OneTownCityTheme
import com.onetowncity.app.designsystem.OneTownCityTopAppBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            OneTownCityTheme {
                OneTownCityAppShell()
            }
        }
    }
}

private enum class AppTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val deepLink: String,
) {
    HOME("home", "Home", Icons.Filled.Home, Icons.Outlined.Home, "onetowncity://home"),
    SEARCH("search", "Search", Icons.Filled.Search, Icons.Outlined.Search, "onetowncity://search"),
    STUDENTS("students", "Students", Icons.Outlined.School, Icons.Outlined.School, "onetowncity://students"),
    SAVED("saved", "Saved", Icons.Filled.Star, Icons.Outlined.Star, "onetowncity://saved"),
    PROFILE("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person, "onetowncity://profile"),
}

internal enum class LocationPermissionState {
    GRANTED,
    DENIED,
    REVOKED,
    UNAVAILABLE,
}

internal data class CitySelection(
    val slug: String,
    val name: String,
)

private const val CITY_PREFS_NAME = "one_town_city_city_selection"
private const val KEY_SELECTED_CITY_SLUG = "selected_city_slug"
private const val KEY_SELECTED_CITY_NAME = "selected_city_name"
private const val KEY_RECENT_CITIES = "recent_cities"

internal fun resolveLocationPermissionState(context: Context): LocationPermissionState {
    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return when {
        hasFine || hasCoarse -> LocationPermissionState.GRANTED
        else -> {
            val locationManager = context.getSystemService(LocationManager::class.java)
            if (locationManager == null) LocationPermissionState.UNAVAILABLE else LocationPermissionState.DENIED
        }
    }
}

private fun readSavedCity(context: Context): CitySelection? {
    val prefs = context.getSharedPreferences(CITY_PREFS_NAME, Context.MODE_PRIVATE)
    val slug = prefs.getString(KEY_SELECTED_CITY_SLUG, null)
    val name = prefs.getString(KEY_SELECTED_CITY_NAME, null)
    return if (!slug.isNullOrBlank() && !name.isNullOrBlank()) CitySelection(slug = slug, name = name) else null
}

private fun writeSavedCity(context: Context, city: CitySelection?) {
    val prefs = context.getSharedPreferences(CITY_PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit {
        if (city == null) {
            remove(KEY_SELECTED_CITY_SLUG)
            remove(KEY_SELECTED_CITY_NAME)
        } else {
            putString(KEY_SELECTED_CITY_SLUG, city.slug)
            putString(KEY_SELECTED_CITY_NAME, city.name)
        }
    }
}

private fun readRecentCities(context: Context): List<CitySelection> {
    val prefs = context.getSharedPreferences(CITY_PREFS_NAME, Context.MODE_PRIVATE)
    val serialized = prefs.getString(KEY_RECENT_CITIES, "") ?: return emptyList()
    if (serialized.isBlank()) return emptyList()
    return serialized.split("||").mapNotNull { entry ->
        val parts = entry.split("::", limit = 2)
        if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
            CitySelection(slug = parts[0], name = parts[1])
        } else {
            null
        }
    }.distinctBy { it.slug }.take(5)
}

private fun writeRecentCities(context: Context, cities: List<CitySelection>) {
    val prefs = context.getSharedPreferences(CITY_PREFS_NAME, Context.MODE_PRIVATE)
    val serialized = cities.take(5).joinToString("||") { "${it.slug}::${it.name}" }
    prefs.edit { putString(KEY_RECENT_CITIES, serialized) }
}

private fun upsertRecentCity(context: Context, city: CitySelection) {
    val updated = listOf(city) + readRecentCities(context).filter { it.slug != city.slug }
    writeRecentCities(context, updated)
}

internal fun pickPreferredCity(
    currentCity: CitySelection?,
    selectedCity: CitySelection?,
    recentCities: List<CitySelection>,
    permissionState: LocationPermissionState,
): CitySelection? {
    if (selectedCity != null) return selectedCity
    if (permissionState == LocationPermissionState.GRANTED && currentCity != null) return currentCity
    return recentCities.firstOrNull() ?: currentCity
}

private enum class PlaceholderState {
    EMPTY,
    LOADING,
    ERROR,
}

private val appTabs = listOf(
    AppTab.HOME,
    AppTab.SEARCH,
    AppTab.STUDENTS,
    AppTab.SAVED,
    AppTab.PROFILE,
)

@Composable
private fun OneTownCityAppShell() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val selectedRoute = currentBackStackEntry?.destination?.route ?: AppTab.HOME.route
    val selectedTabIndex = appTabs.indexOfFirst { it.route == selectedRoute }.coerceAtLeast(0)
    val context = LocalContext.current
    var permissionState by remember { mutableStateOf(resolveLocationPermissionState(context)) }
    val requestLocationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        permissionState = if (granted.values.any { it }) {
            LocationPermissionState.GRANTED
        } else {
            LocationPermissionState.DENIED
        }
    }

    val getCurrentLocationOrNull: () -> android.location.Location? = {
        val locationManager = context.getSystemService(LocationManager::class.java)
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (locationManager == null || !hasPermission) {
            null
        } else {
            @SuppressLint("MissingPermission")
            val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            location
        }
    }

    var savedCity by remember { mutableStateOf(readSavedCity(context)) }
    var recentCities by remember { mutableStateOf(readRecentCities(context)) }
    var currentCity by remember { mutableStateOf<CitySelection?>(null) }
    var cityQuery by rememberSaveable { mutableStateOf("") }
    var citySuggestions by remember { mutableStateOf<List<CitySuggestion>>(emptyList()) }
    val resolvedCity = pickPreferredCity(currentCity, savedCity, recentCities, permissionState)

    LaunchedEffect(savedCity) {
        val cityToSave = savedCity
        if (cityToSave != null) {
            writeSavedCity(context, cityToSave)
            upsertRecentCity(context, cityToSave)
            recentCities = readRecentCities(context)
        }
    }

    LaunchedEffect(cityQuery) {
        kotlinx.coroutines.delay(300)
        citySuggestions = if (cityQuery.isBlank()) emptyList() else fetchCitySuggestionsSafely(cityQuery)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Surface(
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "City",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        when (permissionState) {
                            LocationPermissionState.GRANTED -> {
                                OneTownCityButton(
                                    text = "Use current city",
                                    onClick = {
                                        val location = getCurrentLocationOrNull()
                                        if (location != null) {
                                            currentCity = CitySelection(slug = "current", name = "Current location")
                                        }
                                    },
                                    variant = OneTownCityButtonVariant.Text,
                                )
                            }
                            LocationPermissionState.DENIED, LocationPermissionState.REVOKED -> {
                                OneTownCityButton(
                                    text = "Allow location",
                                    onClick = {
                                        requestLocationPermission.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                            ),
                                        )
                                    },
                                    variant = OneTownCityButtonVariant.Text,
                                )
                            }
                            LocationPermissionState.UNAVAILABLE -> {
                                Text(
                                    text = "Location unavailable",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Text(
                        text = resolvedCity?.name ?: "Select a city",
                        style = MaterialTheme.typography.headlineSmall,
                    )

                    OneTownCityTextField(
                        value = cityQuery,
                        onValueChange = { cityQuery = it },
                        placeholder = "Search city",
                        leadingIcon = Icons.Filled.LocationOn,
                    )

                    if (citySuggestions.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            citySuggestions.take(3).forEach { suggestion ->
                                OneTownCityButton(
                                    text = suggestion.name,
                                    onClick = {
                                        val city = CitySelection(slug = suggestion.slug, name = suggestion.name)
                                        savedCity = city
                                        currentCity = city
                                        cityQuery = ""
                                        citySuggestions = emptyList()
                                    },
                                    variant = OneTownCityButtonVariant.Outlined,
                                )
                            }
                        }
                    }

                    if (recentCities.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // Each button shares the Row equally (rather than
                            // sizing to its own text) so 3 recent cities always
                            // fit the screen width instead of the Row
                            // overflowing past the edge on longer city names —
                            // the button's own maxLines=1 + ellipsis (see
                            // ButtonContent in DesignSystem.kt) is then a real
                            // constrained-width fallback, not dead code.
                            recentCities.take(3).forEach { recent ->
                                OneTownCityButton(
                                    text = recent.name,
                                    onClick = { savedCity = recent; currentCity = recent },
                                    variant = if (recent.slug == resolvedCity?.slug) OneTownCityButtonVariant.Secondary else OneTownCityButtonVariant.Outlined,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            OneTownCityBottomNavigation(
                selectedTab = selectedTabIndex,
                onTabSelected = { index -> navController.navigateToTab(appTabs[index]) },
                items = appTabs.map {
                    OneTownCityBottomNavItem(
                        label = it.label,
                        selectedIcon = it.selectedIcon,
                        unselectedIcon = it.unselectedIcon,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppTab.HOME.route,
            // contentWindowInsets on the Scaffold above is WindowInsets.safeDrawing
            // (status/nav bars + display cutout) — it deliberately excludes the
            // IME inset, so a text field near the bottom of any screen (city
            // search, in-page search bars) would sit behind the on-screen
            // keyboard without this. One centralized modifier here covers every
            // destination instead of each screen needing its own imePadding().
            modifier = Modifier.fillMaxSize().padding(innerPadding).imePadding(),
        ) {
            appTabs.forEach { tab ->
                composable(
                    route = tab.route,
                    deepLinks = listOf(
                        navDeepLink { uriPattern = tab.deepLink },
                        navDeepLink { uriPattern = "https://onetowncity.com/${tab.route}" },
                        navDeepLink { uriPattern = "https://www.onetowncity.com/${tab.route}" },
                    ),
                    arguments = listOf(
                        navArgument("tab") { type = NavType.StringType; defaultValue = tab.route }
                    ),
                ) {
                    when (tab) {
                        AppTab.HOME -> PlaceholderShellScreen(
                            title = "Home",
                            description = "Your feed and community updates will appear here when content is ready.",
                            state = PlaceholderState.LOADING,
                            icon = Icons.Outlined.Home,
                            onPrimaryAction = { },
                        )
                        AppTab.SEARCH -> SearchPlaceholderScreen()
                        AppTab.STUDENTS -> StudentsHubScreen(navController)
                        AppTab.SAVED -> PlaceholderShellScreen(
                            title = "Saved",
                            description = "Saved listings and bookmarked updates will appear here.",
                            state = PlaceholderState.EMPTY,
                            icon = Icons.Outlined.Star,
                            onPrimaryAction = { },
                        )
                        AppTab.PROFILE -> PlaceholderShellScreen(
                            title = "Profile",
                            description = "Profile details and account controls are coming soon.",
                            state = PlaceholderState.ERROR,
                            icon = Icons.Outlined.Person,
                            onPrimaryAction = { },
                        )
                    }
                }
            }

            composable(
                route = "students/{categoryKey}",
                arguments = listOf(navArgument("categoryKey") { type = NavType.StringType }),
                deepLinks = listOf(
                    navDeepLink { uriPattern = "onetowncity://students/{categoryKey}" },
                    navDeepLink { uriPattern = "https://onetowncity.com/students/{categoryKey}" },
                    navDeepLink { uriPattern = "https://www.onetowncity.com/students/{categoryKey}" },
                ),
            ) { backStackEntry ->
                val key = backStackEntry.arguments?.getString("categoryKey") ?: StudentsCategory.TUITION_CENTERS.key
                val category = studentsCategories.firstOrNull { it.key == key } ?: StudentsCategory.TUITION_CENTERS
                StudentsCategoryScreen(navController, category)
            }

            composable(
                route = "student-service/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.IntType }),
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getInt("itemId") ?: return@composable
                val item = studentServicesCache[itemId]
                if (item == null) {
                    OneTownCityEmptyState(
                        title = "Service not found",
                        message = "This student service is no longer available.",
                        icon = Icons.Outlined.Build,
                        action = {
                            OneTownCityButton(
                                text = "Back",
                                onClick = { navController.popBackStack() },
                                variant = OneTownCityButtonVariant.Outlined,
                            )
                        },
                    )
                } else {
                    StudentServiceDetailScreen(navController = navController, item = item)
                }
            }

            composable(
                route = "scholarship/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.IntType }),
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getInt("itemId") ?: return@composable
                val item = ScholarshipItem(id = itemId, title = "No verified scholarship record", description = "The backend currently has no verified scholarship record for this item.", category = "Unavailable")
                ScholarshipDetailScreen(navController = navController, item = item)
            }

            composable(
                route = "lost-found/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.IntType }),
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getInt("itemId") ?: return@composable
                val item = LostFoundItem(
                    id = itemId,
                    title = "No verified lost & found item",
                    category = "Unavailable",
                    description = "This backend does not currently expose live lost or found listings. No personal or contact details are shown without verified server data.",
                    location = "",
                    date = "",
                    cityName = "",
                )
                LostFoundDetailScreen(navController = navController, item = item)
            }

            composable(
                route = "place/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.IntType }),
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getInt("itemId") ?: return@composable
                val item = PlaceItem(
                    id = itemId,
                    title = "No verified place record",
                    category = "Unavailable",
                    description = "The current backend does not expose a live places-to-visit dataset for this filtered view.",
                    cityName = "",
                )
                PlaceDetailScreen(navController = navController, item = item)
            }

            composable(
                route = "marketplace/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.IntType }),
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getInt("itemId") ?: return@composable
                val item = marketplaceCache[itemId]
                if (item == null) {
                    OneTownCityEmptyState(
                        title = "Listing not found",
                        message = "This marketplace listing is no longer available.",
                        icon = Icons.Outlined.ShoppingCart,
                        action = {
                            OneTownCityButton(
                                text = "Back",
                                onClick = { navController.popBackStack() },
                                variant = OneTownCityButtonVariant.Outlined,
                            )
                        },
                    )
                } else {
                    MarketplaceDetailScreen(navController = navController, item = item)
                }
            }

            composable(
                route = "event/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.IntType }),
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getInt("itemId") ?: return@composable
                val item = eventCache[itemId]
                if (item == null) {
                    OneTownCityEmptyState(
                        title = "Event not found",
                        message = "This event is no longer available.",
                        icon = Icons.Outlined.Event,
                        action = {
                            OneTownCityButton(
                                text = "Back",
                                onClick = { navController.popBackStack() },
                                variant = OneTownCityButtonVariant.Outlined,
                            )
                        },
                    )
                } else {
                    EventDetailScreen(navController = navController, item = item)
                }
            }

            composable(
                route = "tuition-center/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.IntType }),
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getInt("itemId") ?: return@composable
                val item = tuitionCenterCache[itemId]
                if (item == null) {
                    OneTownCityEmptyState(
                        title = "Tuition center not found",
                        message = "This listing is no longer available.",
                        icon = Icons.Outlined.School,
                        action = {
                            OneTownCityButton(
                                text = "Back",
                                onClick = { navController.popBackStack() },
                                variant = OneTownCityButtonVariant.Outlined,
                            )
                        },
                    )
                } else {
                    TuitionCenterDetailScreen(navController = navController, item = item)
                }
            }
        }
    }
}

@Composable
private fun SearchPlaceholderScreen() {
    val scrollState = rememberScrollState()
    var query by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OneTownCitySearchBar(
            query = query,
            onQueryChange = { query = it },
            placeholder = "Search nearby places",
            onClear = { query = "" },
        )

        OneTownCityEmptyState(
            title = "Search is ready",
            message = "No search results are available yet. Once content is added, matching listings and places will appear here.",
            icon = Icons.Outlined.Search,
            action = {
                OneTownCityButton(
                    text = "Clear search",
                    onClick = { query = "" },
                    variant = OneTownCityButtonVariant.Outlined,
                )
            },
        )
    }
}

private enum class StudentsCategory(
    val key: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
) {
    TUITION_CENTERS("tuition-centers", "Tuition Centers", "Find learning hubs and academic support options.", Icons.Outlined.School),
    EVENTS("events", "Events", "Browse campus and community events.", Icons.Outlined.Event),
    BUY_SELL_EXCHANGE("buy-sell-exchange", "Buy / Sell / Exchange", "Discover study essentials and student exchanges.", Icons.Outlined.ShoppingCart),
    STUDENT_SERVICES("student-services", "Student Services", "Explore local services for student needs.", Icons.Outlined.Build),
    SCHOLARSHIPS("scholarships", "Scholarships & Government Schemes", "Access support and funding opportunities.", Icons.Outlined.EmojiEvents),
    LOST_FOUND("lost-found", "Lost & Found", "Report or search for misplaced items.", Icons.Outlined.Search),
    PLACES_TO_VISIT("places-to-visit", "Places to Visit", "Discover nearby student-friendly destinations.", Icons.Outlined.LocationOn),
}

private val studentsCategories = listOf(
    StudentsCategory.TUITION_CENTERS,
    StudentsCategory.EVENTS,
    StudentsCategory.BUY_SELL_EXCHANGE,
    StudentsCategory.STUDENT_SERVICES,
    StudentsCategory.SCHOLARSHIPS,
    StudentsCategory.LOST_FOUND,
    StudentsCategory.PLACES_TO_VISIT,
)

@Composable
private fun StudentsHubScreen(navController: NavController) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredCategories = remember(query) {
        if (query.isBlank()) studentsCategories else studentsCategories.filter {
            it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OneTownCitySearchBar(
            query = query,
            onQueryChange = { query = it },
            placeholder = "Search student categories",
            onClear = { query = "" },
        )

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Students",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Browse student resources and discovery categories.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (filteredCategories.isEmpty()) {
            OneTownCityEmptyState(
                title = "No matching categories",
                message = "Try a different keyword to discover student services and opportunities.",
                icon = Icons.Outlined.Search,
                action = {
                    OneTownCityButton(
                        text = "Clear search",
                        onClick = { query = "" },
                        variant = OneTownCityButtonVariant.Outlined,
                    )
                },
            )
        } else {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 180.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 12.dp),
            ) {
                items(filteredCategories.size) { index ->
                    val category = filteredCategories[index]
                    StudentsCategoryCard(
                        category = category,
                        onClick = { navController.navigate("students/${category.key}") },
                    )
                }
            }
        }
    }
}

@Composable
private fun StudentsCategoryCard(
    category: StudentsCategory,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                shape = CircleShape,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Text(
                text = category.title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = category.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StudentsCategoryScreen(
    navController: NavController,
    category: StudentsCategory,
) {
    when (category) {
        StudentsCategory.TUITION_CENTERS -> {
            TuitionCentersFeatureScreen(navController = navController)
        }
        StudentsCategory.EVENTS -> {
            EventsFeatureScreen(navController = navController)
        }
        StudentsCategory.BUY_SELL_EXCHANGE -> {
            MarketplaceFeatureScreen(navController = navController)
        }
        StudentsCategory.STUDENT_SERVICES -> {
            StudentServicesFeatureScreen(navController = navController)
        }
        StudentsCategory.SCHOLARSHIPS -> {
            ScholarshipsFeatureScreen(navController = navController)
        }
        StudentsCategory.LOST_FOUND -> {
            LostFoundFeatureScreen(navController = navController)
        }
        StudentsCategory.PLACES_TO_VISIT -> {
            PlacesToVisitFeatureScreen(navController = navController)
        }
    }
}

internal data class StudentServiceItem(
    val id: Int,
    val name: String,
    val category: String,
    val description: String,
    val address: String,
    val phoneNumber: String,
    val website: String,
    val mapsLink: String,
    val imageUrl: String,
    val cityName: String,
    val citySlug: String,
)

internal fun filterStudentServices(
    items: List<StudentServiceItem>,
    query: String,
    selectedCategory: String,
): List<StudentServiceItem> {
    val normalizedQuery = query.trim()
    return items.filter { item ->
        val matchesQuery = normalizedQuery.isBlank() ||
            item.name.contains(normalizedQuery, ignoreCase = true) ||
            item.description.contains(normalizedQuery, ignoreCase = true) ||
            item.address.contains(normalizedQuery, ignoreCase = true)

        val matchesCategory = selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true)
        matchesQuery && matchesCategory
    }
}

internal data class ScholarshipItem(
    val id: Int,
    val title: String,
    val description: String,
    val category: String,
    val deadline: String = "",
    val eligibility: String = "",
    val officialUrl: String = "",
    val sourceLabel: String = "OneTownCity",
    val sourceType: String = "local",
)

internal fun filterScholarships(
    items: List<ScholarshipItem>,
    query: String,
    selectedCategory: String,
): List<ScholarshipItem> {
    val normalizedQuery = query.trim()
    return items.filter { item ->
        val matchesQuery = normalizedQuery.isBlank() ||
            item.title.contains(normalizedQuery, ignoreCase = true) ||
            item.description.contains(normalizedQuery, ignoreCase = true) ||
            item.category.contains(normalizedQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true)
        matchesQuery && matchesCategory
    }
}

internal data class LostFoundItem(
    val id: Int,
    val title: String,
    val category: String,
    val description: String,
    val location: String,
    val date: String,
    val imageUrl: String = "",
    val contact: String = "",
    val reportUrl: String = "",
    val cityName: String = "",
    val ownerInfo: String = "",
)

internal fun filterLostFound(
    items: List<LostFoundItem>,
    query: String,
    selectedCategory: String,
): List<LostFoundItem> {
    val normalizedQuery = query.trim()
    return items.filter { item ->
        val matchesQuery = normalizedQuery.isBlank() ||
            item.title.contains(normalizedQuery, ignoreCase = true) ||
            item.description.contains(normalizedQuery, ignoreCase = true) ||
            item.location.contains(normalizedQuery, ignoreCase = true) ||
            item.category.contains(normalizedQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true)
        matchesQuery && matchesCategory
    }
}

internal data class PlaceItem(
    val id: Int,
    val title: String,
    val category: String,
    val description: String,
    val cityName: String,
    val address: String = "",
    val imageUrl: String = "",
    val mapsLink: String = "",
    val distanceLabel: String = "",
)

internal fun filterPlacesToVisit(
    items: List<PlaceItem>,
    query: String,
    selectedCity: String,
    selectedCategory: String,
): List<PlaceItem> {
    val normalizedQuery = query.trim()
    return items.filter { item ->
        val matchesQuery = normalizedQuery.isBlank() ||
            item.title.contains(normalizedQuery, ignoreCase = true) ||
            item.description.contains(normalizedQuery, ignoreCase = true) ||
            item.address.contains(normalizedQuery, ignoreCase = true) ||
            item.cityName.contains(normalizedQuery, ignoreCase = true)
        val matchesCity = selectedCity == "All" || item.cityName.equals(selectedCity, ignoreCase = true)
        val matchesCategory = selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true)
        matchesQuery && matchesCity && matchesCategory
    }
}

internal data class MarketplaceItem(
    val id: Int,
    val title: String,
    val description: String,
    val price: String,
    val transactionType: String,
    val category: String,
    val location: String,
    val imageUrl: String,
    val contactNumber: String,
    val ownerName: String,
    val ownerId: Int? = null,
    val url: String = "",
    val condition: String = "",
)

internal data class MarketplacePage(
    val items: List<MarketplaceItem>,
    val nextPage: Int?,
    val count: Int,
)

internal enum class MarketplaceTypeFilter(val label: String, val apiValue: String?) {
    ALL("All", null),
    FOR_SALE("For sale", "sale"),
    FOR_RENT("For rent", "rent"),
    EXCHANGE("Exchange", null),
}

private val marketplaceCategories = listOf(
    "All",
    "Books",
    "Electronics",
    "Furniture",
    "Bikes",
    "Stationery",
    "Other",
)

internal fun filterMarketplaceItems(
    items: List<MarketplaceItem>,
    query: String,
    selectedCategory: String,
    typeFilter: MarketplaceTypeFilter,
): List<MarketplaceItem> {
    val normalizedQuery = query.trim()
    return items.filter { item ->
        val matchesQuery = normalizedQuery.isBlank() ||
            item.title.contains(normalizedQuery, ignoreCase = true) ||
            item.description.contains(normalizedQuery, ignoreCase = true) ||
            item.location.contains(normalizedQuery, ignoreCase = true)

        val matchesCategory = selectedCategory == "All" ||
            item.category.equals(selectedCategory, ignoreCase = true) ||
            item.title.contains(selectedCategory, ignoreCase = true) ||
            item.description.contains(selectedCategory, ignoreCase = true)

        val matchesType = when (typeFilter) {
            MarketplaceTypeFilter.ALL -> true
            MarketplaceTypeFilter.FOR_SALE -> item.transactionType.equals("sale", ignoreCase = true)
            MarketplaceTypeFilter.FOR_RENT -> item.transactionType.equals("rent", ignoreCase = true)
            MarketplaceTypeFilter.EXCHANGE -> item.transactionType.equals("exchange", ignoreCase = true) ||
                item.title.contains("exchange", ignoreCase = true) ||
                item.description.contains("exchange", ignoreCase = true)
        }

        matchesQuery && matchesCategory && matchesType
    }
}

private const val MARKETPLACE_API_BASE_URL = "https://onetowncity.com"

private fun safeWebUri(rawUrl: String): Uri? {
    val uri = rawUrl.trim().toUri()
    return uri.takeIf { it.scheme.equals("https", ignoreCase = true) }
}

@Composable
private fun rememberOptimizedImageRequest(url: String): ImageRequest {
    val context = LocalContext.current
    return remember(url) {
        ImageRequest.Builder(context)
            .data(safeWebUri(url))
            .size(640)
            .crossfade(false)
            .build()
    }
}

private class BoundedItemCache<K, V>(private val maximumSize: Int) {
    private val values = LinkedHashMap<K, V>(maximumSize, 0.75f, true)

    @Synchronized
    operator fun get(key: K): V? = values[key]

    @Synchronized
    operator fun set(key: K, value: V) {
        values[key] = value
        while (values.size > maximumSize) values.remove(values.entries.iterator().next().key)
    }
}

private val marketplaceCache = BoundedItemCache<Int, MarketplaceItem>(200)
private val studentServicesCache = BoundedItemCache<Int, StudentServiceItem>(200)

private var marketplaceFetcher: suspend (String, String, MarketplaceTypeFilter, Int) -> MarketplacePage = { query, citySlug, typeFilter, page ->
    fetchMarketplaceListings(query, citySlug, typeFilter, page)
}

private fun buildMarketplaceUrl(query: String, citySlug: String, typeFilter: MarketplaceTypeFilter, page: Int, pageSize: Int): String {
    val encodedQuery = URLEncoder.encode(if (query.isBlank()) "" else query, "UTF-8")
    val encodedCity = citySlug.takeIf { it.isNotBlank() }?.let { URLEncoder.encode(it, "UTF-8") }
    return buildString {
        append(MARKETPLACE_API_BASE_URL)
        append("/api/v1/listings/property/?")
        if (encodedQuery.isNotBlank()) {
            append("q=")
            append(encodedQuery)
            append("&")
        }
        if (typeFilter != MarketplaceTypeFilter.ALL && typeFilter.apiValue != null) {
            append("property_type=")
            append(typeFilter.apiValue)
            append("&")
        }
        append("page=")
        append(page)
        append("&page_size=")
        append(pageSize)
        if (!encodedCity.isNullOrEmpty()) {
            append("&city=")
            append(encodedCity)
        }
    }
}

private suspend fun fetchMarketplaceListings(
    query: String,
    citySlug: String,
    typeFilter: MarketplaceTypeFilter,
    page: Int,
): MarketplacePage = withContext(Dispatchers.IO) {
    val url = URL(buildMarketplaceUrl(query, citySlug, typeFilter, page, 10))
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 15000
    connection.readTimeout = 15000
    connection.setRequestProperty("Accept", "application/json")
    connection.doInput = true

    val responseCode = connection.responseCode
    if (responseCode !in 200..299) {
        throw IllegalStateException("Unable to load marketplace listings. HTTP $responseCode")
    }

    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
    val json = JSONObject(responseText)
    val results = json.optJSONArray("results") ?: JSONArray()
    val nextLink = json.optString("next", "")
    val nextPage = if (nextLink.isBlank()) null else page + 1
    val items = mutableListOf<MarketplaceItem>()

    for (i in 0 until results.length()) {
        val result = results.getJSONObject(i)
        val title = result.optString("title", "Listing")
        val description = result.optString("description", "").ifBlank { "Student listing details coming soon." }
        val location = result.optString("location", "")
        val price = result.optString("price", "")
        val propertyType = result.optString("property_type", "sale")
        val contactNumber = result.optString("contact_number", "")
        val imageUrl = result.optString("display_image", "")
        val ownerPayload = result.optJSONObject("owner")
        val ownerName = ownerPayload?.optString("full_name") ?: "Seller"
        val ownerId = ownerPayload?.optInt("id")?.takeIf { it > 0 }
        val condition = result.optString("condition", "").ifBlank { "Used" }
        val item = MarketplaceItem(
            id = result.optInt("id"),
            title = title,
            description = description,
            price = if (price.isBlank()) "Price on request" else "₹${price.toBigDecimalOrNull()?.toPlainString() ?: price}",
            transactionType = propertyType.lowercase(),
            category = inferMarketplaceCategory(title, description),
            location = location.ifBlank { "Kuppam" },
            imageUrl = imageUrl,
            contactNumber = contactNumber,
            ownerName = ownerName,
            ownerId = ownerId,
            url = result.optString("url", ""),
            condition = condition,
        )
        items += item
        marketplaceCache[item.id] = item
    }

    MarketplacePage(items = items, nextPage = nextPage, count = json.optInt("count", items.size))
}

private fun inferMarketplaceCategory(title: String, description: String): String {
    val haystack = (title + " " + description).lowercase()
    return when {
        haystack.contains("book") -> "Books"
        haystack.contains("laptop") || haystack.contains("mobile") || haystack.contains("charger") || haystack.contains("tablet") -> "Electronics"
        haystack.contains("chair") || haystack.contains("table") || haystack.contains("bed") || haystack.contains("sofa") -> "Furniture"
        haystack.contains("bike") || haystack.contains("cycle") || haystack.contains("scooter") -> "Bikes"
        haystack.contains("notebook") || haystack.contains("pen") || haystack.contains("stationery") || haystack.contains("copy") -> "Stationery"
        else -> "Other"
    }
}

private suspend fun fetchStudentServices(query: String, citySlug: String, category: String, page: Int): List<StudentServiceItem> = withContext(Dispatchers.IO) {
    val encodedQuery = URLEncoder.encode(if (query.isBlank()) "" else query, "UTF-8")
    val encodedCity = citySlug.takeIf { it.isNotBlank() }?.let { URLEncoder.encode(it, "UTF-8") }
    val categoryParam = if (category == "All" || category.isBlank()) "" else "&category=${URLEncoder.encode(category, "UTF-8")}" 
    val urlString = buildString {
        append(MARKETPLACE_API_BASE_URL)
        append("/api/v1/listings/business/?")
        if (encodedQuery.isNotBlank()) {
            append("q=")
            append(encodedQuery)
            append("&")
        }
        append("page=")
        append(page)
        append("&page_size=10")
        if (!encodedCity.isNullOrEmpty()) {
            append("&city=")
            append(encodedCity)
        }
        append(categoryParam)
    }

    val url = URL(urlString)
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 15000
    connection.readTimeout = 15000
    connection.setRequestProperty("Accept", "application/json")
    connection.doInput = true

    val responseCode = connection.responseCode
    if (responseCode !in 200..299) {
        throw IllegalStateException("Unable to load student services. HTTP $responseCode")
    }

    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
    val json = JSONObject(responseText)
    val results = json.optJSONArray("results") ?: JSONArray()
    val items = mutableListOf<StudentServiceItem>()

    for (i in 0 until results.length()) {
        val result = results.getJSONObject(i)
        val city = result.optJSONObject("city")
        val cityName = city?.optString("name") ?: citySlug.ifBlank { "Kuppam" }
        val citySlugValue = city?.optString("slug") ?: citySlug.ifBlank { "kuppam" }
        val serviceCategory = result.optString("category", "other")
        val name = result.optString("name", "Service")
        val address = result.optString("address", "")
        val phoneNumber = result.optString("phone_number", "")
        val website = result.optString("website", "")
        val mapsLink = result.optString("maps_link", "")
        val imageUrl = result.optString("display_image", "")
        val description = result.optString("description", "").ifBlank { "Service information coming soon." }
        val item = StudentServiceItem(
            id = result.optInt("id"),
            name = name,
            category = serviceCategory,
            description = description,
            address = address.ifBlank { cityName },
            phoneNumber = phoneNumber,
            website = website,
            mapsLink = mapsLink,
            imageUrl = imageUrl,
            cityName = cityName,
            citySlug = citySlugValue,
        )
        studentServicesCache[item.id] = item
        items += item
    }

    items
}

@Composable
private fun StudentServicesFeatureScreen(navController: NavController) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var cityQuery by rememberSaveable { mutableStateOf("Kuppam") }
    var selectedCity by rememberSaveable { mutableStateOf<String?>(null) }
    var citySuggestions by remember { mutableStateOf<List<CitySuggestion>>(emptyList()) }
    var items by remember { mutableStateOf<List<StudentServiceItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var activeRequest by remember { mutableStateOf<Job?>(null) }
    val serviceCategories = listOf(
        "All",
        "school",
        "college",
        "pharmacy",
        "hospital",
        "transport",
        "restaurant",
        "grocery",
        "salon",
        "automobile",
        "other",
    )

    fun loadServices() {
        activeRequest?.cancel()
        activeRequest = coroutineScope.launch {
            isLoading = true
            error = null
            try {
                val loaded = fetchStudentServices(query, selectedCity ?: cityQuery.trim(), selectedCategory, 1)
                items = filterStudentServices(loaded, query, selectedCategory)
                error = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                items = emptyList()
                error = e.message ?: "Unable to load student services right now."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(query, selectedCategory, selectedCity ?: cityQuery) {
        kotlinx.coroutines.delay(300)
        loadServices()
    }

    LaunchedEffect(cityQuery) {
        kotlinx.coroutines.delay(300)
        if (cityQuery.isBlank()) {
            citySuggestions = emptyList()
        } else {
            citySuggestions = fetchCitySuggestionsSafely(cityQuery)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OneTownCityTopAppBar(
            title = "Student Services",
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavigationClick = { navController.popBackStack() },
        )

        OneTownCityTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search services",
            leadingIcon = Icons.Default.Search,
        )

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Location",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                OneTownCityTextField(
                    value = cityQuery,
                    onValueChange = { cityQuery = it; selectedCity = null },
                    placeholder = "City or area",
                    leadingIcon = Icons.Filled.LocationOn,
                )
                if (citySuggestions.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        citySuggestions.take(3).forEach { suggestion ->
                            OneTownCityButton(
                                text = suggestion.name,
                                onClick = {
                                    selectedCity = suggestion.slug
                                    cityQuery = suggestion.name
                                    citySuggestions = emptyList()
                                },
                                variant = OneTownCityButtonVariant.Outlined,
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            serviceCategories.forEach { category ->
                val selected = category == selectedCategory
                OneTownCityButton(
                    text = category.replaceFirstChar { it.titlecase() },
                    onClick = { selectedCategory = category },
                    variant = if (selected) OneTownCityButtonVariant.Secondary else OneTownCityButtonVariant.Outlined,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    OneTownCityCircularLoading(label = "Loading services")
                }
            }
            error != null -> {
                OneTownCityErrorState(
                    title = "Unable to load student services",
                    message = error ?: "Please try again later.",
                    actionText = "Retry",
                    onRetry = { loadServices() },
                )
            }
            items.isEmpty() -> {
                OneTownCityEmptyState(
                    title = "No student services found",
                    message = "Try a different keyword, city, or service category to find nearby support and care options.",
                    icon = Icons.Outlined.Build,
                    action = {
                        OneTownCityButton(
                            text = "Reset filters",
                            onClick = {
                                query = ""
                                selectedCategory = "All"
                                cityQuery = "Kuppam"
                                selectedCity = null
                                citySuggestions = emptyList()
                            },
                            variant = OneTownCityButtonVariant.Outlined,
                        )
                    },
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    itemsIndexed(items, key = { _, item -> item.id }) { _, item ->
                        StudentServiceCard(
                            item = item,
                            onClick = { navController.navigate("student-service/${item.id}") },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentServiceCard(
    item: StudentServiceItem,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (item.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = rememberOptimizedImageRequest(item.imageUrl),
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
                    Icon(
                        imageVector = Icons.Outlined.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.phoneNumber.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = "Call",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                maxLines = 1,
                            )
                        }
                    }
                }

                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = item.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (item.phoneNumber.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = item.phoneNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentServiceDetailScreen(
    navController: NavController,
    item: StudentServiceItem,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
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
                if (item.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = rememberOptimizedImageRequest(item.imageUrl),
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
                        Icon(
                            imageVector = Icons.Outlined.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (item.address.isNotBlank()) {
                            DetailRow(icon = Icons.Filled.LocationOn, title = item.address)
                        }
                        if (item.phoneNumber.isNotBlank()) {
                            DetailRow(icon = Icons.Filled.Call, title = item.phoneNumber)
                        }
                        if (item.website.isNotBlank()) {
                            DetailRow(icon = Icons.Filled.Language, title = item.website)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (item.phoneNumber.isNotBlank()) {
                        OneTownCityButton(
                            text = "Call",
                            onClick = {
                                val uri = Uri.parse("tel:${item.phoneNumber}")
                                val intent = Intent(Intent.ACTION_DIAL, uri)
                                activity?.startActivity(intent)
                            },
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
                val mapsUri = safeWebUri(item.mapsLink) ?: Uri.parse(
                    "https://www.google.com/maps/search/?api=1&query=${URLEncoder.encode(item.address.ifBlank { item.name }, "UTF-8")}" 
                )
                OneTownCityButton(
                    text = "Get directions",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, mapsUri)
                        try {
                            activity?.startActivity(intent)
                        } catch (_: ActivityNotFoundException) { }
                    },
                    leadingIcon = Icons.Filled.LocationOn,
                    variant = OneTownCityButtonVariant.Outlined,
                )
            }
        }
    }
}

@Composable
private fun ScholarshipsFeatureScreen(navController: NavController) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val categories = listOf("All", "Government", "Education", "Merit", "Need-based")
    val items = remember { emptyList<ScholarshipItem>() }

    fun loadScholarships() {
        isLoading = true
        error = null
        // The current backend exposes no scholarship or scheme model. We intentionally avoid inventing
        // program content or fake deadlines. This screen therefore resolves to a verified empty state.
        isLoading = false
        error = null
    }

    LaunchedEffect(query, selectedCategory) {
        loadScholarships()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OneTownCityTopAppBar(
            title = "Scholarships & Government Schemes",
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavigationClick = { navController.popBackStack() },
        )

        OneTownCityTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search scholarships and schemes",
            leadingIcon = Icons.Default.Search,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categories.forEach { category ->
                val selected = category == selectedCategory
                OneTownCityButton(
                    text = category,
                    onClick = { selectedCategory = category },
                    variant = if (selected) OneTownCityButtonVariant.Secondary else OneTownCityButtonVariant.Outlined,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    OneTownCityCircularLoading(label = "Loading scholarship data")
                }
            }
            error != null -> {
                OneTownCityErrorState(
                    title = "Unable to load scholarship data",
                    message = error ?: "Please try again later.",
                    actionText = "Retry",
                    onRetry = { loadScholarships() },
                )
            }
            items.isEmpty() -> {
                OneTownCityEmptyState(
                    title = "No verified scholarship data available",
                    message = "This backend does not currently expose scholarship or government-scheme records. We do not show guessed program details, deadlines, or benefits.",
                    icon = Icons.Outlined.EmojiEvents,
                    action = {
                        OneTownCityButton(
                            text = "Reset",
                            onClick = {
                                query = ""
                                selectedCategory = "All"
                            },
                            variant = OneTownCityButtonVariant.Outlined,
                        )
                    },
                )
            }
            else -> {
                val filtered = filterScholarships(items, query, selectedCategory)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    itemsIndexed(filtered, key = { _, item -> item.id }) { _, item ->
                        ScholarshipCard(item = item, onClick = { navController.navigate("scholarship/${item.id}") })
                    }
                }
            }
        }
    }
}

@Composable
private fun ScholarshipCard(
    item: ScholarshipItem,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.sourceType == "official") {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            text = "Official",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )

            if (item.deadline.isNotBlank()) {
                Text(
                    text = "Deadline: ${item.deadline}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (item.eligibility.isNotBlank()) {
                Text(
                    text = "Eligibility: ${item.eligibility}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (item.sourceLabel.isNotBlank()) {
                Text(
                    text = "Source: ${item.sourceLabel}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ScholarshipDetailScreen(
    navController: NavController,
    item: ScholarshipItem,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
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
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (item.sourceType == "official") "Official external information" else "OneTownCity reference",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (item.category.isNotBlank()) {
                            DetailRow(icon = Icons.Outlined.EmojiEvents, title = item.category)
                        }
                        if (item.deadline.isNotBlank()) {
                            DetailRow(icon = Icons.Filled.Info, title = "Deadline: ${item.deadline}")
                        }
                        if (item.eligibility.isNotBlank()) {
                            DetailRow(icon = Icons.Filled.Check, title = "Eligibility: ${item.eligibility}")
                        }
                        if (item.sourceLabel.isNotBlank()) {
                            DetailRow(icon = Icons.Filled.Language, title = item.sourceLabel)
                        }
                    }
                }
            }

            if (item.officialUrl.isNotBlank()) {
                item {
                    OneTownCityButton(
                        text = "Open official source",
                        onClick = {
                            safeWebUri(item.officialUrl)?.let { uri ->
                                try {
                                    activity?.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                } catch (_: ActivityNotFoundException) { }
                            }
                        },
                        leadingIcon = Icons.Filled.Language,
                        variant = OneTownCityButtonVariant.Primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun LostFoundFeatureScreen(navController: NavController) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val categories = listOf("All", "Lost", "Found")
    val items = remember { emptyList<LostFoundItem>() }

    fun loadLostFound() {
        isLoading = true
        error = null
        isLoading = false
    }

    LaunchedEffect(query, selectedCategory) {
        loadLostFound()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OneTownCityTopAppBar(
            title = "Lost & Found",
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavigationClick = { navController.popBackStack() },
        )

        OneTownCityTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search lost or found items",
            leadingIcon = Icons.Default.Search,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categories.forEach { category ->
                val selected = category == selectedCategory
                OneTownCityButton(
                    text = category,
                    onClick = { selectedCategory = category },
                    variant = if (selected) OneTownCityButtonVariant.Secondary else OneTownCityButtonVariant.Outlined,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    OneTownCityCircularLoading(label = "Loading lost & found reports")
                }
            }
            error != null -> {
                OneTownCityErrorState(
                    title = "Unable to load Lost & Found",
                    message = error ?: "Please try again later.",
                    actionText = "Retry",
                    onRetry = { loadLostFound() },
                )
            }
            items.isEmpty() -> {
                OneTownCityEmptyState(
                    title = "No verified lost or found items yet",
                    message = "This community feature is not currently backed by a live lost-and-found listing source. We do not expose guessed ownership details or unverifiable contact data.",
                    icon = Icons.Outlined.Search,
                    action = {
                        OneTownCityButton(
                            text = "Reset",
                            onClick = {
                                query = ""
                                selectedCategory = "All"
                            },
                            variant = OneTownCityButtonVariant.Outlined,
                        )
                    },
                )
            }
            else -> {
                val filtered = filterLostFound(items, query, selectedCategory)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    itemsIndexed(filtered, key = { _, item -> item.id }) { _, item ->
                        LostFoundCard(item = item, onClick = { navController.navigate("lost-found/${item.id}") })
                    }
                }
            }
        }
    }
}

@Composable
private fun LostFoundCard(
    item: LostFoundItem,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (item.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = rememberOptimizedImageRequest(item.imageUrl),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    contentScale = ContentScale.Crop,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.date,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            if (item.location.isNotBlank()) {
                DetailRow(icon = Icons.Filled.LocationOn, title = item.location)
            }
        }
    }
}

@Composable
private fun LostFoundDetailScreen(
    navController: NavController,
    item: LostFoundItem,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
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
                if (item.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = rememberOptimizedImageRequest(item.imageUrl),
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(text = item.title, style = MaterialTheme.typography.headlineSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text(text = item.category, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(text = item.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (item.location.isNotBlank()) DetailRow(icon = Icons.Filled.LocationOn, title = item.location)
                        if (item.date.isNotBlank()) DetailRow(icon = Icons.Filled.DateRange, title = item.date)
                        if (item.cityName.isNotBlank()) DetailRow(icon = Icons.Filled.LocationOn, title = item.cityName)
                        if (item.ownerInfo.isNotBlank()) DetailRow(icon = Icons.Filled.Person, title = item.ownerInfo)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (item.contact.isNotBlank()) {
                        OneTownCityButton(
                            text = "Contact",
                            onClick = {
                                val uri = Uri.parse("tel:${item.contact}")
                                val intent = Intent(Intent.ACTION_DIAL, uri)
                                activity?.startActivity(intent)
                            },
                            leadingIcon = Icons.Filled.Phone,
                            variant = OneTownCityButtonVariant.Primary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (item.reportUrl.isNotBlank()) {
                        OneTownCityButton(
                            text = "Report",
                            onClick = {
                                safeWebUri(item.reportUrl)?.let { uri ->
                                    try {
                                        activity?.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    } catch (_: ActivityNotFoundException) { }
                                }
                            },
                            leadingIcon = Icons.Filled.Warning,
                            variant = OneTownCityButtonVariant.Secondary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlacesToVisitFeatureScreen(navController: NavController) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCity by rememberSaveable { mutableStateOf("All") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var permissionState by remember { mutableStateOf(LocationPermissionState.UNAVAILABLE) }
    val cities = listOf("All", "Kuppam")
    val categories = listOf("All", "Nature", "Heritage", "Education", "Food", "Relaxation")
    val items = remember { emptyList<PlaceItem>() }

    fun loadPlaces() {
        isLoading = true
        error = null
        isLoading = false
    }

    LaunchedEffect(query, selectedCity, selectedCategory) {
        loadPlaces()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OneTownCityTopAppBar(
            title = "Places to Visit",
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavigationClick = { navController.popBackStack() },
        )

        OneTownCityTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search places",
            leadingIcon = Icons.Default.Search,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            cities.forEach { city ->
                val selected = city == selectedCity
                OneTownCityButton(
                    text = city,
                    onClick = { selectedCity = city },
                    variant = if (selected) OneTownCityButtonVariant.Secondary else OneTownCityButtonVariant.Outlined,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categories.forEach { category ->
                val selected = category == selectedCategory
                OneTownCityButton(
                    text = category,
                    onClick = { selectedCategory = category },
                    variant = if (selected) OneTownCityButtonVariant.Secondary else OneTownCityButtonVariant.Outlined,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        when (permissionState) {
            LocationPermissionState.GRANTED -> {
                Text(
                    text = "Location permission granted. Nearby suggestions can be shown when available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LocationPermissionState.DENIED -> {
                Text(
                    text = "Location permission denied. Browsing still works using city and category filters.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LocationPermissionState.REVOKED -> {
                Text(
                    text = "Location permission is unavailable. Browsing remains available using your chosen city.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LocationPermissionState.UNAVAILABLE -> {
                Text(
                    text = "Location unavailable on this device. City-based browsing remains available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    OneTownCityCircularLoading(label = "Loading places")
                }
            }
            error != null -> {
                OneTownCityErrorState(
                    title = "Unable to load places",
                    message = error ?: "Please try again later.",
                    actionText = "Retry",
                    onRetry = { loadPlaces() },
                )
            }
            items.isEmpty() -> {
                OneTownCityEmptyState(
                    title = "No verified places available",
                    message = "The current backend does not expose a live places-to-visit dataset for this city. We do not invent destination details or fake distances.",
                    icon = Icons.Outlined.LocationOn,
                    action = {
                        OneTownCityButton(
                            text = "Reset",
                            onClick = {
                                query = ""
                                selectedCity = "All"
                                selectedCategory = "All"
                            },
                            variant = OneTownCityButtonVariant.Outlined,
                        )
                    },
                )
            }
            else -> {
                val filtered = filterPlacesToVisit(items, query, selectedCity, selectedCategory)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    itemsIndexed(filtered, key = { _, item -> item.id }) { _, item ->
                        PlaceCard(item = item, onClick = { navController.navigate("place/${item.id}") })
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceCard(
    item: PlaceItem,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (item.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = rememberOptimizedImageRequest(item.imageUrl),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    contentScale = ContentScale.Crop,
                )
            }
            Text(
                text = item.category,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.distanceLabel.isNotBlank()) {
                Text(
                    text = item.distanceLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.address.isNotBlank()) {
                DetailRow(icon = Icons.Filled.LocationOn, title = item.address)
            }
        }
    }
}

@Composable
private fun PlaceDetailScreen(
    navController: NavController,
    item: PlaceItem,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
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
                if (item.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = rememberOptimizedImageRequest(item.imageUrl),
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(text = item.title, style = MaterialTheme.typography.headlineSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text(text = item.category, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(text = item.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (item.address.isNotBlank()) DetailRow(icon = Icons.Filled.LocationOn, title = item.address)
                        if (item.cityName.isNotBlank()) DetailRow(icon = Icons.Filled.LocationOn, title = item.cityName)
                        if (item.distanceLabel.isNotBlank()) DetailRow(icon = Icons.Filled.LocationOn, title = item.distanceLabel)
                    }
                }
            }

            if (item.mapsLink.isNotBlank()) {
                item {
                    OneTownCityButton(
                        text = "Open in maps",
                        onClick = {
                            safeWebUri(item.mapsLink)?.let { uri ->
                                try {
                                    activity?.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                } catch (_: ActivityNotFoundException) { }
                            }
                        },
                        leadingIcon = Icons.Filled.LocationOn,
                        variant = OneTownCityButtonVariant.Primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketplaceFeatureScreen(navController: NavController) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var selectedType by rememberSaveable { mutableStateOf(MarketplaceTypeFilter.ALL) }
    var cityQuery by rememberSaveable { mutableStateOf("Kuppam") }
    var selectedCity by rememberSaveable { mutableStateOf<String?>(null) }
    var citySuggestions by remember { mutableStateOf<List<CitySuggestion>>(emptyList()) }
    var items by remember { mutableStateOf<List<MarketplaceItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableStateOf(1) }
    val coroutineScope = rememberCoroutineScope()
    var activeRequest by remember { mutableStateOf<Job?>(null) }
    val listState = rememberLazyListState()

    fun loadPage(reset: Boolean = false) {
        activeRequest?.cancel()
        activeRequest = coroutineScope.launch {
            if (reset) {
                isLoading = true
                isLoadingMore = false
                page = 1
                error = null
            } else {
                if (!hasMore || isLoadingMore) return@launch
                isLoadingMore = true
            }

            try {
                val result = marketplaceFetcher(query, selectedCity ?: cityQuery.trim(), selectedType, if (reset) 1 else page)
                val nextItems = if (reset) result.items else items + result.items
                items = filterMarketplaceItems(nextItems, query, selectedCategory, selectedType).distinctBy { it.id }
                hasMore = result.nextPage != null
                page = result.nextPage ?: ((if (reset) 1 else page) + 1)
                error = null
            } catch (e: Exception) {
                if (reset) {
                    items = emptyList()
                    error = e.message ?: "Unable to load marketplace listings."
                } else {
                    error = e.message ?: "Unable to load more listings."
                }
            } finally {
                isLoading = false
                isLoadingMore = false
            }
        }
    }

    LaunchedEffect(query, selectedCategory, selectedType, selectedCity ?: cityQuery) {
        kotlinx.coroutines.delay(300)
        loadPage(reset = true)
    }

    LaunchedEffect(cityQuery) {
        kotlinx.coroutines.delay(300)
        if (cityQuery.isBlank()) {
            citySuggestions = emptyList()
        } else {
            citySuggestions = fetchCitySuggestionsSafely(cityQuery)
        }
    }

    LaunchedEffect(listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index) {
        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        if (!isLoading && !isLoadingMore && hasMore && lastVisibleIndex >= items.size - 3) {
            loadPage(reset = false)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OneTownCityTopAppBar(
            title = "Buy / Sell / Exchange",
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavigationClick = { navController.popBackStack() },
        )

        OneTownCityTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search student marketplace",
            leadingIcon = Icons.Default.Search,
        )

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Location & filters",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                OneTownCityTextField(
                    value = cityQuery,
                    onValueChange = { cityQuery = it; selectedCity = null },
                    placeholder = "City or area",
                    leadingIcon = Icons.Filled.LocationOn,
                )
                if (citySuggestions.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        citySuggestions.take(3).forEach { suggestion ->
                            OneTownCityButton(
                                text = suggestion.name,
                                onClick = {
                                    selectedCity = suggestion.slug
                                    cityQuery = suggestion.name
                                    citySuggestions = emptyList()
                                },
                                variant = OneTownCityButtonVariant.Outlined,
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MarketplaceTypeFilter.entries.forEach { filter ->
                        val selected = filter == selectedType
                        OneTownCityButton(
                            text = filter.label,
                            onClick = { selectedType = filter },
                            variant = if (selected) OneTownCityButtonVariant.Primary else OneTownCityButtonVariant.Outlined,
                        )
                    }
                }
            }
        }

        if (marketplaceCategories.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                marketplaceCategories.forEach { category ->
                    val selected = category == selectedCategory
                    OneTownCityButton(
                        text = category,
                        onClick = { selectedCategory = category },
                        variant = if (selected) OneTownCityButtonVariant.Secondary else OneTownCityButtonVariant.Outlined,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    OneTownCityCircularLoading(label = "Loading listings")
                }
            }
            error != null -> {
                OneTownCityErrorState(
                    title = "Unable to load marketplace",
                    message = error ?: "Please try again later.",
                    actionText = "Retry",
                    onRetry = { loadPage(reset = true) },
                )
            }
            items.isEmpty() -> {
                OneTownCityEmptyState(
                    title = "No marketplace listings found",
                    message = "Try a different keyword, category, or city to discover student items and exchanges near you.",
                    icon = Icons.Outlined.ShoppingCart,
                    action = {
                        OneTownCityButton(
                            text = "Reset filters",
                            onClick = {
                                query = ""
                                selectedCategory = "All"
                                selectedType = MarketplaceTypeFilter.ALL
                                cityQuery = "Kuppam"
                                selectedCity = null
                                citySuggestions = emptyList()
                            },
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
                    itemsIndexed(items, key = { _, item -> item.id }) { _, item ->
                        MarketplaceCard(
                            item = item,
                            onClick = { navController.navigate("marketplace/${item.id}") },
                        )
                    }
                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
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
private fun MarketplaceCard(
    item: MarketplaceItem,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
                    Icon(
                        imageVector = Icons.Outlined.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            text = if (item.transactionType.equals("rent", true)) "For rent" else if (item.transactionType.equals("sale", true)) "For sale" else "Exchange",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            maxLines = 1,
                        )
                    }
                }

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = item.price,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = item.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (item.contactNumber.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = item.contactNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketplaceDetailScreen(
    navController: NavController,
    item: MarketplaceItem,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
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
                        Icon(
                            imageVector = Icons.Outlined.ShoppingCart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = item.price,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Seller information",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (item.ownerName.isNotBlank()) {
                            DetailRow(icon = Icons.Filled.Person, title = item.ownerName)
                        }
                        if (item.location.isNotBlank()) {
                            DetailRow(icon = Icons.Filled.LocationOn, title = item.location)
                        }
                        if (item.contactNumber.isNotBlank()) {
                            DetailRow(icon = Icons.Filled.Phone, title = item.contactNumber)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (item.contactNumber.isNotBlank()) {
                        OneTownCityButton(
                            text = "Call seller",
                            onClick = {
                                val uri = Uri.parse("tel:${item.contactNumber}")
                                val intent = Intent(Intent.ACTION_DIAL, uri)
                                activity?.startActivity(intent)
                            },
                            leadingIcon = Icons.Filled.Call,
                            variant = OneTownCityButtonVariant.Primary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (item.url.isNotBlank()) {
                        OneTownCityButton(
                            text = "View listing",
                            onClick = {
                                safeWebUri(item.url)?.let { uri ->
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
        }
    }
}

private data class EventItem(
    val id: Int,
    val title: String,
    val description: String,
    val location: String,
    val contactNumber: String,
    val eventDate: String,
    val imageUrl: String,
    val cityName: String,
    val citySlug: String,
    val detailUrl: String,
)

private data class EventsPage(
    val items: List<EventItem>,
    val nextPage: Int?,
    val count: Int,
)

private const val EVENTS_API_BASE_URL = "https://onetowncity.com"
private val eventCache = BoundedItemCache<Int, EventItem>(200)

private var eventsFetcher: suspend (String, String, Int) -> EventsPage = { query, citySlug, page ->
    fetchEvents(query, citySlug, page)
}

private fun buildEventsUrl(query: String, citySlug: String, page: Int, pageSize: Int): String {
    val encodedQuery = URLEncoder.encode(if (query.isBlank()) "" else query, "UTF-8")
    val encodedCity = citySlug.takeIf { it.isNotBlank() }?.let { URLEncoder.encode(it, "UTF-8") }
    return buildString {
        append(EVENTS_API_BASE_URL)
        append("/api/v1/listings/event/?")
        if (encodedQuery.isNotBlank()) {
            append("q=")
            append(encodedQuery)
            append("&")
        }
        append("page=")
        append(page)
        append("&page_size=")
        append(pageSize)
        append("&upcoming=true")
        if (!encodedCity.isNullOrEmpty()) {
            append("&city=")
            append(encodedCity)
        }
    }
}

private suspend fun fetchEvents(
    query: String,
    citySlug: String,
    page: Int,
): EventsPage = withContext(Dispatchers.IO) {
    val url = URL(buildEventsUrl(query, citySlug, page, 10))
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 15000
    connection.readTimeout = 15000
    connection.setRequestProperty("Accept", "application/json")
    connection.doInput = true

    val responseCode = connection.responseCode
    if (responseCode !in 200..299) {
        throw IllegalStateException("Unable to load events. HTTP $responseCode")
    }

    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
    val json = JSONObject(responseText)
    val results = json.optJSONArray("results") ?: JSONArray()
    val nextLink = json.optString("next", "")
    val nextPage = if (nextLink.isBlank()) null else page + 1
    val items = mutableListOf<EventItem>()

    for (i in 0 until results.length()) {
        val result = results.getJSONObject(i)
        val city = result.optJSONObject("city")
        val cityName = city?.optString("name") ?: citySlug.ifBlank { "Kuppam" }
        val citySlugValue = city?.optString("slug") ?: citySlug.ifBlank { "kuppam" }
        val title = result.optString("title", "Event")
        val description = result.optString("description", "")
        val location = result.optString("location", "")
        val contactNumber = result.optString("contact_number", "")
        val eventDate = result.optString("event_date", "")
        val imageUrl = result.optString("display_image", "")
        val detailUrl = result.optString("url", "")

        val item = EventItem(
            id = result.optInt("id"),
            title = title,
            description = description.ifBlank { "Local event details coming soon." },
            location = location.ifBlank { cityName },
            contactNumber = contactNumber,
            eventDate = eventDate,
            imageUrl = imageUrl,
            cityName = cityName,
            citySlug = citySlugValue,
            detailUrl = detailUrl,
        )
        items += item
        eventCache[item.id] = item
    }

    EventsPage(items = items, nextPage = nextPage, count = json.optInt("count", items.size))
}

private enum class EventDateFilter(val label: String) {
    ALL("All"),
    THIS_WEEK("This week"),
    THIS_MONTH("This month"),
    UPCOMING("Upcoming"),
}

private fun matchesDateFilter(dateValue: String, filter: EventDateFilter): Boolean {
    if (dateValue.isBlank()) return true

    val parsedDate = try {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        formatter.isLenient = false
        formatter.parse(dateValue)
    } catch (_: Exception) {
        return true
    } ?: return true

    val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getDefault(), java.util.Locale.getDefault())
    val today = calendar.clone() as java.util.Calendar
    val eventDate = java.util.Calendar.getInstance(java.util.TimeZone.getDefault(), java.util.Locale.getDefault())
    eventDate.time = parsedDate

    return when (filter) {
        EventDateFilter.ALL -> true
        EventDateFilter.THIS_WEEK -> !eventDate.before(today) && !eventDate.after(addDays(today, 7))
        EventDateFilter.THIS_MONTH -> !eventDate.before(today) && !eventDate.after(addDays(today, 30))
        EventDateFilter.UPCOMING -> !eventDate.before(today)
    }
}

private fun addDays(calendar: java.util.Calendar, days: Int): java.util.Calendar {
    val copy = calendar.clone() as java.util.Calendar
    copy.add(java.util.Calendar.DAY_OF_YEAR, days)
    return copy
}

private fun formatEventDate(dateValue: String): String {
    if (dateValue.isBlank()) return "Date coming soon"
    return try {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        formatter.isLenient = false
        val parsed = formatter.parse(dateValue) ?: return dateValue
        java.text.SimpleDateFormat("EEE, d MMM", java.util.Locale.getDefault()).format(parsed)
    } catch (_: Exception) {
        dateValue
    }
}

private fun formatEventDateFull(dateValue: String): String {
    if (dateValue.isBlank()) return "Date coming soon"
    return try {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        formatter.isLenient = false
        val parsed = formatter.parse(dateValue) ?: return dateValue
        java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale.getDefault()).format(parsed)
    } catch (_: Exception) {
        dateValue
    }
}

private fun formatEventTime(dateValue: String): String {
    if (dateValue.isBlank()) return "Time TBA"
    return try {
        if (dateValue.contains("T")) {
            val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            parser.isLenient = false
            val parsed = parser.parse(dateValue) ?: return "Time TBA"
            java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(parsed)
        } else {
            "Time TBA"
        }
    } catch (_: Exception) {
        "Time TBA"
    }
}

@Composable
private fun EventsFeatureScreen(navController: NavController) {
    var query by rememberSaveable { mutableStateOf("") }
    var cityQuery by rememberSaveable { mutableStateOf("Kuppam") }
    var selectedCity by rememberSaveable { mutableStateOf<String?>(null) }
    var dateFilter by rememberSaveable { mutableStateOf(EventDateFilter.UPCOMING) }
    var citySuggestions by remember { mutableStateOf<List<CitySuggestion>>(emptyList()) }
    var items by remember { mutableStateOf<List<EventItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableStateOf(1) }
    val coroutineScope = rememberCoroutineScope()
    var activeRequest by remember { mutableStateOf<Job?>(null) }
    val listState = rememberLazyGridState()

    fun loadPage(reset: Boolean = false) {
        activeRequest?.cancel()
        activeRequest = coroutineScope.launch {
            if (reset) {
                isLoading = true
                isLoadingMore = false
                page = 1
                error = null
            } else {
                if (!hasMore || isLoadingMore) return@launch
                isLoadingMore = true
            }

            try {
                val citySlug = selectedCity ?: cityQuery.trim()
                val pageToLoad = if (reset) 1 else page
                val result = eventsFetcher(query, citySlug, pageToLoad)
                if (reset) {
                    items = result.items.filter { matchesDateFilter(it.eventDate, dateFilter) }
                } else {
                    items = (items + result.items).distinctBy { it.id }.filter { matchesDateFilter(it.eventDate, dateFilter) }
                }
                hasMore = result.nextPage != null
                page = result.nextPage ?: (pageToLoad + 1)
                error = null
            } catch (e: Exception) {
                if (reset) {
                    items = emptyList()
                    error = e.message ?: "Unable to load events right now."
                } else {
                    error = e.message ?: "Unable to load more events."
                }
            } finally {
                isLoading = false
                isLoadingMore = false
            }
        }
    }

    LaunchedEffect(query, selectedCity ?: cityQuery, dateFilter) {
        kotlinx.coroutines.delay(300)
        loadPage(reset = true)
    }

    LaunchedEffect(cityQuery) {
        kotlinx.coroutines.delay(300)
        if (cityQuery.isBlank()) {
            citySuggestions = emptyList()
        } else {
            citySuggestions = fetchCitySuggestions(cityQuery)
        }
    }

    LaunchedEffect(listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index) {
        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        if (!isLoading && !isLoadingMore && hasMore && lastVisibleIndex >= items.size - 3) {
            loadPage(reset = false)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OneTownCityTopAppBar(
            title = "Events",
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavigationClick = { navController.popBackStack() },
        )

        OneTownCityTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search events",
            leadingIcon = Icons.Default.Search,
        )

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Location & date",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                OneTownCityTextField(
                    value = cityQuery,
                    onValueChange = { cityQuery = it; selectedCity = null },
                    placeholder = "City or area",
                    leadingIcon = Icons.Filled.LocationOn,
                )
                if (citySuggestions.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        citySuggestions.take(3).forEach { suggestion ->
                            OneTownCityButton(
                                text = suggestion.name,
                                onClick = {
                                    selectedCity = suggestion.slug
                                    cityQuery = suggestion.name
                                    citySuggestions = emptyList()
                                },
                                variant = OneTownCityButtonVariant.Outlined,
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    EventDateFilter.entries.forEach { filter ->
                        val selected = filter == dateFilter
                        OneTownCityButton(
                            text = filter.label,
                            onClick = { dateFilter = filter },
                            variant = if (selected) OneTownCityButtonVariant.Primary else OneTownCityButtonVariant.Outlined,
                        )
                    }
                }
            }
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    OneTownCityCircularLoading(label = "Loading events")
                }
            }
            error != null -> {
                OneTownCityErrorState(
                    title = "Unable to load events",
                    message = error ?: "Please try again later.",
                    actionText = "Retry",
                    onRetry = { loadPage(reset = true) },
                )
            }
            items.isEmpty() -> {
                OneTownCityEmptyState(
                    title = "No events found",
                    message = "Try a different city, keyword, or date filter to discover upcoming opportunities in your area.",
                    icon = Icons.Outlined.Event,
                    action = {
                        OneTownCityButton(
                            text = "Reset filters",
                            onClick = {
                                query = ""
                                cityQuery = "Kuppam"
                                selectedCity = null
                                dateFilter = EventDateFilter.UPCOMING
                                citySuggestions = emptyList()
                            },
                            variant = OneTownCityButtonVariant.Outlined,
                        )
                    },
                )
            }
            else -> {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val columns = when {
                        maxWidth < 420.dp -> 1
                        maxWidth < 840.dp -> 2
                        else -> 3
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        itemsIndexed(items, key = { _, item -> item.id }) { _, item ->
                            EventCard(
                                item = item,
                                onClick = { navController.navigate("event/${item.id}") },
                            )
                        }
                        if (isLoadingMore) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    OneTownCityCircularLoading(label = "Loading more")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCard(
    item: EventItem,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = item.location,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (item.description.isNotBlank()) {
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    modifier = Modifier.widthIn(min = 90.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = formatEventDate(item.eventDate).split(",").firstOrNull() ?: "Date",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = formatEventDate(item.eventDate).removePrefix(formatEventDate(item.eventDate).split(",").firstOrNull() ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                        )
                        Text(
                            text = formatEventTime(item.eventDate),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventDetailScreen(
    navController: NavController,
    item: EventItem,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        OneTownCityTopAppBar(
            title = "Event",
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavigationClick = { navController.popBackStack() },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        ) {
            item {
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
                        Icon(
                            imageVector = Icons.Outlined.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                modifier = Modifier.widthIn(min = 110.dp),
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = formatEventDate(item.eventDate).split(",").firstOrNull() ?: "Date",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = formatEventDate(item.eventDate).removePrefix(formatEventDate(item.eventDate).split(",").firstOrNull() ?: ""),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = formatEventTime(item.eventDate),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = formatEventDateFull(item.eventDate),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = item.location,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (item.location.isNotBlank()) {
                            DetailRow(icon = Icons.Filled.LocationOn, title = item.location)
                        }
                        if (item.contactNumber.isNotBlank()) {
                            DetailRow(icon = Icons.Filled.Call, title = item.contactNumber)
                        }
                        if (item.detailUrl.isNotBlank()) {
                            DetailRow(icon = Icons.Filled.Language, title = item.detailUrl)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (item.contactNumber.isNotBlank()) {
                        OneTownCityButton(
                            text = "Call organizer",
                            onClick = {
                                val uri = Uri.parse("tel:${item.contactNumber}")
                                val intent = Intent(Intent.ACTION_DIAL, uri)
                                activity?.startActivity(intent)
                            },
                            leadingIcon = Icons.Filled.Call,
                            variant = OneTownCityButtonVariant.Primary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (item.detailUrl.isNotBlank()) {
                        OneTownCityButton(
                            text = "Register",
                            onClick = {
                                safeWebUri(item.detailUrl)?.let { uri ->
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
        }
    }
}

private data class TuitionCenterItem(
    val id: Int,
    val name: String,
    val category: String,
    val subtitle: String,
    val description: String,
    val phoneNumber: String,
    val website: String,
    val mapsLink: String,
    val imageUrl: String,
    val cityName: String,
    val citySlug: String,
)

private data class TuitionCentersPage(
    val items: List<TuitionCenterItem>,
    val nextPage: Int?,
    val count: Int,
)

private data class CitySuggestion(
    val slug: String,
    val name: String,
)

private const val TUITION_API_BASE_URL = "https://onetowncity.com"
private val tuitionCenterCache = BoundedItemCache<Int, TuitionCenterItem>(200)

private var tuitionCentersFetcher: suspend (String, String, Int) -> TuitionCentersPage = { query, citySlug, page ->
    fetchTuitionCenters(query, citySlug, page)
}

private var citySuggestionsFetcher: suspend (String) -> List<CitySuggestion> = { query ->
    fetchCitySuggestions(query)
}

private fun buildTuitionCentersUrl(query: String, citySlug: String, page: Int, pageSize: Int): String {
    val resolvedQuery = if (query.isBlank()) "tuition" else query
    val encodedQuery = URLEncoder.encode(resolvedQuery, "UTF-8")
    val encodedCity = citySlug.takeIf { it.isNotBlank() }?.let { URLEncoder.encode(it, "UTF-8") }
    return buildString {
        append(TUITION_API_BASE_URL)
        append("/api/v1/listings/business/?q=")
        append(encodedQuery)
        append("&page=")
        append(page)
        append("&page_size=")
        append(pageSize)
        if (!encodedCity.isNullOrEmpty()) {
            append("&city=")
            append(encodedCity)
        }
    }
}

private suspend fun fetchTuitionCenters(
    query: String,
    citySlug: String,
    page: Int,
): TuitionCentersPage = withContext(Dispatchers.IO) {
    val url = URL(buildTuitionCentersUrl(query, citySlug, page, 10))
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 15000
    connection.readTimeout = 15000
    connection.setRequestProperty("Accept", "application/json")
    connection.doInput = true

    val responseCode = connection.responseCode
    if (responseCode !in 200..299) {
        throw IllegalStateException("Unable to load tuition centers. HTTP $responseCode")
    }

    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
    val json = JSONObject(responseText)
    val results = json.optJSONArray("results") ?: JSONArray()
    val nextLink = json.optString("next", "")
    val nextPage = if (nextLink.isBlank()) null else page + 1
    val items = mutableListOf<TuitionCenterItem>()

    for (i in 0 until results.length()) {
        val result = results.getJSONObject(i)
        val city = result.optJSONObject("city")
        val cityName = city?.optString("name") ?: "Kuppam"
        val citySlugValue = city?.optString("slug") ?: citySlug
        val name = result.optString("name", "Tuition Center")
        val category = result.optString("category", "education")
        val address = result.optString("address", "")
        val phone = result.optString("phone_number", "")
        val description = result.optString("description", "")
        val website = result.optString("website", "")
        val mapsLink = result.optString("maps_link", "")
        val imageUrl = result.optString("display_image", "")
        val item = TuitionCenterItem(
            id = result.optInt("id"),
            name = name,
            category = category,
            subtitle = address.ifBlank { cityName },
            description = description.ifBlank { "Academic support and coaching options in your area." },
            phoneNumber = phone,
            website = website,
            mapsLink = mapsLink,
            imageUrl = imageUrl,
            cityName = cityName,
            citySlug = citySlugValue,
        )
        items += item
        tuitionCenterCache[item.id] = item
    }

    TuitionCentersPage(items = items, nextPage = nextPage, count = json.optInt("count", items.size))
}

private suspend fun fetchCitySuggestions(cityQuery: String): List<CitySuggestion> = withContext(Dispatchers.IO) {
    if (cityQuery.isBlank()) return@withContext emptyList()
    val encoded = URLEncoder.encode(cityQuery, "UTF-8")
    val url = URL("${TUITION_API_BASE_URL}/api/v1/locations/cities/?q=$encoded")
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 15000
    connection.readTimeout = 15000
    connection.setRequestProperty("Accept", "application/json")
    connection.doInput = true
    val responseCode = connection.responseCode
    if (responseCode !in 200..299) return@withContext emptyList()
    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
    val array = JSONArray(responseText)
    val suggestions = mutableListOf<CitySuggestion>()
    for (i in 0 until array.length()) {
        val item = array.getJSONObject(i)
        val slug = item.optString("slug", "")
        val name = item.optString("name", "")
        if (slug.isNotBlank() && name.isNotBlank()) {
            suggestions += CitySuggestion(slug = slug, name = name)
        }
    }
    suggestions.take(5)
}

private suspend fun fetchCitySuggestionsSafely(
    cityQuery: String,
    fetcher: suspend (String) -> List<CitySuggestion>,
): List<CitySuggestion> = try {
    fetcher(cityQuery)
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    emptyList()
}

private suspend fun fetchCitySuggestionsSafely(cityQuery: String): List<CitySuggestion> =
    fetchCitySuggestionsSafely(cityQuery, ::fetchCitySuggestions)

@Composable
internal fun TuitionCentersFeatureScreen(navController: NavController) {
    var query by rememberSaveable { mutableStateOf("") }
    var cityQuery by rememberSaveable { mutableStateOf("Kuppam") }
    var selectedCity by rememberSaveable { mutableStateOf<CitySuggestion?>(null) }
    var citySuggestions by remember { mutableStateOf<List<CitySuggestion>>(emptyList()) }
    var items by remember { mutableStateOf<List<TuitionCenterItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableStateOf(1) }
    val coroutineScope = rememberCoroutineScope()
    var activeRequest by remember { mutableStateOf<Job?>(null) }
    val listState = rememberLazyListState()

    fun loadPage(reset: Boolean = false) {
        activeRequest?.cancel()
        activeRequest = coroutineScope.launch {
            if (reset) {
                isLoading = true
                isLoadingMore = false
                page = 1
                error = null
            } else {
                if (!hasMore || isLoadingMore) return@launch
                isLoadingMore = true
            }
            try {
                val pageToLoad = if (reset) 1 else page
                val result = tuitionCentersFetcher(query, selectedCity?.slug ?: cityQuery.trim(), pageToLoad)
                if (reset) {
                    items = result.items
                } else {
                    items = items + result.items
                }
                hasMore = result.nextPage != null
                page = result.nextPage ?: (pageToLoad + 1)
                error = null
            } catch (e: Exception) {
                if (reset) {
                    items = emptyList()
                    error = e.message ?: "Unable to load tuition centers right now."
                } else {
                    error = e.message ?: "Unable to load more tuition centers."
                }
            } finally {
                isLoading = false
                isLoadingMore = false
            }
        }
    }

    LaunchedEffect(query, selectedCity?.slug ?: cityQuery) {
        kotlinx.coroutines.delay(300)
        loadPage(reset = true)
    }

    LaunchedEffect(cityQuery) {
        kotlinx.coroutines.delay(300)
        citySuggestions = citySuggestionsFetcher(cityQuery)
    }

    LaunchedEffect(listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index) {
        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        if (!isLoading && !isLoadingMore && hasMore && lastVisibleIndex >= items.size - 3) {
            loadPage(reset = false)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OneTownCityTopAppBar(
            title = "Tuition Centers",
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavigationClick = { navController.popBackStack() },
        )

        OneTownCityTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search tuition centers",
            leadingIcon = Icons.Default.Search,
        )

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Location aware",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                OneTownCityTextField(
                    value = cityQuery,
                    onValueChange = { cityQuery = it; selectedCity = null },
                    placeholder = "City or area",
                    leadingIcon = Icons.Filled.LocationOn,
                )
                if (citySuggestions.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        citySuggestions.take(3).forEach { suggestion ->
                            OneTownCityButton(
                                text = suggestion.name,
                                onClick = {
                                    selectedCity = suggestion
                                    cityQuery = suggestion.name
                                    citySuggestions = emptyList()
                                },
                                variant = OneTownCityButtonVariant.Outlined,
                            )
                        }
                    }
                }
                if (selectedCity != null || cityQuery.isNotBlank()) {
                    Text(
                        text = "Filter: ${selectedCity?.name ?: cityQuery}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    OneTownCityCircularLoading(label = "Loading tuition centers")
                }
            }
            error != null -> {
                OneTownCityErrorState(
                    title = "Unable to load tuition centers",
                    message = error ?: "Please try again later.",
                    actionText = "Retry",
                    onRetry = { loadPage(reset = true) },
                )
            }
            items.isEmpty() -> {
                OneTownCityEmptyState(
                    title = "No tuition centers found",
                    message = "No nearby coaching or academic support listings match your current search and city filter.",
                    icon = Icons.Outlined.School,
                    action = {
                        OneTownCityButton(
                            text = "Reset filters",
                            onClick = {
                                query = ""
                                selectedCity = null
                                cityQuery = "Kuppam"
                                citySuggestions = emptyList()
                            },
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
                    itemsIndexed(items, key = { _, item -> item.id }) { _, item ->
                        TuitionCenterCard(
                            item = item,
                            onClick = { navController.navigate("tuition-center/${item.id}") },
                        )
                    }
                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
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
private fun TuitionCenterCard(
    item: TuitionCenterItem,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (item.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = rememberOptimizedImageRequest(item.imageUrl),
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
                    Icon(
                        imageVector = Icons.Outlined.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                        modifier = Modifier.size(30.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (item.phoneNumber.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = item.phoneNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TuitionCenterDetailScreen(
    navController: NavController,
    item: TuitionCenterItem,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
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
                if (item.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = rememberOptimizedImageRequest(item.imageUrl),
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
                        Icon(
                            imageVector = Icons.Outlined.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (item.subtitle.isNotBlank()) {
                            DetailRow(icon = Icons.Filled.LocationOn, title = item.subtitle)
                        }
                        if (item.phoneNumber.isNotBlank()) {
                            DetailRow(icon = Icons.Filled.Call, title = item.phoneNumber)
                        }
                        if (item.website.isNotBlank()) {
                            DetailRow(icon = Icons.Filled.Language, title = item.website)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (item.phoneNumber.isNotBlank()) {
                        OneTownCityButton(
                            text = "Call",
                            onClick = {
                                val uri = Uri.parse("tel:${item.phoneNumber}")
                                val intent = Intent(Intent.ACTION_DIAL, uri)
                                activity?.startActivity(intent)
                            },
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
                val mapsUri = safeWebUri(item.mapsLink) ?: Uri.parse(
                    "https://www.google.com/maps/search/?api=1&query=${URLEncoder.encode(item.name, "UTF-8")}" 
                )
                OneTownCityButton(
                    text = "Get directions",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, mapsUri)
                        try {
                            activity?.startActivity(intent)
                        } catch (_: ActivityNotFoundException) { }
                    },
                    leadingIcon = Icons.Filled.LocationOn,
                    variant = OneTownCityButtonVariant.Outlined,
                )
            }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PlaceholderShellScreen(
    title: String,
    description: String,
    state: PlaceholderState,
    icon: ImageVector,
    onPrimaryAction: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(
            tonalElevation = 1.dp,
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f),
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "Native shell",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        when (state) {
            PlaceholderState.LOADING -> {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        OneTownCityCircularLoading(label = "Loading content")
                    }
                }
            }
            PlaceholderState.ERROR -> {
                OneTownCityErrorState(
                    title = "Unable to load this section",
                    message = description,
                    actionText = "Retry",
                    onRetry = onPrimaryAction,
                )
            }
            PlaceholderState.EMPTY -> {
                OneTownCityEmptyState(
                    title = "$title is empty",
                    message = description,
                    icon = icon,
                    action = {
                        OneTownCityButton(
                            text = "Refresh",
                            onClick = onPrimaryAction,
                            variant = OneTownCityButtonVariant.Outlined,
                        )
                    },
                )
            }
        }
    }
}

private fun NavController.navigateToTab(tab: AppTab) {
    navigate(tab.route) {
        val current = graph.findStartDestination().id
        popUpTo(current) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
