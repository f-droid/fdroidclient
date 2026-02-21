package org.fdroid.download

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.fdroid.settings.SettingsManager
import java.net.InetAddress
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
@Singleton
class DnsCache @Inject constructor(
    private val settingsManager: SettingsManager
) {

    private val log = KotlinLogging.logger {}

    private var cache: MutableMap<String, List<InetAddress>>
    private var writeScheduled: AtomicBoolean = AtomicBoolean(false)

    init {
        cache = stringToIpMap(settingsManager.dnsCache).toMutableMap()
    }

    fun insert(hostname: String, ipList: List<InetAddress>) {
        cache[hostname] = ipList
        cacheWrite()
    }

    fun remove(hostname: String) {
        val ipList = cache.remove(hostname)
        if (ipList != null) {
            cacheWrite()
        }
    }

    fun lookup(hostname: String): List<InetAddress>? {
        return if (settingsManager.useDnsCache) {
            cache[hostname]
        } else {
            null
        }
    }

    private fun cacheWrite() {
        if (writeScheduled.compareAndSet(expectedValue = false, newValue = true)) {
            MainScope().launch {
                delay(1000L)
                settingsManager.dnsCache = ipMapToString(cache)
                writeScheduled.store(false)
            }
        }
    }

    private fun ipMapToString(ipMap: Map<String, List<InetAddress>>): String {
        try {
            var output = ""
            ipMap.forEach { (key, addresses) ->
                if (!output.isEmpty()) {
                    output += "\n"
                }
                output += key
                for (item in addresses) {
                    if (!output.isEmpty()) {
                        output += " "
                    }
                    output += item.hostAddress
                }
            }
            return output
        } catch (e: Exception) {
            log.error(e) { "Error converting IP map to string, returning empty string: " }
            return ""
        }
    }

    private fun stringToIpMap(string: String): Map<String, List<InetAddress>> {
        try {
            val output = mutableMapOf<String, List<InetAddress>>()
            for (line in string.split("\n")) {
                val items = line.split(" ").toMutableList()
                val key = items.removeAt(0)
                val ipList = mutableListOf<InetAddress>()
                for (ip in items) {
                    try {
                        ipList.add(InetAddress.getByName(ip))
                    } catch (e: UnknownHostException) {
                        // should not occur, if an ip address is supplied only the format is checked
                        log.error(e) { "Error parsing IP address, moving on to next item: " }
                    }
                }
                output[key] = ipList
            }
            return output
        } catch (e: Exception) {
            log.error(e) { "Error converting string to IP map, returning empty map: " }
            return emptyMap()
        }
    }
}
