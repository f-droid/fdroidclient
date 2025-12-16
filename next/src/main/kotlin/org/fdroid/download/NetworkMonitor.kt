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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ConnectivityManager.NetworkCallback() {

    private val connectivityManager =
        context.getSystemService(ConnectivityManager::class.java) as ConnectivityManager
    private val _networkState = MutableStateFlow(
        connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.let {
            NetworkState(it)
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
        _networkState.update { NetworkState(networkCapabilities) }
    }

    override fun onLost(network: Network) {
        _networkState.update { NetworkState(isOnline = false, isMetered = false) }
    }
}

data class NetworkState(
    val isOnline: Boolean,
    val isMetered: Boolean,
) {
    constructor(networkCapabilities: NetworkCapabilities) : this(
        isOnline = networkCapabilities.hasCapability(NET_CAPABILITY_INTERNET),
        isMetered = !networkCapabilities.hasCapability(NET_CAPABILITY_NOT_METERED),
    )
}
