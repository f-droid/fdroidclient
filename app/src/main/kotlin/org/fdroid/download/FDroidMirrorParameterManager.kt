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
) : MirrorParameterManager {

  override fun cacheMirrorIpAddresses(
    mirrorUrl: String,
    ipv4Addresses: List<String>,
    ipv6Addresses: List<String>,
  ) {
    dnsWithCache.populateCacheWithStrings(mirrorUrl, ipv4Addresses, ipv6Addresses)
  }

  override fun shouldRetryRequest(mirrorUrl: String): Boolean {
    return dnsWithCache.shouldRetryRequest(mirrorUrl)
  }

  // TODO overhaul default MirrorChooser
  override fun incrementMirrorErrorCount(mirrorUrl: String) {}

  override fun getMirrorErrorCount(mirrorUrl: String): Int = 0

  override fun preferForeignMirrors(): Boolean {
    return settingsManager.mirrorChooser == SettingsConstants.MirrorChooserValues.PreferForeign
  }

  override fun getCurrentLocation(): String = getCurrentLocation(context)
}
