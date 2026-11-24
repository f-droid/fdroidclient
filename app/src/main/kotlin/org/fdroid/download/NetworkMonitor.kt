package org.fdroid.download

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.fdroid.settings.SettingsManager
import org.fdroid.utils.IoDispatcher

@Singleton
class NetworkMonitor
@Inject
constructor(
  @param:ApplicationContext private val context: Context,
  private val settingsManager: SettingsManager,
  @param:IoDispatcher private val coroutineScope: CoroutineScope,
) : ConnectivityManager.NetworkCallback() {

  private val connectivityManager =
    context.getSystemService(ConnectivityManager::class.java) as ConnectivityManager
  private val warnWhenMetered
    get() = settingsManager.warnWhenMeteredFlow.value

  private val _networkState =
    MutableStateFlow(
      connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.let {
        NetworkState(it, warnWhenMetered)
      } ?: NetworkState(isOnline = false, isMetered = false, warnWhenMetered = warnWhenMetered),
    )
  val networkState = _networkState.asStateFlow()

  init {
    /**
     * We are not using [ConnectivityManager.getActiveNetwork] or
     * [ConnectivityManager.isActiveNetworkMetered], because often the active network is null. What
     * we are doing instead is simpler and seems to work better.
     */
    connectivityManager.registerDefaultNetworkCallback(this)
    // observe the setting for whether to warn when on a metered network
    coroutineScope.launch {
      // drop first emission as that is the initial value, and we only want to react to changes
      settingsManager.warnWhenMeteredFlow.drop(1).collect { warnWhenMetered ->
        if (!warnWhenMetered) {
          _networkState.update { it.copy(warnWhenMetered = false) }
        }
      }
    }
  }

  override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
    _networkState.update { NetworkState(networkCapabilities, warnWhenMetered) }
  }

  override fun onLost(network: Network) {
    _networkState.update {
      NetworkState(isOnline = false, isMetered = false, warnWhenMetered = warnWhenMetered)
    }
  }
}

data class NetworkState(
  val isOnline: Boolean,
  private val isMetered: Boolean,
  val warnWhenMetered: Boolean = true,
) {
  val showWarningDialog: Boolean = isMetered && warnWhenMetered

  constructor(
    networkCapabilities: NetworkCapabilities,
    warnWhenMetered: Boolean,
  ) : this(
    isOnline = networkCapabilities.hasCapability(NET_CAPABILITY_INTERNET),
    isMetered = !networkCapabilities.hasCapability(NET_CAPABILITY_NOT_METERED),
    warnWhenMetered = warnWhenMetered,
  )
}
