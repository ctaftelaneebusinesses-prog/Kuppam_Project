package com.onetowncity.app.cache

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-wide connectivity source of truth. [isOnlineNow] is what OfflineCache
 * consults synchronously on every request to decide network-first vs
 * cache-first; [isOnline] is the reactive StateFlow the UI observes so it
 * can recover the moment connectivity returns (offline-first spec's
 * "the UI must recover when connectivity returns").
 *
 * Registers a single ConnectivityManager.NetworkCallback for the process
 * lifetime rather than polling — init() is idempotent, called once from
 * MainActivity.onCreate alongside SessionManager.init/OfflineCache.init.
 */
internal object NetworkMonitor {
    private var initialized = false
    private lateinit var connectivityManager: ConnectivityManager

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = hasInternet()
        }

        override fun onLost(network: Network) {
            _isOnline.value = hasInternet()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            _isOnline.value = hasInternet()
        }
    }

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        connectivityManager = context.applicationContext.getSystemService(ConnectivityManager::class.java)
        _isOnline.value = hasInternet()
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        runCatching { connectivityManager.registerNetworkCallback(request, callback) }
    }

    /** Synchronous, best-effort check — used by OfflineCache to decide network-first vs cache-first without suspending. */
    fun isOnlineNow(): Boolean = if (initialized) hasInternet() else false

    private fun hasInternet(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
