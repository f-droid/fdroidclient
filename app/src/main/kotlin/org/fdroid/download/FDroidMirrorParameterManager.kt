package org.fdroid.download

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import org.fdroid.settings.SettingsConstants
import org.fdroid.settings.SettingsManager
import org.fdroid.utils.getCurrentLocation

@OptIn(ExperimentalAtomicApi::class)
@Singleton
class FDroidMirrorParameterManager
@Inject
constructor(
  @param:ApplicationContext private val context: Context,
  private val settingsManager: SettingsManager,
  private val dnsWithCache: DnsWithCache,
  private val errorCache: ErrorCache,
) : MirrorParameterManager {

  override fun cacheMirrorIpAddresses(
    hostname: String,
    ipv4Addresses: List<String>,
    ipv6Addresses: List<String>,
  ) {
    dnsWithCache.populateCacheWithStrings(hostname, ipv4Addresses, ipv6Addresses)
  }

  override fun shouldRetryRequest(hostname: String): Boolean {
    return dnsWithCache.shouldRetryRequest(hostname)
  }

  override fun incrementMirrorErrorCount(hostname: String) {
    errorCache.incrementErrorCount(hostname)
  }

  override fun getMirrorErrorCount(hostname: String): Int {
    return errorCache.getErrorCount(hostname)
  }

  override fun preferForeignMirrors(): Boolean {
    return settingsManager.mirrorChooser == SettingsConstants.MirrorChooserValues.PreferForeign
  }

  override fun getCurrentLocation(): String = getCurrentLocation(context)
}
