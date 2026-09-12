package com.onetowncity.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The app-wide "which city am I browsing" broadcast. Every browse/feature
 * screen (Business, Property, Project, Search, Students' sub-screens, etc.)
 * reads [current] once at launch to seed its own city filter from the
 * globally selected city instead of a blank default disconnected from what
 * the user actually chose in the city selector (OneTownCityAppShell's
 * "Choose your city" sheet).
 *
 * OneTownCityAppShell is the only writer (its LaunchedEffect(resolvedCity)).
 * Persistence and detection stay exactly where they already are
 * (readSavedCity/readRecentCities/pickPreferredCity in MainActivity.kt) —
 * this is only the live, in-memory broadcast of that already-resolved city,
 * mirroring how auth/SessionManager.authState broadcasts sign-in state.
 */
internal object AppCityState {
    private val _current = MutableStateFlow<CitySelection?>(null)
    val current: StateFlow<CitySelection?> = _current.asStateFlow()

    fun update(city: CitySelection?) {
        _current.value = city
    }
}
