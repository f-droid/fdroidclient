package org.fdroid.download

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.fdroid.settings.SettingsConstants.AutoUpdateValues
import org.fdroid.settings.SettingsManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager,
) : ConnectivityManager.NetworkCallback() {

    private val connectivityManager =
        context.getSystemService(ConnectivityManager::class.java) as ConnectivityManager
    private val neverMetered get() = settingsManager.autoUpdateApps == AutoUpdateValues.Always
    private val _networkState = MutableStateFlow(
        connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.let {
            NetworkState(it, neverMetered)
        } ?: NetworkState(isOnline = false, isMetered = false)
    )
    val networkState = _networkState.asStateFlow()

    init {
        /**
         * We are not using [ConnectivityManager.getActiveNetwork] or
         * [ConnectivityManager.isActiveNetworkMetered], because often the active network is null.
         * What we are doing instead is simpler and seems to work better.
         */
        connectivityManager.registerDefaultNetworkCallback(this)
    }

    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        _networkState.update { NetworkState(networkCapabilities, neverMetered) }
    }

    override fun onLost(network: Network) {
        _networkState.update { NetworkState(isOnline = false, isMetered = false) }
    }
}

data class NetworkState(
    val isOnline: Boolean,
    val isMetered: Boolean,
) {
    constructor(networkCapabilities: NetworkCapabilities, neverMetered: Boolean) : this(
        isOnline = networkCapabilities.hasCapability(NET_CAPABILITY_INTERNET),
        isMetered = if (neverMetered) false
        else !networkCapabilities.hasCapability(NET_CAPABILITY_NOT_METERED),
    )
}
