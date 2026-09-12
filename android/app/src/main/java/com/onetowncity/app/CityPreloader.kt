package com.onetowncity.app

import com.onetowncity.app.cache.NetworkMonitor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Fires a bounded, best-effort set of page-1 fetches for a newly selected
 * city while online, so switching to a city and then going offline still
 * has real content to show ("CITY PRELOADING" in the offline-first spec).
 * Each fetcher's own httpJson/fetchListPage call is what actually writes to
 * the offline cache (see MainActivity.httpJsonWithFreshness) — this only
 * decides which page-1 requests are worth firing and bounds how much/how
 * often: one page per content type, once per city per app process (not on
 * every recomposition), matching "do not download the entire backend
 * database" and "avoid ... duplicate requests."
 *
 * Deliberately excludes Marketplace: core/models.py has no backend model
 * for it yet (see marketplaceCache's own comment in MainActivity.kt) —
 * there is nothing real to preload, and caching an always-empty response
 * would itself be exactly the "fake offline mode" this phase rules out.
 */
internal object CityPreloader {
    private val preloadedCities = mutableSetOf<String>()

    suspend fun preloadIfNeeded(citySlug: String?) {
        if (citySlug.isNullOrBlank()) return
        if (!NetworkMonitor.isOnlineNow()) return
        if (!preloadedCities.add(citySlug)) return

        val tasks: List<suspend () -> Unit> = listOf(
            { fetchCategories() },
            { fetchBusinesses(query = "", categoryKey = "", citySlug = citySlug, page = 1) },
            { fetchProperties(query = "", categoryKey = "", citySlug = citySlug, page = 1) },
            { fetchProjects(query = "", categoryKey = "", citySlug = citySlug, page = 1) },
            { fetchEvents(query = "", citySlug = citySlug, page = 1) },
            { fetchScholarships(query = "", citySlug = citySlug, page = 1) },
            { fetchLostFound(query = "", citySlug = citySlug, page = 1) },
            { fetchPlacesToVisit(query = "", citySlug = citySlug, page = 1) },
            { fetchStudentServices(query = "", citySlug = citySlug, page = 1) },
            { fetchTuitionCenters(query = "", citySlug = citySlug, page = 1) },
        )

        coroutineScope {
            tasks.forEach { task ->
                launch {
                    try {
                        task()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // Best-effort: one category failing to preload (e.g.
                        // connectivity drops mid-batch) must never block the
                        // others or surface an error to the user — preloading
                        // is a background optimization, not a user-facing
                        // operation.
                    }
                }
            }
        }
    }
}
