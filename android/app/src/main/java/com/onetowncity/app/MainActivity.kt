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
import android.provider.Settings
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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import coil.Coil
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.ImageRequest
import com.onetowncity.app.auth.AuthState
import com.onetowncity.app.auth.SessionManager
import com.onetowncity.app.cache.DataFreshness
import com.onetowncity.app.cache.NetworkMonitor
import com.onetowncity.app.cache.OfflineCache
import com.onetowncity.app.cache.cacheTargetFor
import com.onetowncity.app.cache.freshnessFor
import com.onetowncity.app.cache.maxAgeForEntityType
import com.onetowncity.app.designsystem.OneTownCityBottomNavItem
import com.onetowncity.app.designsystem.OneTownCityBottomNavigation
import com.onetowncity.app.designsystem.OneTownCityBottomSheet
import com.onetowncity.app.designsystem.OneTownCityButton
import com.onetowncity.app.designsystem.OneTownCityButtonVariant
import com.onetowncity.app.designsystem.OneTownCityCacheStatusBanner
import com.onetowncity.app.designsystem.OneTownCityCategoryIcons
import com.onetowncity.app.designsystem.OneTownCityChipGroup
import com.onetowncity.app.designsystem.OneTownCityCircularLoading
import com.onetowncity.app.designsystem.OneTownCityCityBar
import com.onetowncity.app.designsystem.OneTownCityCornerRadii
import com.onetowncity.app.designsystem.OneTownCityElevation
import com.onetowncity.app.designsystem.OneTownCityEmptyState
import com.onetowncity.app.designsystem.OneTownCityErrorState
import com.onetowncity.app.designsystem.OneTownCityFilterBar
import com.onetowncity.app.designsystem.OneTownCityIcons
import com.onetowncity.app.designsystem.OneTownCityListItem
import com.onetowncity.app.designsystem.OneTownCityListingCard
import com.onetowncity.app.designsystem.OneTownCitySearchBar
import com.onetowncity.app.designsystem.OneTownCitySpacing
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
    // Updated on every onCreate/onNewIntent so OneTownCityAppShell's
    // LaunchedEffect can forward each redirect Intent to the NavController
    // via handleDeepLink(). Compose Navigation does NOT do this
    // automatically for a singleTask Activity's onNewIntent — without this,
    // the OAuth Custom Tab's onetowncity://auth-callback?code=... redirect
    // updated the Activity's intent (see onNewIntent below) but the NavHost
    // never navigated to the "auth-callback" destination, so
    // SessionManager.completeSignIn() was never called and sign-in silently
    // never finished on a warm relaunch. This was the root cause of
    // sign-in "not working" for anyone returning to an already-running app.
    private var latestIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Restores the persisted Supabase session (if any) synchronously so
        // the first Compose frame already knows sign-in state, instead of
        // flashing signed-out UI for a frame while an async load completes.
        SessionManager.init(applicationContext)
        NetworkMonitor.init(applicationContext)
        OfflineCache.init(applicationContext)
        configureImageLoader(applicationContext)
        latestIntent = intent

        setContent {
            OneTownCityTheme {
                OneTownCityAppShell(pendingIntent = latestIntent)
            }
        }
    }

    // MainActivity is singleTask (see AndroidManifest.xml), so returning from
    // the OAuth Custom Tab redelivers here via onNewIntent rather than a new
    // onCreate. setIntent(...) makes the redirect URI visible to
    // AuthCallbackScreen's LaunchedEffect via LocalContext's Activity.intent;
    // updating latestIntent additionally drives OneTownCityAppShell's
    // handleDeepLink effect so the NavHost actually navigates there.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        latestIntent = intent
    }
}

internal enum class AppTab(
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

/**
 * Classifies the outcome of an in-flight permission request — the only
 * reliable moment Android lets an app distinguish "denied, can still ask
 * again" from "denied repeatedly / permanently" (shouldShowRequestPermissionRationale
 * is documented to be meaningful only right after a request completes, not
 * before one has ever been made). Pulled out as a pure function of the two
 * booleans the call site already has to compute, so it's unit-testable
 * without an Activity.
 */
internal fun classifyPermissionResult(anyGranted: Boolean, canShowRationale: Boolean): LocationPermissionState = when {
    anyGranted -> LocationPermissionState.GRANTED
    canShowRationale -> LocationPermissionState.DENIED
    else -> LocationPermissionState.REVOKED
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

private const val KEY_HAS_AUTO_REQUESTED_LOCATION_PERMISSION = "has_auto_requested_location_permission"

/**
 * Whether the app has already auto-launched the system location-permission
 * prompt once (see OneTownCityAppShell's launch effect). Persisted so this
 * only ever happens once per install, never on every subsequent app open —
 * "do not repeatedly ask for permission unnecessarily" — regardless of
 * whether the user granted, denied, or the process was killed mid-prompt.
 */
private fun readHasAutoRequestedLocationPermission(context: Context): Boolean =
    context.getSharedPreferences(CITY_PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_HAS_AUTO_REQUESTED_LOCATION_PERMISSION, false)

private fun writeHasAutoRequestedLocationPermission(context: Context, value: Boolean) {
    context.getSharedPreferences(CITY_PREFS_NAME, Context.MODE_PRIVATE)
        .edit { putBoolean(KEY_HAS_AUTO_REQUESTED_LOCATION_PERMISSION, value) }
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

internal enum class AuthGateTarget { HOME, LOGIN }

/**
 * The mandatory auth gate's routing decision (see OneTownCityAppShell),
 * pulled out as a pure function so it's unit-testable without a real
 * NavController: null means "stay where you are." Login screen only ever
 * routes forward to Home; anywhere else in the app routes back to Login the
 * moment sign-in is lost, whether from an explicit logout or an expired
 * session refresh (SessionManager.signOutLocally).
 */
internal fun authGateDestination(isSignedIn: Boolean, currentRoute: String?): AuthGateTarget? = when {
    // "auth-callback" also promotes to Home: a cold start straight into the
    // OAuth redirect (the process was killed while the user was in the
    // Custom Tab, then relaunched directly via the deep link) never visits
    // "login" at all, so gating on "login" alone would strand a
    // successfully-signed-in user on the callback screen forever.
    isSignedIn && (currentRoute == "login" || currentRoute == "auth-callback") -> AuthGateTarget.HOME
    !isSignedIn && currentRoute != null && currentRoute != "login" && currentRoute != "auth-callback" -> AuthGateTarget.LOGIN
    else -> null
}

private const val LOCATION_API_BASE_URL = "https://onetowncity.com"

internal sealed class ReverseGeocodeResult {
    data class Success(val city: CitySelection) : ReverseGeocodeResult()
    data class Failure(val message: String) : ReverseGeocodeResult()
}

/**
 * Resolves a GPS fix to a real, backend-verified city via
 * POST /api/v1/locations/reverse-geocode/ (core.api.views.reverse_geocode_view,
 * backed by core/location_service.py's reverse_geocode()).
 *
 * Never fabricates a city: any failure (denied/unavailable GPS is handled by
 * the caller before this is invoked; this function only handles the network
 * round trip) returns a Failure with a message instead of an invented
 * CitySelection.
 *
 * Response contract on success (200): {"cityId", "slug", "city", "district",
 * "state", "country", "countryCode", "latitude", "longitude", "source"} —
 * note the city name field is "city", not "name" (different shape from
 * /api/v1/locations/cities/, which uses "name" — see LocationSerializer vs.
 * core.location_service.serialize_location).
 *
 * On failure (city not found, unsupported country, etc.) the API responds
 * with core.api.exceptions.exception_handler's standard envelope:
 * {"error": {"code", "message"}} — that "message" is surfaced to the user
 * as-is since it is already a user-facing sentence (see reverse_geocode()'s
 * ValidationError messages).
 */
private suspend fun reverseGeocodeCurrentLocation(latitude: Double, longitude: Double): ReverseGeocodeResult = withContext(Dispatchers.IO) {
    try {
        val url = URL("$LOCATION_API_BASE_URL/api/v1/locations/reverse-geocode/")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")

        val requestBody = JSONObject().apply {
            put("latitude", latitude)
            put("longitude", longitude)
        }.toString()
        connection.outputStream.use { it.write(requestBody.toByteArray(Charsets.UTF_8)) }

        val responseCode = connection.responseCode
        val responseStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseText = responseStream?.bufferedReader()?.use { it.readText() }.orEmpty()

        if (responseCode !in 200..299) {
            val message = responseText.takeIf { it.isNotBlank() }
                ?.let { runCatching { JSONObject(it) }.getOrNull() }
                ?.optJSONObject("error")
                ?.optString("message")
                ?.takeIf { it.isNotBlank() }
                ?: "We could not resolve that location. Please choose a city manually."
            return@withContext ReverseGeocodeResult.Failure(message)
        }

        val json = JSONObject(responseText)
        val slug = json.optString("slug", "")
        val name = json.optString("city", "")
        if (slug.isBlank() || name.isBlank()) {
            ReverseGeocodeResult.Failure("We could not resolve that location. Please choose a city manually.")
        } else {
            ReverseGeocodeResult.Success(CitySelection(slug = slug, name = name))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ReverseGeocodeResult.Failure("We could not reach OneTownCity to resolve your location. Please check your connection and try again.")
    }
}

/**
 * Backs the city selector's "Use my current location" action end to end:
 * location services off (distinct from the permission just being denied),
 * no last-known location cached by the OS yet, and a failed reverse-geocode
 * (network error or a coordinate OneTownCity can't map to a known city) each
 * get their own message rather than collapsing into one generic failure.
 */
private suspend fun resolveCurrentCity(
    context: Context,
    getCurrentLocationOrNull: () -> android.location.Location?,
): Result<CitySelection> {
    val locationManager = context.getSystemService(LocationManager::class.java)
    if (locationManager != null && !LocationManagerCompat.isLocationEnabled(locationManager)) {
        return Result.failure(IllegalStateException("Location services are turned off. Enable them in your device settings, or choose a city manually."))
    }
    val location = getCurrentLocationOrNull()
        ?: return Result.failure(IllegalStateException("Location unavailable right now. Please choose a city manually."))
    return when (val result = reverseGeocodeCurrentLocation(location.latitude, location.longitude)) {
        is ReverseGeocodeResult.Success -> Result.success(result.city)
        is ReverseGeocodeResult.Failure -> Result.failure(IllegalStateException(result.message))
    }
}

internal sealed class CitySearchResult {
    data class Success(val suggestions: List<CitySuggestion>) : CitySearchResult()
    data class Error(val message: String) : CitySearchResult()
}

/**
 * Same GET /api/v1/locations/cities/?q= endpoint as fetchCitySuggestions,
 * but for the city selector specifically: distinguishes a genuine
 * network/server error from zero real matches, which
 * fetchCitySuggestions/fetchCitySuggestionsSafely deliberately collapse
 * together (their existing callers only ever need "a list, possibly
 * empty" — see fetchCitySuggestions's own non-2xx handling). Kept as a
 * separate function rather than changing fetchCitySuggestions itself: at
 * least one existing call site (EventsFeatureScreen) calls it unguarded and
 * would crash the moment it started throwing.
 */
internal suspend fun searchCities(cityQuery: String): CitySearchResult = withContext(Dispatchers.IO) {
    if (cityQuery.isBlank()) return@withContext CitySearchResult.Success(emptyList())
    val connection = try {
        URL("$API_BASE_URL/api/v1/locations/cities/?q=${URLEncoder.encode(cityQuery, "UTF-8")}").openConnection() as HttpURLConnection
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        return@withContext CitySearchResult.Error("Couldn't reach OneTownCity. Check your connection and try again.")
    }
    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("Accept", "application/json")
        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (responseCode !in 200..299) {
            return@withContext CitySearchResult.Error(apiErrorMessage(responseText, "Unable to search cities (HTTP $responseCode)."))
        }
        val array = if (responseText.isBlank()) JSONArray() else JSONArray(responseText)
        val suggestions = mutableListOf<CitySuggestion>()
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val slug = item.optString("slug", "")
            val name = item.optString("name", "")
            if (slug.isNotBlank() && name.isNotBlank()) {
                suggestions += CitySuggestion(slug = slug, name = name)
            }
        }
        CitySearchResult.Success(suggestions.take(8))
    } catch (e: CancellationException) {
        throw e
    } catch (e: java.io.IOException) {
        CitySearchResult.Error("Couldn't reach OneTownCity. Check your connection and try again.")
    } catch (e: Exception) {
        CitySearchResult.Error(e.message ?: "Unable to search cities right now.")
    } finally {
        connection.disconnect()
    }
}

private val appTabs = listOf(
    AppTab.HOME,
    AppTab.SEARCH,
    AppTab.STUDENTS,
    AppTab.SAVED,
    AppTab.PROFILE,
)

@Composable
private fun OneTownCityAppShell(pendingIntent: Intent?) {
    val navController = rememberNavController()

    // The actual fix for the OAuth redirect never completing sign-in on a
    // warm relaunch — see MainActivity.latestIntent's doc comment. Safe to
    // call on every Intent including plain launcher starts: handleDeepLink
    // is a no-op when nothing in the graph matches.
    LaunchedEffect(pendingIntent) {
        pendingIntent?.let { navController.handleDeepLink(it) }
    }

    // Mandatory auth gate: SessionManager.init() (MainActivity.onCreate)
    // already restored any persisted session synchronously before this
    // first composes, so this initial read is never a stale/async guess.
    val initialIsSignedIn = remember { SessionManager.authState.value is AuthState.SignedIn }
    val authState by SessionManager.authState.collectAsState()
    val isSignedIn = authState is AuthState.SignedIn

    // Keeps the gate a standing invariant, not just a launch-time check —
    // covers signing in from the "login" screen, signing out from Profile,
    // and a session that goes fully expired (SessionManager.signOutLocally)
    // while the user is already browsing.
    LaunchedEffect(isSignedIn) {
        when (authGateDestination(isSignedIn, navController.currentDestination?.route)) {
            AuthGateTarget.HOME -> navController.navigate(AppTab.HOME.route) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
            }
            AuthGateTarget.LOGIN -> navController.navigate("login") {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
            }
            null -> Unit
        }
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val selectedRoute = currentBackStackEntry?.destination?.route ?: AppTab.HOME.route
    val selectedTabIndex = appTabs.indexOfFirst { it.route == selectedRoute }.coerceAtLeast(0)
    // "login"/"auth-callback" are full-screen, chrome-free destinations —
    // no city header, no bottom nav, matching the "polished login screen"
    // requirement rather than showing app browse chrome above/below it.
    val isChromeRoute = selectedRoute != "login" && selectedRoute != "auth-callback"
    val context = LocalContext.current
    val activity = context as? Activity
    var permissionState by remember { mutableStateOf(resolveLocationPermissionState(context)) }
    val requestLocationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        val anyGranted = granted.values.any { it }
        val canShowRationale = !anyGranted && activity != null && (
            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION) ||
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        permissionState = classifyPermissionResult(anyGranted, canShowRationale)
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
    var isResolvingCurrentCity by remember { mutableStateOf(false) }
    var currentCityError by remember { mutableStateOf<String?>(null) }
    var showCitySelector by remember { mutableStateOf(false) }
    var unreadNotificationCount by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val resolvedCity = pickPreferredCity(currentCity, savedCity, recentCities, permissionState)

    // Broadcasts the resolved city app-wide so every browse/feature screen
    // seeds its own city filter from it instead of starting blank —
    // "switching cities" in the selector now actually reaches them.
    LaunchedEffect(resolvedCity) {
        AppCityState.update(resolvedCity)
        CityPreloader.preloadIfNeeded(resolvedCity?.slug)
    }

    LaunchedEffect(savedCity) {
        val cityToSave = savedCity
        if (cityToSave != null) {
            writeSavedCity(context, cityToSave)
            upsertRecentCity(context, cityToSave)
            recentCities = readRecentCities(context)
        }
    }

    // Primary requirement: auto-request location permission once, but only
    // after the user has actually reached the authenticated app (never on
    // "login"/"auth-callback" — the permission dialog must not appear before
    // or in place of the sign-in screen) and only ever once per install.
    val hasAutoRequestedLocationPermission = remember { readHasAutoRequestedLocationPermission(context) }
    LaunchedEffect(isSignedIn, isChromeRoute) {
        if (isSignedIn && isChromeRoute && !hasAutoRequestedLocationPermission) {
            writeHasAutoRequestedLocationPermission(context, true)
            if (permissionState == LocationPermissionState.DENIED) {
                requestLocationPermission.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            }
        }
    }

    fun useCurrentLocation() {
        if (isResolvingCurrentCity) return
        currentCityError = null
        coroutineScope.launch {
            isResolvingCurrentCity = true
            resolveCurrentCity(context, getCurrentLocationOrNull).fold(
                onSuccess = { city ->
                    savedCity = city
                    currentCity = city
                    currentCityError = null
                    showCitySelector = false
                },
                onFailure = { e -> currentCityError = e.message ?: "Unable to detect your location right now." },
            )
            isResolvingCurrentCity = false
        }
    }

    fun openLocationSettings() {
        activity?.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
        )
    }

    // Best-effort: with no native sign-in flow yet (see currentAuthToken),
    // this always hits the same "sign in required" path server-side and
    // quietly keeps the badge at 0 rather than surfacing an error on the
    // app's main shell.
    LaunchedEffect(Unit) {
        unreadNotificationCount = try {
            fetchUnreadNotificationCount()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            0
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            if (isChromeRoute) {
            // Compact, persistent city affordance (Phase 1 audit: "make the
            // active city obvious but not consume excessive screen space")
            // instead of the old full-height inline city panel — tapping it
            // opens the polished CitySelectorSheet below.
            Surface(
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OneTownCityCityBar(
                        cityName = resolvedCity?.name,
                        onClick = { showCitySelector = true },
                        modifier = Modifier.weight(1f),
                    )
                    Box {
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .clickable { navController.navigate("notifications") },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        if (unreadNotificationCount > 0) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.align(Alignment.TopEnd).size(16.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (unreadNotificationCount > 9) "9+" else unreadNotificationCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onError,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }
        },
        bottomBar = {
            if (isChromeRoute) {
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
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (initialIsSignedIn) AppTab.HOME.route else "login",
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
                        AppTab.HOME -> HomeScreen(navController = navController, city = resolvedCity)
                        AppTab.SEARCH -> SearchScreen(navController = navController)
                        AppTab.STUDENTS -> StudentsHubScreen(navController)
                        AppTab.SAVED -> FavoritesScreen(navController)
                        AppTab.PROFILE -> ProfileScreen(navController)
                    }
                }
            }

            composable(route = "notifications") {
                NotificationsScreen(navController)
            }

            composable(route = "my-listings") {
                MyListingsScreen(navController)
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
                val item = scholarshipCache[itemId]
                if (item == null) {
                    OneTownCityEmptyState(
                        title = "Scholarship not found",
                        message = "This scholarship is no longer available.",
                        icon = Icons.Outlined.EmojiEvents,
                        action = {
                            OneTownCityButton(
                                text = "Back",
                                onClick = { navController.popBackStack() },
                                variant = OneTownCityButtonVariant.Outlined,
                            )
                        },
                    )
                } else {
                    ScholarshipDetailScreen(navController = navController, item = item)
                }
            }

            composable(
                route = "lost-found/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.IntType }),
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getInt("itemId") ?: return@composable
                val item = lostFoundCache[itemId]
                if (item == null) {
                    OneTownCityEmptyState(
                        title = "Report not found",
                        message = "This lost & found report is no longer available.",
                        icon = Icons.Outlined.Search,
                        action = {
                            OneTownCityButton(
                                text = "Back",
                                onClick = { navController.popBackStack() },
                                variant = OneTownCityButtonVariant.Outlined,
                            )
                        },
                    )
                } else {
                    LostFoundDetailScreen(navController = navController, item = item)
                }
            }

            composable(
                route = "place/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.IntType }),
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getInt("itemId") ?: return@composable
                val item = placeCache[itemId]
                if (item == null) {
                    OneTownCityEmptyState(
                        title = "Place not found",
                        message = "This place is no longer available.",
                        icon = Icons.Filled.LocationOn,
                        action = {
                            OneTownCityButton(
                                text = "Back",
                                onClick = { navController.popBackStack() },
                                variant = OneTownCityButtonVariant.Outlined,
                            )
                        },
                    )
                } else {
                    PlaceDetailScreen(navController = navController, item = item)
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

            // Mandatory launch/standing auth gate — the app's start destination
            // when signed out, and where OneTownCityAppShell's reactive effect
            // sends the user back to on logout or session expiry. Distinct
            // from "sign-in" below (a secondary, back-navigable prompt pushed
            // from a specific in-app action) so this screen has no back
            // target — there's nothing behind it to return to.
            composable(route = "login") {
                LoginScreen(navController = navController)
            }

            composable(route = "sign-in") {
                SignInScreen(navController = navController)
            }

            composable(
                route = "auth-callback",
                deepLinks = listOf(navDeepLink { uriPattern = "onetowncity://auth-callback" }),
            ) {
                AuthCallbackScreen(navController = navController)
            }

            composable(
                route = "businesses?category={categoryKey}",
                arguments = listOf(navArgument("categoryKey") { type = NavType.StringType; nullable = true }),
            ) { backStackEntry ->
                BusinessBrowseScreen(
                    navController = navController,
                    initialCategoryKey = backStackEntry.arguments?.getString("categoryKey"),
                )
            }

            composable(
                route = "business/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.IntType }),
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getInt("itemId") ?: return@composable
                BusinessDetailScreen(navController = navController, businessId = itemId)
            }

            composable(
                route = "properties?category={categoryKey}",
                arguments = listOf(navArgument("categoryKey") { type = NavType.StringType; nullable = true }),
            ) { backStackEntry ->
                PropertyBrowseScreen(
                    navController = navController,
                    initialCategoryKey = backStackEntry.arguments?.getString("categoryKey"),
                )
            }

            composable(
                route = "property/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.IntType }),
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getInt("itemId") ?: return@composable
                PropertyDetailScreen(navController = navController, propertyId = itemId)
            }

            composable(
                route = "projects?category={categoryKey}",
                arguments = listOf(navArgument("categoryKey") { type = NavType.StringType; nullable = true }),
            ) { backStackEntry ->
                ProjectBrowseScreen(
                    navController = navController,
                    initialCategoryKey = backStackEntry.arguments?.getString("categoryKey"),
                )
            }

            composable(
                route = "project/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.IntType }),
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getInt("itemId") ?: return@composable
                ProjectDetailScreen(navController = navController, projectId = itemId)
            }
        }
    }

    if (showCitySelector) {
        CitySelectorSheet(
            resolvedCity = resolvedCity,
            recentCities = recentCities,
            permissionState = permissionState,
            isResolvingCurrentCity = isResolvingCurrentCity,
            currentCityError = currentCityError,
            onUseCurrentLocation = ::useCurrentLocation,
            onRequestPermission = {
                requestLocationPermission.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            },
            onOpenSettings = ::openLocationSettings,
            onCitySelected = { city ->
                savedCity = city
                currentCity = city
                currentCityError = null
                showCitySelector = false
            },
            onDismiss = { showCitySelector = false },
        )
    }
}

/**
 * The polished city selector (Phase 4): current city, a location action that
 * adapts to every permission state, city search with its own
 * loading/empty/error states, and recent cities with the active one marked.
 */
@Composable
private fun CitySelectorSheet(
    resolvedCity: CitySelection?,
    recentCities: List<CitySelection>,
    permissionState: LocationPermissionState,
    isResolvingCurrentCity: Boolean,
    currentCityError: String?,
    onUseCurrentLocation: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onCitySelected: (CitySelection) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResult by remember { mutableStateOf<CitySearchResult>(CitySearchResult.Success(emptyList())) }
    var retryTick by remember { mutableStateOf(0) }

    LaunchedEffect(query, retryTick) {
        if (query.isBlank()) {
            isSearching = false
            searchResult = CitySearchResult.Success(emptyList())
            return@LaunchedEffect
        }
        isSearching = true
        kotlinx.coroutines.delay(300)
        searchResult = searchCities(query)
        isSearching = false
    }

    OneTownCityBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = OneTownCitySpacing.xl, vertical = OneTownCitySpacing.lg),
            verticalArrangement = Arrangement.spacedBy(OneTownCitySpacing.lg),
        ) {
            Text(text = "Choose your city", style = MaterialTheme.typography.titleLarge)

            Column(verticalArrangement = Arrangement.spacedBy(OneTownCitySpacing.xs)) {
                Text(text = "Current city", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = resolvedCity?.name ?: "No city selected yet", style = MaterialTheme.typography.headlineSmall)
            }

            LocationActionRow(
                permissionState = permissionState,
                isResolving = isResolvingCurrentCity,
                onUseCurrentLocation = onUseCurrentLocation,
                onRequestPermission = onRequestPermission,
                onOpenSettings = onOpenSettings,
            )
            if (currentCityError != null) {
                Text(text = currentCityError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            OneTownCitySearchBar(
                query = query,
                onQueryChange = { query = it },
                placeholder = "Search for a city",
                onClear = { query = "" },
            )

            when {
                isSearching -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = OneTownCitySpacing.lg), contentAlignment = Alignment.Center) {
                        OneTownCityCircularLoading(label = "Searching…")
                    }
                }
                query.isBlank() -> {
                    if (recentCities.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(OneTownCitySpacing.xs)) {
                            Text(text = "Recent", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            recentCities.forEach { city ->
                                OneTownCityListItem(
                                    title = city.name,
                                    onClick = { onCitySelected(city) },
                                    trailing = if (city.slug == resolvedCity?.slug) {
                                        { Icon(imageVector = OneTownCityIcons.check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary) }
                                    } else null,
                                )
                            }
                        }
                    }
                }
                else -> when (val state = searchResult) {
                    is CitySearchResult.Error -> OneTownCityErrorState(
                        title = "Couldn't search cities",
                        message = state.message,
                        actionText = "Retry",
                        onRetry = { retryTick++ },
                    )
                    is CitySearchResult.Success -> if (state.suggestions.isEmpty()) {
                        OneTownCityEmptyState(
                            title = "No cities found",
                            message = "Try a different spelling, or a nearby town.",
                            icon = OneTownCityIcons.search,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(OneTownCitySpacing.xs)) {
                            state.suggestions.forEach { suggestion ->
                                OneTownCityListItem(
                                    title = suggestion.name,
                                    onClick = { onCitySelected(CitySelection(slug = suggestion.slug, name = suggestion.name)) },
                                    trailing = if (suggestion.slug == resolvedCity?.slug) {
                                        { Icon(imageVector = OneTownCityIcons.check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary) }
                                    } else null,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationActionRow(
    permissionState: LocationPermissionState,
    isResolving: Boolean,
    onUseCurrentLocation: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    when (permissionState) {
        LocationPermissionState.GRANTED -> OneTownCityButton(
            text = if (isResolving) "Locating…" else "Use my current location",
            onClick = onUseCurrentLocation,
            enabled = !isResolving,
            leadingIcon = OneTownCityIcons.location,
            variant = OneTownCityButtonVariant.Outlined,
            modifier = Modifier.fillMaxWidth(),
        )
        LocationPermissionState.DENIED -> OneTownCityButton(
            text = "Allow location access",
            onClick = onRequestPermission,
            leadingIcon = OneTownCityIcons.location,
            variant = OneTownCityButtonVariant.Outlined,
            modifier = Modifier.fillMaxWidth(),
        )
        LocationPermissionState.REVOKED -> Column(verticalArrangement = Arrangement.spacedBy(OneTownCitySpacing.xs)) {
            Text(
                text = "Location permission was denied. Enable it in Settings to detect your city automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OneTownCityButton(
                text = "Open Settings",
                onClick = onOpenSettings,
                variant = OneTownCityButtonVariant.Outlined,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        LocationPermissionState.UNAVAILABLE -> Text(
            text = "Location isn't available on this device. Search for your city instead.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SearchScreen(navController: NavController) {
    var query by rememberSaveable { mutableStateOf("") }
    var cityQuery by rememberSaveable { mutableStateOf(AppCityState.current.value?.name.orEmpty()) }
    var selectedCity by rememberSaveable(stateSaver = CitySuggestionSaver) { mutableStateOf(AppCityState.current.value?.let { CitySuggestion(it.slug, it.name) }) }
    var hasManualCityOverride by rememberSaveable { mutableStateOf(false) }
    var citySuggestions by remember { mutableStateOf<List<CitySuggestion>>(emptyList()) }
    var sections by remember { mutableStateOf<List<SearchSection>>(emptyList()) }
    var selectedModelKey by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isOfflineNoCache by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    val isOnline by NetworkMonitor.isOnline.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var activeRequest by remember { mutableStateOf<Job?>(null) }
    val context = LocalContext.current
    val activity = context as? Activity

    fun runSearch() {
        activeRequest?.cancel()
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            sections = emptyList()
            selectedModelKey = null
            isLoading = false
            error = null
            isOfflineNoCache = false
            hasSearched = false
            return
        }
        activeRequest = coroutineScope.launch {
            isLoading = true
            error = null
            isOfflineNoCache = false
            hasSearched = true
            try {
                sections = fetchSearchResults(trimmedQuery, selectedCity?.slug ?: cityQuery.trim())
                selectedModelKey = null
                error = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: OfflineNoCacheException) {
                // A network failure while offline must never look like "we
                // searched and found nothing" — this is a distinct state
                // ("Search must not return misleading empty results when
                // the network failed").
                sections = emptyList()
                isOfflineNoCache = true
            } catch (e: Exception) {
                sections = emptyList()
                error = e.message ?: "Unable to search right now."
            } finally {
                isLoading = false
            }
        }
    }

    val globalCity by AppCityState.current.collectAsState()
    LaunchedEffect(globalCity) {
        if (!hasManualCityOverride) {
            selectedCity = globalCity?.let { CitySuggestion(it.slug, it.name) }
            cityQuery = globalCity?.name.orEmpty()
        }
    }

    LaunchedEffect(query, selectedCity?.slug ?: cityQuery) {
        kotlinx.coroutines.delay(400)
        runSearch()
    }

    LaunchedEffect(cityQuery) {
        kotlinx.coroutines.delay(300)
        citySuggestions = if (cityQuery.isBlank()) emptyList() else fetchCitySuggestionsSafely(cityQuery)
    }

    LaunchedEffect(isOnline) {
        if (isOnline && (isOfflineNoCache || (error != null && sections.isEmpty()))) {
            runSearch()
        }
    }

    val modelKeyByLabel = remember(sections) { sections.associate { searchSectionLabel(it.modelKey) to it.modelKey } }
    val visibleSections = remember(sections, selectedModelKey) {
        if (selectedModelKey == null) sections else sections.filter { it.modelKey == selectedModelKey }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = OneTownCitySpacing.xl, vertical = OneTownCitySpacing.lg),
        verticalArrangement = Arrangement.spacedBy(OneTownCitySpacing.lg),
    ) {
        OneTownCitySearchBar(
            query = query,
            onQueryChange = { query = it },
            placeholder = "Search businesses, properties, jobs, events...",
            onClear = { query = "" },
        )

        Surface(
            shape = RoundedCornerShape(OneTownCityCornerRadii.xl),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = OneTownCityElevation.low,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(OneTownCitySpacing.lg),
                verticalArrangement = Arrangement.spacedBy(OneTownCitySpacing.md),
            ) {
                Text(
                    text = "Location",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                OneTownCityTextField(
                    value = cityQuery,
                    onValueChange = { cityQuery = it; selectedCity = null; hasManualCityOverride = true },
                    placeholder = "City or area (optional)",
                    leadingIcon = Icons.Filled.LocationOn,
                )
                if (citySuggestions.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(OneTownCitySpacing.sm),
                    ) {
                        citySuggestions.take(3).forEach { suggestion ->
                            OneTownCityButton(
                                text = suggestion.name,
                                onClick = {
                                    selectedCity = suggestion
                                    cityQuery = suggestion.name
                                    citySuggestions = emptyList()
                                    hasManualCityOverride = true
                                },
                                variant = OneTownCityButtonVariant.Outlined,
                            )
                        }
                    }
                }
            }
        }

        if (sections.size > 1) {
            OneTownCityFilterBar(
                options = modelKeyByLabel.keys.toList(),
                selectedOption = selectedModelKey?.let { searchSectionLabel(it) },
                onOptionSelected = { label -> selectedModelKey = label?.let { modelKeyByLabel[it] } },
                allLabel = "All (${sections.sumOf { it.items.size }})",
            )
        }

        if (!isOnline && sections.isNotEmpty()) {
            OneTownCityCacheStatusBanner(
                message = "You're offline — showing saved results",
                isOffline = true,
            )
        }

        when {
            !hasSearched -> {
                OneTownCityEmptyState(
                    title = "Search OneTownCity",
                    message = "Search across businesses, properties, jobs, events, news and projects.",
                    icon = Icons.Outlined.Search,
                )
            }
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    OneTownCityCircularLoading(label = "Searching")
                }
            }
            isOfflineNoCache -> {
                OneTownCityEmptyState(
                    title = "You're offline",
                    message = "This search hasn't been run yet on this device, so nothing's saved for it. Connect to the internet once to search.",
                    icon = Icons.Outlined.Search,
                )
            }
            error != null -> {
                OneTownCityErrorState(
                    title = "Unable to search",
                    message = error ?: "Please try again later.",
                    actionText = "Retry",
                    onRetry = { runSearch() },
                )
            }
            sections.isEmpty() -> {
                OneTownCityEmptyState(
                    title = "No results found",
                    message = "Try a different keyword, or clear the city filter.",
                    icon = Icons.Outlined.Search,
                    action = {
                        OneTownCityButton(
                            text = "Clear search",
                            onClick = { query = ""; cityQuery = ""; selectedCity = null },
                            variant = OneTownCityButtonVariant.Outlined,
                        )
                    },
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(OneTownCitySpacing.lg),
                    contentPadding = PaddingValues(bottom = OneTownCitySpacing.xxl),
                ) {
                    visibleSections.forEach { section ->
                        item(key = "header-${section.modelKey}") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(OneTownCitySpacing.sm),
                            ) {
                                Icon(
                                    imageVector = OneTownCityCategoryIcons.forModelKey(section.modelKey),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "${section.label} (${section.items.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        }
                        itemsIndexed(section.items, key = { _, result -> "${section.modelKey}-${result.id}" }) { _, result ->
                            SearchResultCard(
                                item = result,
                                onClick = {
                                    // Business/Property/Project have real native detail
                                    // screens (Phase 4) that fetch by id, so route there
                                    // directly; every other model_key still opens the web
                                    // listing page since no native screen exists for it.
                                    when (result.modelKey) {
                                        "business" -> navController.navigate("business/${result.id}")
                                        "property" -> navController.navigate("property/${result.id}")
                                        "project" -> navController.navigate("project/${result.id}")
                                        else -> {
                                            safeWebUri(API_BASE_URL + result.url)?.let { uri ->
                                                try {
                                                    activity?.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                                } catch (_: ActivityNotFoundException) { }
                                            }
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    item: ListingSummary,
    onClick: () -> Unit,
) {
    OneTownCityListingCard(
        title = item.title,
        subtitle = listOfNotNull(item.subtitle.takeIf { it.isNotBlank() }, item.cityName.takeIf { it.isNotBlank() })
            .joinToString(" · ")
            .takeIf { it.isNotBlank() },
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
        onClick = onClick,
    )
}

internal enum class StudentsCategory(
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

internal val studentsCategories = listOf(
    StudentsCategory.TUITION_CENTERS,
    StudentsCategory.EVENTS,
    StudentsCategory.BUY_SELL_EXCHANGE,
    StudentsCategory.STUDENT_SERVICES,
    StudentsCategory.SCHOLARSHIPS,
    StudentsCategory.LOST_FOUND,
    StudentsCategory.PLACES_TO_VISIT,
)

/**
 * Groups the 7 real student categories under a short, meaningful heading
 * each — "appropriate grouping and hierarchy" instead of one flat grid.
 * Every StudentsCategory appears in exactly one group; groupsFor() below
 * asserts that at test time so a newly added category can't silently be
 * left out of the directory.
 */
internal data class StudentsCategoryGroup(val title: String, val categories: List<StudentsCategory>)

internal val studentsCategoryGroups = listOf(
    StudentsCategoryGroup(
        title = "Academics & Funding",
        categories = listOf(StudentsCategory.TUITION_CENTERS, StudentsCategory.SCHOLARSHIPS),
    ),
    StudentsCategoryGroup(
        title = "Campus Life",
        categories = listOf(StudentsCategory.EVENTS, StudentsCategory.LOST_FOUND),
    ),
    StudentsCategoryGroup(
        title = "Marketplace & Services",
        categories = listOf(StudentsCategory.BUY_SELL_EXCHANGE, StudentsCategory.STUDENT_SERVICES),
    ),
    StudentsCategoryGroup(
        title = "Explore",
        categories = listOf(StudentsCategory.PLACES_TO_VISIT),
    ),
)

@Composable
private fun StudentsHubScreen(navController: NavController) {
    var query by rememberSaveable { mutableStateOf("") }
    val isSearching = query.isNotBlank()
    val searchResults = remember(query) {
        if (!isSearching) emptyList() else studentsCategories.filter {
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

        if (!isSearching) {
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
        }

        if (isSearching && searchResults.isEmpty()) {
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                if (isSearching) {
                    items(searchResults, key = { it.key }) { category ->
                        StudentsCategoryCard(
                            category = category,
                            onClick = { navController.navigate("students/${category.key}") },
                        )
                    }
                } else {
                    studentsCategoryGroups.forEach { group ->
                        item(key = "header-${group.title}") {
                            Text(
                                text = group.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                            )
                        }
                        items(group.categories, key = { it.key }) { category ->
                            StudentsCategoryCard(
                                category = category,
                                onClick = { navController.navigate("students/${category.key}") },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-width row card, not a fixed-size grid tile: title/description wrap
 * with no maxLines/ellipsis, so long names like "Scholarships & Government
 * Schemes" are always fully readable and the row's height simply grows at
 * larger font scales (verified conceptually up to 2.0x) instead of clipping.
 */
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                shape = CircleShape,
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
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
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
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
    val avgRating: Double = 0.0,
    val reviewCount: Int = 0,
    val commentCount: Int = 0,
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
    val cityName: String = "",
    val provider: String = "",
    val contactNumber: String = "",
    val avgRating: Double = 0.0,
    val reviewCount: Int = 0,
    val commentCount: Int = 0,
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
    val avgRating: Double = 0.0,
    val reviewCount: Int = 0,
    val commentCount: Int = 0,
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

private val scholarshipCache = BoundedItemCache<Int, ScholarshipItem>(200)

private fun parseScholarship(json: JSONObject): ScholarshipItem {
    val item = ScholarshipItem(
        id = json.optInt("id"),
        title = json.optString("title", "Scholarship"),
        description = json.optString("description", "").ifBlank { "No description provided." },
        category = json.optString("scholarship_type", "other"),
        deadline = json.optString("application_deadline", ""),
        eligibility = json.optString("eligibility", ""),
        officialUrl = json.optString("official_url", ""),
        cityName = json.cityName(""),
        provider = json.optString("provider", ""),
        contactNumber = json.optString("contact_number", ""),
        avgRating = json.optDouble("avg_rating", 0.0).let { if (it.isNaN()) 0.0 else it },
        reviewCount = json.optInt("review_count", 0),
        commentCount = json.optInt("comment_count", 0),
    )
    scholarshipCache[item.id] = item
    return item
}

/** Exposed separately so ListingsViewModel's stale-while-revalidate can peek the offline cache for the exact same URL before deciding whether a network refresh is needed. */
internal fun buildScholarshipsUrl(query: String, citySlug: String, page: Int): String = buildString {
    val encodedCity = citySlug.takeIf { it.isNotBlank() }?.let { URLEncoder.encode(it, "UTF-8") }
    append(API_BASE_URL)
    append("/api/v1/listings/scholarship/?page=")
    append(page)
    append("&page_size=10")
    if (query.isNotBlank()) {
        append("&q=")
        append(URLEncoder.encode(query, "UTF-8"))
    }
    if (!encodedCity.isNullOrEmpty()) {
        append("&city=")
        append(encodedCity)
    }
}

/** Wires to the real /api/v1/listings/scholarship/ endpoint (core.models.Scholarship) — a standalone listing type, not a Business category. */
internal suspend fun fetchScholarships(query: String, citySlug: String, page: Int): ApiListPage<ScholarshipItem> =
    fetchListPage(buildScholarshipsUrl(query, citySlug, page), page) { parseScholarship(it) }

private val lostFoundCache = BoundedItemCache<Int, LostFoundItem>(200)

private fun parseLostFound(json: JSONObject, citySlugFallback: String): LostFoundItem {
    val city = json.optJSONObject("city")
    val item = LostFoundItem(
        id = json.optInt("id"),
        title = json.optString("title", "Item"),
        category = json.optString("report_type", "lost").replaceFirstChar { it.titlecase() },
        description = json.optString("description", "").ifBlank { "No description provided." },
        location = json.optString("location", ""),
        date = json.optString("event_date", ""),
        imageUrl = json.optString("display_image", ""),
        contact = json.optString("contact_number", ""),
        reportUrl = json.optString("url", ""),
        cityName = city?.optString("name") ?: citySlugFallback,
        avgRating = json.optDouble("avg_rating", 0.0).let { if (it.isNaN()) 0.0 else it },
        reviewCount = json.optInt("review_count", 0),
        commentCount = json.optInt("comment_count", 0),
    )
    lostFoundCache[item.id] = item
    return item
}

/** Exposed separately so ListingsViewModel's stale-while-revalidate can peek the offline cache for the exact same URL before deciding whether a network refresh is needed. */
internal fun buildLostFoundUrl(query: String, citySlug: String, page: Int): String = buildString {
    val encodedCity = citySlug.takeIf { it.isNotBlank() }?.let { URLEncoder.encode(it, "UTF-8") }
    append(API_BASE_URL)
    append("/api/v1/listings/lostfound/?page=")
    append(page)
    append("&page_size=10")
    if (query.isNotBlank()) {
        append("&q=")
        append(URLEncoder.encode(query, "UTF-8"))
    }
    if (!encodedCity.isNullOrEmpty()) {
        append("&city=")
        append(encodedCity)
    }
}

/** Wires to the real /api/v1/listings/lostfound/ endpoint (core.models.LostFound). The generic listing API has no report_type query param, so Lost/Found is refined client-side via filterLostFound, same as every other client-side chip filter in this file. */
internal suspend fun fetchLostFound(query: String, citySlug: String, page: Int): ApiListPage<LostFoundItem> =
    fetchListPage(buildLostFoundUrl(query, citySlug, page), page) { parseLostFound(it, citySlug) }

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
    val avgRating: Double = 0.0,
    val reviewCount: Int = 0,
    val commentCount: Int = 0,
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

private val placeCache = BoundedItemCache<Int, PlaceItem>(200)

private fun parsePlace(result: JSONObject, citySlugFallback: String): PlaceItem {
    val city = result.optJSONObject("city")
    val cityName = city?.optString("name") ?: citySlugFallback
    val item = PlaceItem(
        id = result.optInt("id"),
        title = result.optString("name", "Place to visit"),
        category = result.optString("category", "tourism"),
        description = result.optString("description", "").ifBlank { "More details are coming soon for this place." },
        cityName = cityName,
        address = result.optString("address", ""),
        imageUrl = result.optString("display_image", ""),
        mapsLink = result.optString("maps_link", ""),
        avgRating = result.optDouble("avg_rating", 0.0).let { if (it.isNaN()) 0.0 else it },
        reviewCount = result.optInt("review_count", 0),
        commentCount = result.optInt("comment_count", 0),
    )
    placeCache[item.id] = item
    return item
}

/** Places to Visit filters on the real `category=tourism` (core/models.py's Business.CATEGORY_CHOICES) — the same data the web's /places-to-visit/ directory page shows. */
internal suspend fun fetchPlacesToVisit(query: String, citySlug: String, page: Int): ApiListPage<PlaceItem> =
    fetchListPage(buildBusinessCategoryUrl("tourism", query, citySlug, page), page) { parsePlace(it, citySlug) }

// Buy / Sell / Exchange is not a separate backend model — core/models.py's
// Business.CATEGORY_CHOICES has a real 'marketplace' entry ("Buy / Sell /
// Exchange"), served by the exact same GET /api/v1/listings/business/
// ?category=<key> endpoint as Places to Visit ('tourism'), Tuition Centers
// ('tuition_center'), and Student Services ('student_services'). It reuses
// BusinessItem/fetchBusinesses/parseBusiness directly — see
// MarketplaceFeatureScreen — rather than a parallel, never-populated shape
// for fields (price/condition/transactionType) that don't exist on any real
// listing model.

internal const val API_BASE_URL = "https://onetowncity.com"

/**
 * Every auth-required call below goes through this single function rather
 * than each screen reading the session directly, so core/api/authentication.py's
 * SupabaseTokenAuthentication (Authorization: Bearer <token>) is wired up in
 * exactly one place. Backed by SessionManager's in-memory, refreshed access
 * token (see auth/SessionManager.kt) — null when signed out.
 */
private suspend fun currentAuthToken(): String? = SessionManager.ensureFreshAccessToken()

internal class AuthRequiredException : Exception("Sign in to use this feature.")

/** Thrown when offline and this specific request has never been cached — distinct from a generic failure so the UI can show "connect once to load this" instead of a plain error. */
internal class OfflineNoCacheException(message: String) : Exception(message)

private suspend fun rawHttpRequest(
    urlString: String,
    method: String,
    jsonBody: String?,
    token: String?,
): Pair<Int, String> = withContext(Dispatchers.IO) {
    val connection = URL(urlString).openConnection() as HttpURLConnection
    try {
        connection.requestMethod = method
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("Accept", "application/json")
        if (token != null) {
            connection.setRequestProperty("Authorization", "Bearer $token")
        }
        if (jsonBody != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }
        }
        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        responseCode to responseText
    } finally {
        connection.disconnect()
    }
}

/**
 * Single low-level HTTP+JSON call shared by every screen's network code —
 * consistent timeouts, headers, auth-header attachment, and error-envelope
 * parsing (core/api/exceptions.py's {"error": {"code", "message"}} shape) in
 * one place instead of each screen re-implementing HttpURLConnection
 * boilerplate. Throws AuthRequiredException for a call that needs a token
 * this build has no way to obtain yet, or on a real 401 from the server;
 * IllegalStateException (with the server's own message where available) for
 * every other non-2xx response.
 */
internal suspend fun httpJson(
    urlString: String,
    method: String = "GET",
    jsonBody: String? = null,
    requiresAuth: Boolean = false,
): JSONObject = httpJsonWithFreshness(urlString, method, jsonBody, requiresAuth).first

/**
 * Cache-aware variant of [httpJson] that additionally reports whether the
 * result came from a live network response or the offline cache — this is
 * the actual offline-first mechanism: every existing fetchXxx() function
 * (and everything built on fetchListPage) gets caching and offline fallback
 * for free through this one function, no changes needed anywhere else.
 *
 * Caching only ever applies to GET + !requiresAuth calls to public
 * listings/categories endpoints (see cacheTargetFor) — auth-required calls
 * (favorites, notifications, profile, account mutations) always hit the
 * network exactly as before and are never read from or written to the
 * offline cache. That split is deliberate, not incidental: "do not blindly
 * make user-specific mutable operations offline."
 */
internal suspend fun httpJsonWithFreshness(
    urlString: String,
    method: String = "GET",
    jsonBody: String? = null,
    requiresAuth: Boolean = false,
): Pair<JSONObject, DataFreshness> {
    val token = if (requiresAuth) (currentAuthToken() ?: throw AuthRequiredException()) else null
    val cacheTarget = if (method == "GET" && !requiresAuth) cacheTargetFor(urlString) else null

    suspend fun liveRequest(): JSONObject {
        val (responseCode, responseText) = rawHttpRequest(urlString, method, jsonBody, token)
        if (responseCode == 401) throw AuthRequiredException()
        if (responseCode !in 200..299) throw IllegalStateException(apiErrorMessage(responseText, "Request failed (HTTP $responseCode)."))
        if (cacheTarget != null) {
            OfflineCache.write(urlString, cacheTarget.citySlug, cacheTarget.entityType, responseText)
        }
        return if (responseText.isBlank()) JSONObject() else JSONObject(responseText)
    }

    if (cacheTarget == null) {
        return liveRequest() to DataFreshness.LIVE
    }

    val maxAge = maxAgeForEntityType(cacheTarget.entityType)

    if (!NetworkMonitor.isOnlineNow()) {
        val cached = OfflineCache.read(urlString)
            ?: throw OfflineNoCacheException("You're offline and this hasn't been loaded yet. Connect to the internet once to load it.")
        val freshness = freshnessFor(cached.cachedAtMillis, maxAge)
        return JSONObject(cached.json) to freshness
    }

    return try {
        liveRequest() to DataFreshness.LIVE
    } catch (e: CancellationException) {
        throw e
    } catch (e: AuthRequiredException) {
        throw e
    } catch (e: Exception) {
        // NetworkMonitor thinks we're online but the request itself still
        // failed — DNS failure, timeout, server unavailable, API 500, a
        // captive portal, etc. Fall back to cache rather than surfacing a
        // hard error when there's something useful to show.
        val cached = OfflineCache.read(urlString)
        if (cached != null) {
            val freshness = freshnessFor(cached.cachedAtMillis, maxAge)
            JSONObject(cached.json) to freshness
        } else {
            throw e
        }
    }
}

internal fun apiErrorMessage(responseText: String, fallback: String): String {
    if (responseText.isBlank()) return fallback
    return runCatching { JSONObject(responseText) }.getOrNull()
        ?.optJSONObject("error")
        ?.optString("message")
        ?.takeIf { it.isNotBlank() }
        ?: fallback
}

internal data class ApiListPage<T>(val items: List<T>, val nextPage: Int?, val count: Int)

/** Shared pagination-envelope parser for every core.api list endpoint — {"count","next","previous","page_size","results"} (see core/api/pagination.py). */
internal suspend fun <T> fetchListPage(
    urlString: String,
    currentPage: Int,
    requiresAuth: Boolean = false,
    parseItem: (JSONObject) -> T,
): ApiListPage<T> {
    val json = httpJson(urlString, requiresAuth = requiresAuth)
    val results = json.optJSONArray("results") ?: JSONArray()
    val items = mutableListOf<T>()
    for (i in 0 until results.length()) {
        items += parseItem(results.getJSONObject(i))
    }
    val nextLink = json.optString("next", "")
    val nextPage = if (nextLink.isBlank()) null else currentPage + 1
    return ApiListPage(items = items, nextPage = nextPage, count = json.optInt("count", items.size))
}

/**
 * The "instantly show what we have" half of stale-while-revalidate — reads
 * whatever OfflineCache already holds for this exact list-page URL, without
 * touching the network at all, so ListingsViewModel can display it with no
 * loading spinner while a real fetchListPage call refreshes it in the
 * background. Returns null on a cache miss (nothing to show yet).
 */
internal suspend fun <T> peekCachedListPage(urlString: String, parseItem: (JSONObject) -> T): ApiListPage<T>? {
    val cached = OfflineCache.read(urlString) ?: return null
    val json = if (cached.json.isBlank()) JSONObject() else JSONObject(cached.json)
    val results = json.optJSONArray("results") ?: JSONArray()
    val items = mutableListOf<T>()
    for (i in 0 until results.length()) {
        items += parseItem(results.getJSONObject(i))
    }
    val nextLink = json.optString("next", "")
    return ApiListPage(items = items, nextPage = if (nextLink.isBlank()) null else 2, count = json.optInt("count", items.size))
}

/** Shared URL builder for every "Business rows filtered to one category" screen (Tuition Centers, Student Services, Places to Visit) — see core/api/views.py's _list_listings, which ANDs `category` and `q` together rather than one replacing the other. */
internal fun buildBusinessCategoryUrl(category: String, query: String, citySlug: String, page: Int, pageSize: Int = 10): String {
    val encodedCity = citySlug.takeIf { it.isNotBlank() }?.let { URLEncoder.encode(it, "UTF-8") }
    return buildString {
        append(API_BASE_URL)
        append("/api/v1/listings/business/?category=")
        append(URLEncoder.encode(category, "UTF-8"))
        if (query.isNotBlank()) {
            append("&q=")
            append(URLEncoder.encode(query, "UTF-8"))
        }
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

/**
 * A listing summary shape shared by Search and Favorites — both endpoints
 * return heterogeneous listing types (business/property/job/event/news/
 * project) using the exact same serializer fields (core/api/serializers.py's
 * COMMON_LISTING_FIELDS, which always includes `model_key`), so one parser
 * covers every type instead of one per screen.
 */
internal data class ListingSummary(
    val id: Int,
    val modelKey: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val url: String,
    val cityName: String,
)

internal fun parseListingSummary(json: JSONObject): ListingSummary {
    val modelKey = json.optString("model_key", "")
    val title = if (modelKey == "business") json.optString("name", "Listing") else json.optString("title", "Listing")
    val subtitle = when (modelKey) {
        "business" -> json.optString("address", "")
        "property" -> json.optString("location", "")
        "job" -> json.optString("company", "")
        "event" -> json.optString("location", "")
        "news" -> json.optString("source", "")
        "project" -> json.optString("location", "")
        "scholarship" -> json.optString("provider", "")
        "lostfound" -> json.optString("location", "")
        else -> ""
    }
    val city = json.optJSONObject("city")
    return ListingSummary(
        id = json.optInt("id"),
        modelKey = modelKey,
        title = title,
        subtitle = subtitle,
        imageUrl = json.optString("display_image", ""),
        url = json.optString("url", ""),
        cityName = city?.optString("name") ?: "",
    )
}

internal data class SearchSection(
    val modelKey: String,
    val label: String,
    val icon: ImageVector,
    val items: List<ListingSummary>,
)

private fun searchSectionLabel(modelKey: String): String = when (modelKey) {
    "business" -> "Businesses"
    "property" -> "Properties"
    "job" -> "Jobs"
    "event" -> "Events"
    "news" -> "News"
    "project" -> "Projects"
    else -> modelKey.replaceFirstChar { it.titlecase() }
}

private fun searchSectionIcon(modelKey: String): ImageVector = when (modelKey) {
    "business" -> Icons.Outlined.Build
    "property" -> Icons.Filled.Home
    "job" -> Icons.Outlined.Build
    "event" -> Icons.Outlined.Event
    else -> Icons.Filled.Info
}

/** Wires to core.api.views.search_view (`GET /api/v1/search/?q=&city=`), which already returns every listing type — this parses its per-model `results` sections instead of re-implementing per-type search. */
private suspend fun fetchSearchResults(query: String, citySlug: String): List<SearchSection> {
    if (query.isBlank()) return emptyList()
    val encodedQuery = URLEncoder.encode(query, "UTF-8")
    val encodedCity = citySlug.takeIf { it.isNotBlank() }?.let { URLEncoder.encode(it, "UTF-8") }
    val urlString = buildString {
        append(API_BASE_URL)
        append("/api/v1/search/?q=")
        append(encodedQuery)
        if (!encodedCity.isNullOrEmpty()) {
            append("&city=")
            append(encodedCity)
        }
    }
    val json = httpJson(urlString)
    val resultsArray = json.optJSONArray("results") ?: JSONArray()
    val sections = mutableListOf<SearchSection>()
    for (i in 0 until resultsArray.length()) {
        val section = resultsArray.getJSONObject(i)
        val modelKey = section.optString("model_key", "")
        val itemsArray = section.optJSONArray("items") ?: JSONArray()
        val items = mutableListOf<ListingSummary>()
        for (j in 0 until itemsArray.length()) {
            items += parseListingSummary(itemsArray.getJSONObject(j))
        }
        if (items.isNotEmpty()) {
            sections += SearchSection(modelKey, searchSectionLabel(modelKey), searchSectionIcon(modelKey), items)
        }
    }
    return sections
}

/**
 * `GET /api/v1/my/favorites/` (core.api.views.my_favorites) — same
 * heterogeneous, self-describing listing shape as search, so it reuses
 * ListingSummary/parseListingSummary rather than a second parser.
 */
private suspend fun fetchFavorites(page: Int): ApiListPage<ListingSummary> =
    fetchListPage("$API_BASE_URL/api/v1/my/favorites/?page=$page", page, requiresAuth = true) { parseListingSummary(it) }

/** `POST /api/v1/listings/{model_key}/{pk}/favorite/` (toggle_favorite_view) — returns the new favorited state; false means it was just removed. */
private suspend fun toggleFavorite(modelKey: String, id: Int): Boolean =
    httpJson("$API_BASE_URL/api/v1/listings/$modelKey/$id/favorite/", method = "POST", requiresAuth = true)
        .optBoolean("favorited", false)

/**
 * `GET /api/v1/my/listings/` (core.api.views.my_listings_view) — every
 * listing the signed-in user owns, across all 8 listing models, merged and
 * sorted server-side (core/api/views.py:344). Same self-describing shape as
 * favorites/search, so it reuses ListingSummary/parseListingSummary too.
 */
internal suspend fun fetchMyListings(page: Int): ApiListPage<ListingSummary> =
    fetchListPage("$API_BASE_URL/api/v1/my/listings/?page=$page", page, requiresAuth = true) { parseListingSummary(it) }

internal data class NotificationItem(
    val id: Int,
    val type: String,
    val message: String,
    val url: String,
    val isRead: Boolean,
    val createdAt: String,
)

private fun parseNotification(json: JSONObject) = NotificationItem(
    id = json.optInt("id"),
    type = json.optString("type", ""),
    message = json.optString("message", ""),
    url = json.optString("url", ""),
    isRead = json.optBoolean("is_read", false),
    createdAt = json.optString("created_at", ""),
)

/**
 * In-app notification list only (`GET /api/v1/notifications/`, core.api.
 * views.notifications_view) — deliberately NOT OS-level push. The web's
 * push channel (core/push.py) is real browser Web Push via VAPID keys,
 * which has no Android equivalent without a Firebase/FCM project (a new,
 * separate notification backend this phase explicitly does not add); this
 * only connects to the same in-app bell feed the website's navbar shows.
 */
private suspend fun fetchNotifications(page: Int): ApiListPage<NotificationItem> =
    fetchListPage("$API_BASE_URL/api/v1/notifications/?page=$page", page, requiresAuth = true) { parseNotification(it) }

private suspend fun fetchUnreadNotificationCount(): Int =
    httpJson("$API_BASE_URL/api/v1/notifications/unread-count/", requiresAuth = true).optInt("count", 0)

private suspend fun markNotificationRead(id: Int) {
    httpJson("$API_BASE_URL/api/v1/notifications/$id/read/", method = "POST", requiresAuth = true)
}

private suspend fun markAllNotificationsRead() {
    httpJson("$API_BASE_URL/api/v1/notifications/read-all/", method = "POST", requiresAuth = true)
}

internal fun safeWebUri(rawUrl: String): Uri? {
    val uri = rawUrl.trim().toUri()
    return uri.takeIf { it.scheme.equals("https", ignoreCase = true) }
}

/**
 * Coil's own default ImageLoader sizes its disk cache as a percentage of
 * free device storage (effectively unbounded on a roomy device) — this
 * pins an explicit, reasonable cap instead, matching the offline-first
 * spec's "images should also use an appropriate cache strategy" +
 * "reasonable storage limits", the same way OfflineCache caps its own
 * table at MAX_CACHE_ENTRIES. Called once from MainActivity.onCreate.
 */
private fun configureImageLoader(context: Context) {
    val imageLoader = ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.15)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizeBytes(50L * 1024 * 1024)
                .build()
        }
        .build()
    Coil.setImageLoader(imageLoader)
}

@Composable
internal fun rememberOptimizedImageRequest(url: String): ImageRequest {
    val context = LocalContext.current
    return remember(url) {
        ImageRequest.Builder(context)
            .data(safeWebUri(url))
            .size(640)
            .crossfade(false)
            .build()
    }
}

internal class BoundedItemCache<K, V>(private val maximumSize: Int) {
    private val values = LinkedHashMap<K, V>(maximumSize, 0.75f, true)

    @Synchronized
    operator fun get(key: K): V? = values[key]

    @Synchronized
    operator fun set(key: K, value: V) {
        values[key] = value
        while (values.size > maximumSize) values.remove(values.entries.iterator().next().key)
    }
}

private val studentServicesCache = BoundedItemCache<Int, StudentServiceItem>(200)

private fun parseStudentService(result: JSONObject, citySlugFallback: String): StudentServiceItem {
    val city = result.optJSONObject("city")
    val cityName = city?.optString("name") ?: citySlugFallback
    val citySlugValue = city?.optString("slug") ?: citySlugFallback
    val address = result.optString("address", "")
    val item = StudentServiceItem(
        id = result.optInt("id"),
        name = result.optString("name", "Service"),
        category = result.optString("category", "student_services"),
        description = result.optString("description", "").ifBlank { "Service information coming soon." },
        address = address.ifBlank { cityName },
        phoneNumber = result.optString("phone_number", ""),
        website = result.optString("website", ""),
        mapsLink = result.optString("maps_link", ""),
        imageUrl = result.optString("display_image", ""),
        cityName = cityName,
        citySlug = citySlugValue,
        avgRating = result.optDouble("avg_rating", 0.0).let { if (it.isNaN()) 0.0 else it },
        reviewCount = result.optInt("review_count", 0),
        commentCount = result.optInt("comment_count", 0),
    )
    studentServicesCache[item.id] = item
    return item
}

/**
 * Student Services always filters on the real `category=student_services`
 * (core/models.py's Business.CATEGORY_CHOICES) rather than a stand-in list
 * of unrelated generic business categories (school/pharmacy/transport/...)
 * that a previous version of this screen substituted for it.
 */
internal suspend fun fetchStudentServices(query: String, citySlug: String, page: Int): ApiListPage<StudentServiceItem> =
    fetchListPage(buildBusinessCategoryUrl("student_services", query, citySlug, page), page) { parseStudentService(it, citySlug) }

@Composable
private fun StudentServicesFeatureScreen(navController: NavController) {
    var query by rememberSaveable { mutableStateOf("") }
    var cityQuery by rememberSaveable { mutableStateOf(AppCityState.current.value?.name.orEmpty()) }
    var selectedCity by rememberSaveable { mutableStateOf(AppCityState.current.value?.slug) }
    var hasManualCityOverride by rememberSaveable { mutableStateOf(false) }
    var citySuggestions by remember { mutableStateOf<List<CitySuggestion>>(emptyList()) }
    val listState = rememberLazyListState()

    val viewModel: ListingsViewModel<StudentServiceItem> = viewModel(
        factory = viewModelFactory {
            initializer {
                ListingsViewModel(
                    buildUrl = { q, _, citySlug, page -> buildBusinessCategoryUrl("student_services", q, citySlug, page) },
                    fetchPage = { q, _, citySlug, page -> fetchStudentServices(q, citySlug, page) },
                    parseItem = { parseStudentService(it, "") },
                )
            }
        },
    )
    val uiState by viewModel.state.collectAsState()

    val globalCity by AppCityState.current.collectAsState()
    LaunchedEffect(globalCity) {
        if (!hasManualCityOverride) {
            selectedCity = globalCity?.slug
            cityQuery = globalCity?.name.orEmpty()
        }
    }

    LaunchedEffect(query, selectedCity ?: cityQuery) {
        kotlinx.coroutines.delay(300)
        viewModel.load(query, "", selectedCity ?: cityQuery.trim(), reset = true)
    }

    LaunchedEffect(cityQuery) {
        kotlinx.coroutines.delay(300)
        citySuggestions = if (cityQuery.isBlank()) emptyList() else fetchCitySuggestionsSafely(cityQuery)
    }

    val lastVisibleIndex by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 } }
    LaunchedEffect(lastVisibleIndex) {
        if (!uiState.isLoading && !uiState.isLoadingMore && uiState.hasMore && lastVisibleIndex >= uiState.items.size - 3) {
            viewModel.load(query, "", selectedCity ?: cityQuery.trim(), reset = false)
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
                    onValueChange = { cityQuery = it; selectedCity = null; hasManualCityOverride = true },
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
                                    hasManualCityOverride = true
                                },
                                variant = OneTownCityButtonVariant.Outlined,
                            )
                        }
                    }
                }
            }
        }

        if (uiState.isShowingCachedData) {
            OneTownCityCacheStatusBanner(
                message = if (uiState.isRefreshing) "Showing saved results — refreshing…" else "You're offline — showing saved results",
                isOffline = !uiState.isRefreshing,
            )
        }

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    OneTownCityCircularLoading(label = "Loading services")
                }
            }
            uiState.isOfflineNoCache -> {
                OneTownCityEmptyState(
                    title = "You're offline",
                    message = "Student services haven't been loaded yet on this device. Connect to the internet once to load them.",
                    icon = Icons.Outlined.Build,
                    action = {
                        OneTownCityButton(
                            text = "Retry",
                            onClick = { viewModel.load(query, "", selectedCity ?: cityQuery.trim(), reset = true) },
                            variant = OneTownCityButtonVariant.Outlined,
                        )
                    },
                )
            }
            uiState.error != null -> {
                OneTownCityErrorState(
                    title = "Unable to load student services",
                    message = uiState.error ?: "Please try again later.",
                    actionText = "Retry",
                    onRetry = { viewModel.load(query, "", selectedCity ?: cityQuery.trim(), reset = true) },
                )
            }
            uiState.items.isEmpty() -> {
                OneTownCityEmptyState(
                    title = "No student services found",
                    message = "Try a different keyword or city to find nearby support and care options.",
                    icon = Icons.Outlined.Build,
                    action = {
                        OneTownCityButton(
                            text = "Reset filters",
                            onClick = {
                                query = ""
                                cityQuery = ""
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
                    itemsIndexed(uiState.items, key = { _, item -> item.id }) { _, item ->
                        StudentServiceCard(
                            item = item,
                            onClick = { navController.navigate("student-service/${item.id}") },
                        )
                    }
                    if (uiState.isLoadingMore) {
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
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
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
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )

                if (item.phoneNumber.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Call,
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
                                val uri = "tel:${item.phoneNumber}".toUri()
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
                val mapsUri = safeWebUri(item.mapsLink)
                    ?: "https://www.google.com/maps/search/?api=1&query=${URLEncoder.encode(item.address.ifBlank { item.name }, "UTF-8")}".toUri()
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

private val scholarshipTypeFilters = listOf(
    "All", "government", "merit", "need_based", "minority", "sports", "research", "other",
)

private fun scholarshipTypeLabel(value: String): String = when (value) {
    "All" -> "All"
    "government" -> "Government"
    "merit" -> "Merit"
    "need_based" -> "Need-based"
    "minority" -> "Minority"
    "sports" -> "Sports"
    "research" -> "Research"
    else -> "Other"
}

@Composable
private fun ScholarshipsFeatureScreen(navController: NavController) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var cityQuery by rememberSaveable { mutableStateOf(AppCityState.current.value?.name.orEmpty()) }
    var selectedCity by rememberSaveable(stateSaver = CitySuggestionSaver) { mutableStateOf(AppCityState.current.value?.let { CitySuggestion(it.slug, it.name) }) }
    var hasManualCityOverride by rememberSaveable { mutableStateOf(false) }
    var citySuggestions by remember { mutableStateOf<List<CitySuggestion>>(emptyList()) }
    val listState = rememberLazyListState()

    // scholarship_type has no server-side filter param (core/api/views.py's
    // _list_listings only supports category=<Business choice> or
    // listing_category__key, neither of which apply to Scholarship) — the
    // real scholarship_type value is always fetched, then refined client-
    // side via filterScholarships, same as before this ViewModel migration.
    val viewModel: ListingsViewModel<ScholarshipItem> = viewModel(
        factory = viewModelFactory {
            initializer {
                ListingsViewModel(
                    buildUrl = { q, _, citySlug, page -> buildScholarshipsUrl(q, citySlug, page) },
                    fetchPage = { q, _, citySlug, page -> fetchScholarships(q, citySlug, page) },
                    parseItem = { parseScholarship(it) },
                )
            }
        },
    )
    val uiState by viewModel.state.collectAsState()
    val displayItems = remember(uiState.items, selectedCategory) {
        filterScholarships(uiState.items, "", selectedCategory)
    }

    val globalCity by AppCityState.current.collectAsState()
    LaunchedEffect(globalCity) {
        if (!hasManualCityOverride) {
            selectedCity = globalCity?.let { CitySuggestion(it.slug, it.name) }
            cityQuery = globalCity?.name.orEmpty()
        }
    }

    LaunchedEffect(query, selectedCity?.slug ?: cityQuery) {
        kotlinx.coroutines.delay(300)
        viewModel.load(query, "", selectedCity?.slug ?: cityQuery.trim(), reset = true)
    }

    LaunchedEffect(cityQuery) {
        kotlinx.coroutines.delay(300)
        citySuggestions = if (cityQuery.isBlank()) emptyList() else fetchCitySuggestionsSafely(cityQuery)
    }

    val lastVisibleIndex by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 } }
    LaunchedEffect(lastVisibleIndex) {
        if (!uiState.isLoading && !uiState.isLoadingMore && uiState.hasMore && lastVisibleIndex >= displayItems.size - 3) {
            viewModel.load(query, "", selectedCity?.slug ?: cityQuery.trim(), reset = false)
        }
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
                    onValueChange = { cityQuery = it; selectedCity = null; hasManualCityOverride = true },
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
                                    hasManualCityOverride = true
                                },
                                variant = OneTownCityButtonVariant.Outlined,
                            )
                        }
                    }
                }
            }
        }

        run {
            val labelByKey = scholarshipTypeFilters.associateWith { scholarshipTypeLabel(it) }
            val keyByLabel = labelByKey.entries.associate { (key, label) -> label to key }
            OneTownCityChipGroup(
                items = scholarshipTypeFilters.map { labelByKey.getValue(it) },
                selected = setOf(labelByKey.getValue(selectedCategory)),
                onSelected = { label -> selectedCategory = keyByLabel[label] ?: "All" },
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
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    OneTownCityCircularLoading(label = "Loading scholarship data")
                }
            }
            uiState.isOfflineNoCache -> {
                OneTownCityEmptyState(
                    title = "You're offline",
                    message = "Scholarships haven't been loaded yet on this device. Connect to the internet once to load them.",
                    icon = Icons.Outlined.EmojiEvents,
                    action = {
                        OneTownCityButton(
                            text = "Retry",
                            onClick = { viewModel.load(query, "", selectedCity?.slug ?: cityQuery.trim(), reset = true) },
                            variant = OneTownCityButtonVariant.Outlined,
                        )
                    },
                )
            }
            uiState.error != null -> {
                OneTownCityErrorState(
                    title = "Unable to load scholarship data",
                    message = uiState.error ?: "Please try again later.",
                    actionText = "Retry",
                    onRetry = { viewModel.load(query, "", selectedCity?.slug ?: cityQuery.trim(), reset = true) },
                )
            }
            displayItems.isEmpty() -> {
                OneTownCityEmptyState(
                    title = "No scholarships found",
                    message = "No scholarships or schemes match your current search, city, or type filter.",
                    icon = Icons.Outlined.EmojiEvents,
                    action = {
                        OneTownCityButton(
                            text = "Reset filters",
                            onClick = {
                                query = ""
                                selectedCategory = "All"
                                cityQuery = ""
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
                    itemsIndexed(displayItems, key = { _, item -> item.id }) { _, item ->
                        ScholarshipCard(item = item, onClick = { navController.navigate("scholarship/${item.id}") })
                    }
                    if (uiState.isLoadingMore) {
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
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            if (item.cityName.isNotBlank()) {
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
                        text = item.cityName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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

            if (item.provider.isNotBlank()) {
                Text(
                    text = item.provider,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (item.deadline.isNotBlank()) {
                Text(
                    text = "Deadline: ${item.deadline}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )

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
                        if (item.cityName.isNotBlank()) {
                            DetailRow(icon = Icons.Filled.LocationOn, title = item.cityName)
                        }
                        if (item.category.isNotBlank()) {
                            DetailRow(icon = Icons.Outlined.EmojiEvents, title = item.category)
                        }
                        if (item.provider.isNotBlank()) {
                            DetailRow(icon = Icons.Filled.Info, title = item.provider)
                        }
                        if (item.deadline.isNotBlank()) {
                            DetailRow(icon = Icons.Filled.Info, title = "Deadline: ${item.deadline}")
                        }
                        if (item.eligibility.isNotBlank()) {
                            DetailRow(icon = Icons.Filled.Check, title = "Eligibility: ${item.eligibility}")
                        }
                        if (item.contactNumber.isNotBlank()) {
                            DetailRow(icon = Icons.Filled.Call, title = item.contactNumber)
                        }
                        if (item.sourceLabel.isNotBlank()) {
                            DetailRow(icon = Icons.Filled.Language, title = item.sourceLabel)
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (item.contactNumber.isNotBlank()) {
                        OneTownCityButton(
                            text = "Call",
                            onClick = { activity?.startActivity(Intent(Intent.ACTION_DIAL, "tel:${item.contactNumber}".toUri())) },
                            leadingIcon = Icons.Filled.Call,
                            variant = OneTownCityButtonVariant.Primary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (item.officialUrl.isNotBlank()) {
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
                            variant = OneTownCityButtonVariant.Secondary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            item {
                ReviewsSection(
                    modelKey = "scholarship",
                    listingId = item.id,
                    avgRating = item.avgRating,
                    reviewCount = item.reviewCount,
                    navController = navController,
                )
            }
            item {
                CommentsSection(
                    modelKey = "scholarship",
                    listingId = item.id,
                    commentCount = item.commentCount,
                    navController = navController,
                )
            }
        }
    }
}

@Composable
private fun LostFoundFeatureScreen(navController: NavController) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var cityQuery by rememberSaveable { mutableStateOf(AppCityState.current.value?.name.orEmpty()) }
    var selectedCity by rememberSaveable(stateSaver = CitySuggestionSaver) { mutableStateOf(AppCityState.current.value?.let { CitySuggestion(it.slug, it.name) }) }
    var hasManualCityOverride by rememberSaveable { mutableStateOf(false) }
    var citySuggestions by remember { mutableStateOf<List<CitySuggestion>>(emptyList()) }
    val listState = rememberLazyListState()
    val categories = listOf("All", "Lost", "Found")

    // The generic listing API has no report_type query param (see
    // fetchLostFound's doc comment) — Lost/Found is always refined
    // client-side via filterLostFound, same as before this migration.
    val viewModel: ListingsViewModel<LostFoundItem> = viewModel(
        factory = viewModelFactory {
            initializer {
                ListingsViewModel(
                    buildUrl = { q, _, citySlug, page -> buildLostFoundUrl(q, citySlug, page) },
                    fetchPage = { q, _, citySlug, page -> fetchLostFound(q, citySlug, page) },
                    parseItem = { parseLostFound(it, "") },
                )
            }
        },
    )
    val uiState by viewModel.state.collectAsState()
    val displayItems = remember(uiState.items, selectedCategory) {
        filterLostFound(uiState.items, "", selectedCategory)
    }

    val globalCity by AppCityState.current.collectAsState()
    LaunchedEffect(globalCity) {
        if (!hasManualCityOverride) {
            selectedCity = globalCity?.let { CitySuggestion(it.slug, it.name) }
            cityQuery = globalCity?.name.orEmpty()
        }
    }

    LaunchedEffect(query, selectedCity?.slug ?: cityQuery) {
        kotlinx.coroutines.delay(300)
        viewModel.load(query, "", selectedCity?.slug ?: cityQuery.trim(), reset = true)
    }

    LaunchedEffect(cityQuery) {
        kotlinx.coroutines.delay(300)
        citySuggestions = if (cityQuery.isBlank()) emptyList() else fetchCitySuggestionsSafely(cityQuery)
    }

    val lastVisibleIndex by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 } }
    LaunchedEffect(lastVisibleIndex) {
        if (!uiState.isLoading && !uiState.isLoadingMore && uiState.hasMore && lastVisibleIndex >= displayItems.size - 3) {
            viewModel.load(query, "", selectedCity?.slug ?: cityQuery.trim(), reset = false)
        }
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
                    onValueChange = { cityQuery = it; selectedCity = null; hasManualCityOverride = true },
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
                                    hasManualCityOverride = true
                                },
                                variant = OneTownCityButtonVariant.Outlined,
                            )
                        }
                    }
                }
            }
        }

        OneTownCityChipGroup(
            items = categories,
            selected = setOf(selectedCategory),
            onSelected = { selectedCategory = it },
        )

        if (uiState.isShowingCachedData) {
            OneTownCityCacheStatusBanner(
                message = if (uiState.isRefreshing) "Showing saved results — refreshing…" else "You're offline — showing saved results",
                isOffline = !uiState.isRefreshing,
            )
        }

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    OneTownCityCircularLoading(label = "Loading lost & found reports")
                }
            }
            uiState.isOfflineNoCache -> {
                OneTownCityEmptyState(
                    title = "You're offline",
                    message = "Lost & Found reports haven't been loaded yet on this device. Connect to the internet once to load them.",
                    icon = Icons.Outlined.Search,
                    action = {
                        OneTownCityButton(
                            text = "Retry",
                            onClick = { viewModel.load(query, "", selectedCity?.slug ?: cityQuery.trim(), reset = true) },
                            variant = OneTownCityButtonVariant.Outlined,
                        )
                    },
                )
            }
            uiState.error != null -> {
                OneTownCityErrorState(
                    title = "Unable to load Lost & Found",
                    message = uiState.error ?: "Please try again later.",
                    actionText = "Retry",
                    onRetry = { viewModel.load(query, "", selectedCity?.slug ?: cityQuery.trim(), reset = true) },
                )
            }
            displayItems.isEmpty() -> {
                OneTownCityEmptyState(
                    title = "No reports found",
                    message = "No lost or found reports match your current search, city, or type filter.",
                    icon = Icons.Outlined.Search,
                    action = {
                        OneTownCityButton(
                            text = "Reset filters",
                            onClick = {
                                query = ""
                                selectedCategory = "All"
                                cityQuery = ""
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
                    itemsIndexed(displayItems, key = { _, item -> item.id }) { _, item ->
                        LostFoundCard(item = item, onClick = { navController.navigate("lost-found/${item.id}") })
                    }
                    if (uiState.isLoadingMore) {
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

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (item.location.isNotBlank()) {
                DetailRow(icon = Icons.Filled.LocationOn, title = item.location)
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
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
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
                                val uri = "tel:${item.contact}".toUri()
                                val intent = Intent(Intent.ACTION_DIAL, uri)
                                activity?.startActivity(intent)
                            },
                            leadingIcon = Icons.Filled.Call,
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

            item {
                ReviewsSection(
                    modelKey = "lostfound",
                    listingId = item.id,
                    avgRating = item.avgRating,
                    reviewCount = item.reviewCount,
                    navController = navController,
                )
            }
            item {
                CommentsSection(
                    modelKey = "lostfound",
                    listingId = item.id,
                    commentCount = item.commentCount,
                    navController = navController,
                )
            }
        }
    }
}

@Composable
private fun PlacesToVisitFeatureScreen(navController: NavController) {
    var query by rememberSaveable { mutableStateOf("") }
    var cityQuery by rememberSaveable { mutableStateOf(AppCityState.current.value?.name.orEmpty()) }
    var selectedCity by rememberSaveable(stateSaver = CitySuggestionSaver) { mutableStateOf(AppCityState.current.value?.let { CitySuggestion(it.slug, it.name) }) }
    var hasManualCityOverride by rememberSaveable { mutableStateOf(false) }
    var citySuggestions by remember { mutableStateOf<List<CitySuggestion>>(emptyList()) }
    val localContext = LocalContext.current
    var permissionState by remember { mutableStateOf(resolveLocationPermissionState(localContext)) }
    val listState = rememberLazyListState()

    val viewModel: ListingsViewModel<PlaceItem> = viewModel(
        factory = viewModelFactory {
            initializer {
                ListingsViewModel(
                    buildUrl = { q, _, citySlug, page -> buildBusinessCategoryUrl("tourism", q, citySlug, page) },
                    fetchPage = { q, _, citySlug, page -> fetchPlacesToVisit(q, citySlug, page) },
                    parseItem = { parsePlace(it, "") },
                )
            }
        },
    )
    val uiState by viewModel.state.collectAsState()

    val globalCity by AppCityState.current.collectAsState()
    LaunchedEffect(globalCity) {
        if (!hasManualCityOverride) {
            selectedCity = globalCity?.let { CitySuggestion(it.slug, it.name) }
            cityQuery = globalCity?.name.orEmpty()
        }
    }

    LaunchedEffect(query, selectedCity?.slug ?: cityQuery) {
        kotlinx.coroutines.delay(300)
        viewModel.load(query, "", selectedCity?.slug ?: cityQuery.trim(), reset = true)
    }

    LaunchedEffect(cityQuery) {
        kotlinx.coroutines.delay(300)
        citySuggestions = if (cityQuery.isBlank()) emptyList() else fetchCitySuggestionsSafely(cityQuery)
    }

    val lastVisibleIndex by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 } }
    LaunchedEffect(lastVisibleIndex) {
        if (!uiState.isLoading && !uiState.isLoadingMore && uiState.hasMore && lastVisibleIndex >= uiState.items.size - 3) {
            viewModel.load(query, "", selectedCity?.slug ?: cityQuery.trim(), reset = false)
        }
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
                    onValueChange = { cityQuery = it; selectedCity = null; hasManualCityOverride = true },
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
                                    hasManualCityOverride = true
                                },
                                variant = OneTownCityButtonVariant.Outlined,
                            )
                        }
                    }
                }
                val permissionHint = when (permissionState) {
                    LocationPermissionState.GRANTED -> "Location permission granted. Browsing uses your chosen city below."
                    LocationPermissionState.DENIED -> "Location permission denied. Browsing still works using the city filter."
                    LocationPermissionState.REVOKED -> "Location permission is unavailable. Browsing remains available using your chosen city."
                    LocationPermissionState.UNAVAILABLE -> "Location unavailable on this device. City-based browsing remains available."
                }
                Text(
                    text = permissionHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (uiState.isShowingCachedData) {
            OneTownCityCacheStatusBanner(
                message = if (uiState.isRefreshing) "Showing saved results — refreshing…" else "You're offline — showing saved results",
                isOffline = !uiState.isRefreshing,
            )
        }

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    OneTownCityCircularLoading(label = "Loading places")
                }
            }
            uiState.isOfflineNoCache -> {
                OneTownCityEmptyState(
                    title = "You're offline",
                    message = "Places to visit haven't been loaded yet on this device. Connect to the internet once to load them.",
                    icon = Icons.Outlined.LocationOn,
                    action = {
                        OneTownCityButton(
                            text = "Retry",
                            onClick = { viewModel.load(query, "", selectedCity?.slug ?: cityQuery.trim(), reset = true) },
                            variant = OneTownCityButtonVariant.Outlined,
                        )
                    },
                )
            }
            uiState.error != null -> {
                OneTownCityErrorState(
                    title = "Unable to load places",
                    message = uiState.error ?: "Please try again later.",
                    actionText = "Retry",
                    onRetry = { viewModel.load(query, "", selectedCity?.slug ?: cityQuery.trim(), reset = true) },
                )
            }
            uiState.items.isEmpty() -> {
                OneTownCityEmptyState(
                    title = "No places found",
                    message = "No nearby attractions match your current search and city filter.",
                    icon = Icons.Outlined.LocationOn,
                    action = {
                        OneTownCityButton(
                            text = "Reset filters",
                            onClick = {
                                query = ""
                                selectedCity = null
                                cityQuery = ""
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
                    itemsIndexed(uiState.items, key = { _, item -> item.id }) { _, item ->
                        PlaceCard(item = item, onClick = { navController.navigate("place/${item.id}") })
                    }
                    if (uiState.isLoadingMore) {
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
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.address.isNotBlank()) {
                DetailRow(icon = Icons.Filled.LocationOn, title = item.address)
            }
            Text(
                text = item.category,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            if (item.distanceLabel.isNotBlank()) {
                Text(
                    text = item.distanceLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

internal data class EventItem(
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


private const val EVENTS_API_BASE_URL = "https://onetowncity.com"
private val eventCache = BoundedItemCache<Int, EventItem>(200)

private var eventsFetcher: suspend (String, String, Int, Boolean) -> ApiListPage<EventItem> = { query, citySlug, page, restrictToUpcoming ->
    fetchEvents(query, citySlug, page, restrictToUpcoming)
}

/**
 * `upcoming=true` (core/api/views.py: `qs.filter(event_date__gte=timezone.
 * localdate())`) is only appropriate when the caller actually wants
 * future-only events. It used to be appended unconditionally, which meant
 * selecting the "All" date filter in EventsFeatureScreen still silently
 * asked the server for upcoming-only events — past events could never be
 * shown no matter what the UI filter said. [restrictToUpcoming] defaults to
 * true so every other existing caller (preloading, the This week/This
 * month/Upcoming filters) keeps its prior behavior unchanged.
 */
internal fun buildEventsUrl(query: String, citySlug: String, page: Int, pageSize: Int, restrictToUpcoming: Boolean = true): String {
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
        if (restrictToUpcoming) append("&upcoming=true")
        if (!encodedCity.isNullOrEmpty()) {
            append("&city=")
            append(encodedCity)
        }
    }
}

private fun parseEvent(result: JSONObject, citySlugFallback: String): EventItem {
    val city = result.optJSONObject("city")
    val cityName = city?.optString("name") ?: citySlugFallback
    val citySlugValue = city?.optString("slug") ?: citySlugFallback
    val location = result.optString("location", "")
    val item = EventItem(
        id = result.optInt("id"),
        title = result.optString("title", "Event"),
        description = result.optString("description", "").ifBlank { "Local event details coming soon." },
        location = location.ifBlank { cityName },
        contactNumber = result.optString("contact_number", ""),
        eventDate = result.optString("event_date", ""),
        imageUrl = result.optString("display_image", ""),
        cityName = cityName,
        citySlug = citySlugValue,
        detailUrl = result.optString("url", ""),
    )
    eventCache[item.id] = item
    return item
}

internal suspend fun fetchEvents(
    query: String,
    citySlug: String,
    page: Int,
    restrictToUpcoming: Boolean = true,
): ApiListPage<EventItem> =
    fetchListPage(buildEventsUrl(query, citySlug, page, 10, restrictToUpcoming), page) { parseEvent(it, citySlug) }

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
    var cityQuery by rememberSaveable { mutableStateOf(AppCityState.current.value?.name.orEmpty()) }
    var selectedCity by rememberSaveable { mutableStateOf(AppCityState.current.value?.slug) }
    var hasManualCityOverride by rememberSaveable { mutableStateOf(false) }
    var dateFilter by rememberSaveable { mutableStateOf(EventDateFilter.UPCOMING) }
    var citySuggestions by remember { mutableStateOf<List<CitySuggestion>>(emptyList()) }
    val listState = rememberLazyGridState()

    // dateFilter's finer THIS_WEEK/THIS_MONTH/UPCOMING distinctions are
    // refined client-side via matchesDateFilter (the server only knows
    // upcoming vs. not) — but ALL vs. not-ALL changes the actual network
    // query: only ALL asks the server for past events too, so it's threaded
    // through ListingsViewModel's categoryKey slot ("all"/"upcoming") to
    // vary the fetch (and its offline cache key) rather than being purely
    // a client-side re-filter like the rest of dateFilter.
    val isAllDates = dateFilter == EventDateFilter.ALL
    val dateMode = if (isAllDates) "all" else "upcoming"

    val viewModel: ListingsViewModel<EventItem> = viewModel(
        factory = viewModelFactory {
            initializer {
                ListingsViewModel(
                    buildUrl = { q, mode, citySlug, page -> buildEventsUrl(q, citySlug, page, 10, restrictToUpcoming = mode != "all") },
                    fetchPage = { q, mode, citySlug, page -> eventsFetcher(q, citySlug, page, mode != "all") },
                    parseItem = { parseEvent(it, "") },
                )
            }
        },
    )
    val uiState by viewModel.state.collectAsState()
    val displayItems = remember(uiState.items, dateFilter) {
        uiState.items.distinctBy { it.id }.filter { matchesDateFilter(it.eventDate, dateFilter) }
    }

    val globalCity by AppCityState.current.collectAsState()
    LaunchedEffect(globalCity) {
        if (!hasManualCityOverride) {
            selectedCity = globalCity?.slug
            cityQuery = globalCity?.name.orEmpty()
        }
    }

    LaunchedEffect(query, selectedCity ?: cityQuery, isAllDates) {
        kotlinx.coroutines.delay(300)
        viewModel.load(query, dateMode, selectedCity ?: cityQuery.trim(), reset = true)
    }

    LaunchedEffect(cityQuery) {
        kotlinx.coroutines.delay(300)
        if (cityQuery.isBlank()) {
            citySuggestions = emptyList()
        } else {
            citySuggestions = fetchCitySuggestions(cityQuery)
        }
    }

    val lastVisibleIndex by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 } }
    LaunchedEffect(lastVisibleIndex) {
        if (!uiState.isLoading && !uiState.isLoadingMore && uiState.hasMore && lastVisibleIndex >= displayItems.size - 3) {
            viewModel.load(query, dateMode, selectedCity ?: cityQuery.trim(), reset = false)
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
                    onValueChange = { cityQuery = it; selectedCity = null; hasManualCityOverride = true },
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
                                    hasManualCityOverride = true
                                },
                                variant = OneTownCityButtonVariant.Outlined,
                            )
                        }
                    }
                }
                OneTownCityChipGroup(
                    items = EventDateFilter.entries.map { it.label },
                    selected = setOf(dateFilter.label),
                    onSelected = { label -> dateFilter = EventDateFilter.entries.first { it.label == label } },
                )
            }
        }

        if (uiState.isShowingCachedData) {
            OneTownCityCacheStatusBanner(
                message = if (uiState.isRefreshing) "Showing saved results — refreshing…" else "You're offline — showing saved results",
                isOffline = !uiState.isRefreshing,
            )
        }

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    OneTownCityCircularLoading(label = "Loading events")
                }
            }
            uiState.isOfflineNoCache -> {
                OneTownCityEmptyState(
                    title = "You're offline",
                    message = "Events haven't been loaded yet on this device. Connect to the internet once to load them.",
                    icon = Icons.Outlined.Event,
                    action = {
                        OneTownCityButton(
                            text = "Retry",
                            onClick = { viewModel.load(query, dateMode, selectedCity ?: cityQuery.trim(), reset = true) },
                            variant = OneTownCityButtonVariant.Outlined,
                        )
                    },
                )
            }
            uiState.error != null -> {
                OneTownCityErrorState(
                    title = "Unable to load events",
                    message = uiState.error ?: "Please try again later.",
                    actionText = "Retry",
                    onRetry = { viewModel.load(query, dateMode, selectedCity ?: cityQuery.trim(), reset = true) },
                )
            }
            displayItems.isEmpty() -> {
                OneTownCityEmptyState(
                    title = "No events found",
                    message = "Try a different city, keyword, or date filter to discover upcoming opportunities in your area.",
                    icon = Icons.Outlined.Event,
                    action = {
                        OneTownCityButton(
                            text = "Reset filters",
                            onClick = {
                                query = ""
                                cityQuery = ""
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
                        itemsIndexed(displayItems, key = { _, item -> item.id }) { _, item ->
                            EventCard(
                                item = item,
                                onClick = { navController.navigate("event/${item.id}") },
                            )
                        }
                        if (uiState.isLoadingMore) {
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
                    shape = RoundedCornerShape(12.dp),
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
                                val uri = "tel:${item.contactNumber}".toUri()
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

internal data class TuitionCenterItem(
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
    val avgRating: Double = 0.0,
    val reviewCount: Int = 0,
    val commentCount: Int = 0,
)

internal data class CitySuggestion(
    val slug: String,
    val name: String,
)

/**
 * Without this, every `var selectedCity by remember { mutableStateOf(...) }`
 * across the browse screens loses its value on rotation/process death —
 * `remember` (not rememberSaveable) never survives either — while the
 * screen's own `cityQuery` text (a plain String, natively saveable) does.
 * The resulting fetch then falls back to `cityQuery.trim()` as the `city=`
 * query param instead of the real slug, and core/api/views.py's
 * _resolve_city does an exact `filter(slug=slug)` match: a display name
 * essentially never equals its own slug (casing/spacing differ), so the
 * filter silently matches nothing and the backend returns every city's
 * results unfiltered, with no error surfaced anywhere.
 */
internal val CitySuggestionSaver = listSaver<CitySuggestion?, String>(
    save = { suggestion -> suggestion?.let { listOf(it.slug, it.name) } ?: emptyList() },
    restore = { saved -> if (saved.size == 2) CitySuggestion(slug = saved[0], name = saved[1]) else null },
)

private val tuitionCenterCache = BoundedItemCache<Int, TuitionCenterItem>(200)

private var tuitionCentersFetcher: suspend (String, String, Int) -> ApiListPage<TuitionCenterItem> = { query, citySlug, page ->
    fetchTuitionCenters(query, citySlug, page)
}

private var citySuggestionsFetcher: suspend (String) -> List<CitySuggestion> = { query ->
    fetchCitySuggestions(query)
}

private fun parseTuitionCenter(result: JSONObject, citySlugFallback: String): TuitionCenterItem {
    val city = result.optJSONObject("city")
    val cityName = city?.optString("name") ?: citySlugFallback
    val citySlugValue = city?.optString("slug") ?: citySlugFallback
    val address = result.optString("address", "")
    val item = TuitionCenterItem(
        id = result.optInt("id"),
        name = result.optString("name", "Tuition Center"),
        category = result.optString("category", "tuition_center"),
        subtitle = address.ifBlank { cityName },
        description = result.optString("description", "").ifBlank { "Academic support and coaching options in your area." },
        phoneNumber = result.optString("phone_number", ""),
        website = result.optString("website", ""),
        mapsLink = result.optString("maps_link", ""),
        imageUrl = result.optString("display_image", ""),
        cityName = cityName,
        citySlug = citySlugValue,
        avgRating = result.optDouble("avg_rating", 0.0).let { if (it.isNaN()) 0.0 else it },
        reviewCount = result.optInt("review_count", 0),
        commentCount = result.optInt("comment_count", 0),
    )
    tuitionCenterCache[item.id] = item
    return item
}

/**
 * Tuition Centers always filters on the real `category=tuition_center`
 * (core/models.py's Business.CATEGORY_CHOICES) — a previous version of this
 * screen substituted a plain keyword search (`q=tuition`) for the category
 * filter, which missed listings that don't literally say "tuition" and
 * could match unrelated ones that happen to mention it.
 */
internal suspend fun fetchTuitionCenters(
    query: String,
    citySlug: String,
    page: Int,
): ApiListPage<TuitionCenterItem> =
    fetchListPage(buildBusinessCategoryUrl("tuition_center", query, citySlug, page), page) { parseTuitionCenter(it, citySlug) }

private suspend fun fetchCitySuggestions(cityQuery: String): List<CitySuggestion> = withContext(Dispatchers.IO) {
    if (cityQuery.isBlank()) return@withContext emptyList()
    val encoded = URLEncoder.encode(cityQuery, "UTF-8")
    val url = URL("${API_BASE_URL}/api/v1/locations/cities/?q=$encoded")
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

internal suspend fun fetchCitySuggestionsSafely(cityQuery: String): List<CitySuggestion> =
    fetchCitySuggestionsSafely(cityQuery, ::fetchCitySuggestions)

@Composable
internal fun TuitionCentersFeatureScreen(navController: NavController) {
    var query by rememberSaveable { mutableStateOf("") }
    var cityQuery by rememberSaveable { mutableStateOf(AppCityState.current.value?.name.orEmpty()) }
    var selectedCity by rememberSaveable(stateSaver = CitySuggestionSaver) { mutableStateOf(AppCityState.current.value?.let { CitySuggestion(it.slug, it.name) }) }
    var hasManualCityOverride by rememberSaveable { mutableStateOf(false) }
    var citySuggestions by remember { mutableStateOf<List<CitySuggestion>>(emptyList()) }
    val listState = rememberLazyListState()

    val viewModel: ListingsViewModel<TuitionCenterItem> = viewModel(
        factory = viewModelFactory {
            initializer {
                ListingsViewModel(
                    buildUrl = { q, _, citySlug, page -> buildBusinessCategoryUrl("tuition_center", q, citySlug, page) },
                    fetchPage = { q, _, citySlug, page -> tuitionCentersFetcher(q, citySlug, page) },
                    parseItem = { parseTuitionCenter(it, "") },
                )
            }
        },
    )
    val uiState by viewModel.state.collectAsState()

    val globalCity by AppCityState.current.collectAsState()
    LaunchedEffect(globalCity) {
        if (!hasManualCityOverride) {
            selectedCity = globalCity?.let { CitySuggestion(it.slug, it.name) }
            cityQuery = globalCity?.name.orEmpty()
        }
    }

    LaunchedEffect(query, selectedCity?.slug ?: cityQuery) {
        kotlinx.coroutines.delay(300)
        viewModel.load(query, "", selectedCity?.slug ?: cityQuery.trim(), reset = true)
    }

    LaunchedEffect(cityQuery) {
        kotlinx.coroutines.delay(300)
        citySuggestions = citySuggestionsFetcher(cityQuery)
    }

    val lastVisibleIndex by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 } }
    LaunchedEffect(lastVisibleIndex) {
        if (!uiState.isLoading && !uiState.isLoadingMore && uiState.hasMore && lastVisibleIndex >= uiState.items.size - 3) {
            viewModel.load(query, "", selectedCity?.slug ?: cityQuery.trim(), reset = false)
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
                    onValueChange = { cityQuery = it; selectedCity = null; hasManualCityOverride = true },
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
                                    hasManualCityOverride = true
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

        if (uiState.isShowingCachedData) {
            OneTownCityCacheStatusBanner(
                message = if (uiState.isRefreshing) "Showing saved results — refreshing…" else "You're offline — showing saved results",
                isOffline = !uiState.isRefreshing,
            )
        }

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    OneTownCityCircularLoading(label = "Loading tuition centers")
                }
            }
            uiState.isOfflineNoCache -> {
                OneTownCityEmptyState(
                    title = "You're offline",
                    message = "Tuition centers haven't been loaded yet on this device. Connect to the internet once to load them.",
                    icon = Icons.Outlined.School,
                    action = {
                        OneTownCityButton(
                            text = "Retry",
                            onClick = { viewModel.load(query, "", selectedCity?.slug ?: cityQuery.trim(), reset = true) },
                            variant = OneTownCityButtonVariant.Outlined,
                        )
                    },
                )
            }
            uiState.error != null -> {
                OneTownCityErrorState(
                    title = "Unable to load tuition centers",
                    message = uiState.error ?: "Please try again later.",
                    actionText = "Retry",
                    onRetry = { viewModel.load(query, "", selectedCity?.slug ?: cityQuery.trim(), reset = true) },
                )
            }
            uiState.items.isEmpty() -> {
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
                                cityQuery = ""
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
                    itemsIndexed(uiState.items, key = { _, item -> item.id }) { _, item ->
                        TuitionCenterCard(
                            item = item,
                            onClick = { navController.navigate("tuition-center/${item.id}") },
                        )
                    }
                    if (uiState.isLoadingMore) {
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
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
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
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )

                if (item.phoneNumber.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Call,
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
                                val uri = "tel:${item.phoneNumber}".toUri()
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
                val mapsUri = safeWebUri(item.mapsLink)
                    ?: "https://www.google.com/maps/search/?api=1&query=${URLEncoder.encode(item.name, "UTF-8")}".toUri()
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

@Composable
internal fun DetailRow(icon: ImageVector, title: String) {
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
internal fun SignInRequiredState(message: String, navController: NavController) {
    OneTownCityEmptyState(
        title = "Sign in required",
        message = message,
        icon = Icons.Filled.Person,
        action = {
            OneTownCityButton(
                text = "Sign in",
                onClick = { navController.navigate("sign-in") },
                variant = OneTownCityButtonVariant.Primary,
            )
        },
    )
}

@Composable
private fun FavoritesScreen(navController: NavController) {
    var items by remember { mutableStateOf<List<ListingSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var requiresSignIn by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(1) }
    val coroutineScope = rememberCoroutineScope()
    var activeRequest by remember { mutableStateOf<Job?>(null) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val activity = context as? Activity

    fun loadPage(reset: Boolean = false) {
        activeRequest?.cancel()
        activeRequest = coroutineScope.launch {
            if (reset) {
                isLoading = true
                isLoadingMore = false
                page = 1
                error = null
                requiresSignIn = false
            } else {
                if (!hasMore || isLoadingMore) return@launch
                isLoadingMore = true
            }
            try {
                val pageToLoad = if (reset) 1 else page
                val result = fetchFavorites(pageToLoad)
                items = if (reset) result.items else items + result.items
                hasMore = result.nextPage != null
                page = result.nextPage ?: (pageToLoad + 1)
                error = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthRequiredException) {
                items = emptyList()
                requiresSignIn = true
            } catch (e: Exception) {
                if (reset) {
                    items = emptyList()
                    error = e.message ?: "Unable to load favorites right now."
                } else {
                    error = e.message ?: "Unable to load more favorites."
                }
            } finally {
                isLoading = false
                isLoadingMore = false
            }
        }
    }

    LaunchedEffect(Unit) { loadPage(reset = true) }

    val lastVisibleIndex by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 } }
    LaunchedEffect(lastVisibleIndex) {
        if (!isLoading && !isLoadingMore && hasMore && lastVisibleIndex >= items.size - 3) {
            loadPage(reset = false)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Saved", style = MaterialTheme.typography.headlineSmall)

        when {
            requiresSignIn -> SignInRequiredState(message = "Sign in to save and view your favorite listings.", navController = navController)
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    OneTownCityCircularLoading(label = "Loading favorites")
                }
            }
            error != null -> {
                OneTownCityErrorState(
                    title = "Unable to load favorites",
                    message = error ?: "Please try again later.",
                    actionText = "Retry",
                    onRetry = { loadPage(reset = true) },
                )
            }
            items.isEmpty() -> {
                OneTownCityEmptyState(
                    title = "No favorites yet",
                    message = "Listings you favorite will appear here.",
                    icon = Icons.Filled.Star,
                    action = {
                        OneTownCityButton(
                            text = "Refresh",
                            onClick = { loadPage(reset = true) },
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
                    itemsIndexed(items, key = { _, item -> "${item.modelKey}-${item.id}" }) { _, item ->
                        FavoriteCard(
                            item = item,
                            onOpen = {
                                safeWebUri(API_BASE_URL + item.url)?.let { uri ->
                                    try {
                                        activity?.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    } catch (_: ActivityNotFoundException) { }
                                }
                            },
                            onRemove = {
                                coroutineScope.launch {
                                    try {
                                        val stillFavorited = toggleFavorite(item.modelKey, item.id)
                                        if (!stillFavorited) {
                                            items = items.filterNot { it.modelKey == item.modelKey && it.id == item.id }
                                        }
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (_: Exception) { }
                                }
                            },
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
private fun FavoriteCard(
    item: ListingSummary,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = rememberOptimizedImageRequest(item.imageUrl),
                    contentDescription = item.title,
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (item.subtitle.isNotBlank()) {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box(
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp).clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = Icons.Filled.Star, contentDescription = "Remove from favorites", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun NotificationsScreen(navController: NavController) {
    var items by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var requiresSignIn by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(1) }
    val coroutineScope = rememberCoroutineScope()
    var activeRequest by remember { mutableStateOf<Job?>(null) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val activity = context as? Activity

    fun loadPage(reset: Boolean = false) {
        activeRequest?.cancel()
        activeRequest = coroutineScope.launch {
            if (reset) {
                isLoading = true
                isLoadingMore = false
                page = 1
                error = null
                requiresSignIn = false
            } else {
                if (!hasMore || isLoadingMore) return@launch
                isLoadingMore = true
            }
            try {
                val pageToLoad = if (reset) 1 else page
                val result = fetchNotifications(pageToLoad)
                items = if (reset) result.items else items + result.items
                hasMore = result.nextPage != null
                page = result.nextPage ?: (pageToLoad + 1)
                error = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthRequiredException) {
                items = emptyList()
                requiresSignIn = true
            } catch (e: Exception) {
                if (reset) {
                    items = emptyList()
                    error = e.message ?: "Unable to load notifications right now."
                } else {
                    error = e.message ?: "Unable to load more notifications."
                }
            } finally {
                isLoading = false
                isLoadingMore = false
            }
        }
    }

    LaunchedEffect(Unit) { loadPage(reset = true) }

    val lastVisibleIndex by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 } }
    LaunchedEffect(lastVisibleIndex) {
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
            title = "Notifications",
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavigationClick = { navController.popBackStack() },
        )

        if (items.any { !it.isRead }) {
            OneTownCityButton(
                text = "Mark all read",
                onClick = {
                    coroutineScope.launch {
                        try {
                            markAllNotificationsRead()
                            items = items.map { it.copy(isRead = true) }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) { }
                    }
                },
                variant = OneTownCityButtonVariant.Outlined,
            )
        }

        when {
            requiresSignIn -> SignInRequiredState(message = "Sign in to see your notifications.", navController = navController)
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    OneTownCityCircularLoading(label = "Loading notifications")
                }
            }
            error != null -> {
                OneTownCityErrorState(
                    title = "Unable to load notifications",
                    message = error ?: "Please try again later.",
                    actionText = "Retry",
                    onRetry = { loadPage(reset = true) },
                )
            }
            items.isEmpty() -> {
                OneTownCityEmptyState(
                    title = "No notifications yet",
                    message = "You're all caught up.",
                    icon = Icons.Filled.Check,
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
                        NotificationCard(
                            item = item,
                            onClick = {
                                if (!item.isRead) {
                                    coroutineScope.launch {
                                        try {
                                            markNotificationRead(item.id)
                                            items = items.map { existing -> if (existing.id == item.id) existing.copy(isRead = true) else existing }
                                        } catch (e: CancellationException) {
                                            throw e
                                        } catch (_: Exception) { }
                                    }
                                }
                                if (item.url.isNotBlank()) {
                                    safeWebUri(API_BASE_URL + item.url)?.let { uri ->
                                        try {
                                            activity?.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        } catch (_: ActivityNotFoundException) { }
                                    }
                                }
                            },
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
private fun NotificationCard(
    item: NotificationItem,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (item.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (!item.isRead) {
                Text(text = "New", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Text(text = item.message, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

internal fun NavController.navigateToTab(tab: AppTab) {
    navigate(tab.route) {
        val current = graph.findStartDestination().id
        popUpTo(current) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
