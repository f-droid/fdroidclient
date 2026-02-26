package org.fdroid.download

import java.net.InetAddress
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Dns
import org.fdroid.settings.SettingsManager

@Singleton
class DnsWithCache
@Inject
constructor(private val settingsManager: SettingsManager, private val cache: DnsCache) : Dns {

  override fun lookup(hostname: String): List<InetAddress> {
    if (!settingsManager.useDnsCache) {
      return Dns.SYSTEM.lookup(hostname)
    }
    var ipList = cache.lookup(hostname)
    if (ipList == null) {
      ipList = Dns.SYSTEM.lookup(hostname)
      cache.insert(hostname, ipList)
    }
    return ipList
  }

  /**
   * in case a host is unreachable, check whether the cached dns result is different from the
   * current result. if the cached result is different, remove that result from the cache. returns
   * true if a cached result was removed, indicating that the connection should be retried,
   * otherwise returns false.
   */
  fun shouldRetryRequest(hostname: String): Boolean {
    if (!settingsManager.useDnsCache) {
      // the cache feature was not enabled, so a cached result didn't cause the failure
      return false
    }
    // if no cached result was found, a cached result didn't cause the failure
    val ipList = cache.lookup(hostname) ?: return false
    try {
      val dnsList = Dns.SYSTEM.lookup(hostname)
      for (address in dnsList) {
        if (!ipList.contains(address)) {
          // the cached result doesn't match the current dns result,
          // so the connection should be retried
          cache.remove(hostname)
          return true
        }
      }
      // the cached result matches the current dns result,
      // so a cached result didn't cause the failure
      return false
    } catch (_: UnknownHostException) {
      // the url returned an unknown host exception, so there's no point in retrying
      return false
    }
  }
}
