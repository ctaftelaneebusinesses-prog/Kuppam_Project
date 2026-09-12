package com.onetowncity.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers pickPreferredCity — which city is actually "the resolved city"
 * across every combination of a manually selected city, an
 * automatically-detected one, recent history, and location permission
 * state. Had zero test coverage before Phase 4 despite being the one
 * function every screen's "which city am I browsing" ultimately depends on.
 */
class CityResolutionTest {

    private val manual = CitySelection(slug = "kuppam", name = "Kuppam")
    private val detected = CitySelection(slug = "chittoor", name = "Chittoor")
    private val recent1 = CitySelection(slug = "bengaluru", name = "Bengaluru")
    private val recent2 = CitySelection(slug = "hyderabad", name = "Hyderabad")

    @Test
    fun `a manually selected city always wins, even with a detected city and permission granted`() {
        val result = pickPreferredCity(
            currentCity = detected,
            selectedCity = manual,
            recentCities = listOf(recent1),
            permissionState = LocationPermissionState.GRANTED,
        )
        assertEquals(manual, result)
    }

    @Test
    fun `with no manual selection and permission granted, the detected city wins`() {
        val result = pickPreferredCity(
            currentCity = detected,
            selectedCity = null,
            recentCities = listOf(recent1),
            permissionState = LocationPermissionState.GRANTED,
        )
        assertEquals(detected, result)
    }

    @Test
    fun `a detected city is ignored when permission is not granted, falling back to recent history`() {
        val result = pickPreferredCity(
            currentCity = detected,
            selectedCity = null,
            recentCities = listOf(recent1, recent2),
            permissionState = LocationPermissionState.DENIED,
        )
        assertEquals(recent1, result)
    }

    @Test
    fun `permanently denied permission also falls back to recent history, not the stale detected city`() {
        val result = pickPreferredCity(
            currentCity = detected,
            selectedCity = null,
            recentCities = listOf(recent1),
            permissionState = LocationPermissionState.REVOKED,
        )
        assertEquals(recent1, result)
    }

    @Test
    fun `location unavailable on the device falls back to recent history too`() {
        val result = pickPreferredCity(
            currentCity = null,
            selectedCity = null,
            recentCities = listOf(recent1),
            permissionState = LocationPermissionState.UNAVAILABLE,
        )
        assertEquals(recent1, result)
    }

    @Test
    fun `no recent history but a detected city without permission falls back to the detected city anyway`() {
        // pickPreferredCity's last resort is `recentCities.firstOrNull() ?: currentCity` —
        // a detected-but-unconfirmed city is still better than showing nothing.
        val result = pickPreferredCity(
            currentCity = detected,
            selectedCity = null,
            recentCities = emptyList(),
            permissionState = LocationPermissionState.DENIED,
        )
        assertEquals(detected, result)
    }

    @Test
    fun `nothing selected, nothing detected, no history yet resolves to no city`() {
        val result = pickPreferredCity(
            currentCity = null,
            selectedCity = null,
            recentCities = emptyList(),
            permissionState = LocationPermissionState.DENIED,
        )
        assertNull(result)
    }

    @Test
    fun `switching cities is just picking a new selectedCity, which immediately wins`() {
        val beforeSwitch = pickPreferredCity(detected, manual, listOf(recent1), LocationPermissionState.GRANTED)
        val afterSwitch = pickPreferredCity(detected, recent2, listOf(recent1, manual), LocationPermissionState.GRANTED)

        assertEquals(manual, beforeSwitch)
        assertEquals(recent2, afterSwitch)
    }
}
