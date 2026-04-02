package org.fdroid.download

import java.net.InetAddress
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import mu.KotlinLogging
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
    if (ipList.isNullOrEmpty()) {
      ipList = Dns.SYSTEM.lookup(hostname)
      cache.insert(hostname, ipList)
    }
    return ipList
  }

  /**
   * these methods provide a way to pre-load the cache with values from other sources such as
   * the mirror info from the index, further reducing the needs to do DNS lookups
   */
  fun populateCacheWithStrings(hostname: String, ipv4List: List<String>, ipv6List: List<String>) {
    val ipv4ConvertedList = stringListToIpList(ipv4List)
    val ipv6ConvertedList = stringListToIpList(ipv6List)
    populateCacheWithIps(hostname, ipv4ConvertedList, ipv6ConvertedList)
  }

  fun populateCacheWithIps(hostname: String, ipv4List: List<InetAddress>, ipv6List: List<InetAddress>) {
    // at this time, the DNS cache only supports a single collection because that's what the DNS
    // lookup method returns. since these values are being inserted manually, it's possible that
    // the cache might end up with values that wouldn't have been returned by the lookup method.
    // if that becomes an issue, logic could be added here to filter the values that are inserted.
    val mergedList = mutableListOf<InetAddress>()
    mergedList.addAll(ipv4List)
    mergedList.addAll(ipv6List)
    if (!cache.keys().contains(hostname) && !mergedList.isEmpty()) {
      cache.insert(hostname, mergedList)
    }
  }

  private fun stringListToIpList(ipList: List<String>): List<InetAddress> {
    val log = KotlinLogging.logger {}
    try {
      return ipList.mapNotNull {
        try {
          InetAddress.getByName(it)
        } catch (e: UnknownHostException) {
          log.warn { "Unexpected format for IP address: $it" }
          null
        }
      }
    } catch (e: Exception) {
      log.warn { "Failed to parse list of IP addresses, returning empty list" }
      return emptyList()
    }
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
