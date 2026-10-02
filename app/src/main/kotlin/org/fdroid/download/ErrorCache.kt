package org.fdroid.download

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.fdroid.settings.SettingsManager
import org.fdroid.utils.IoDispatcher

@OptIn(ExperimentalAtomicApi::class)
@Singleton
class ErrorCache
@Inject
constructor(
  private val settingsManager: SettingsManager,
  @param:IoDispatcher private val ioScope: CoroutineScope = MainScope(),
) {

  private val log = KotlinLogging.logger {}

  private val cache: ConcurrentHashMap<String, Int> by lazy {
    stringToIntMap(settingsManager.errorCache)
  }
  private var writeScheduled: AtomicBoolean = AtomicBoolean(false)

  fun incrementErrorCount(hostname: String) {
    cache[hostname] = (cache[hostname] ?: 0) + 1
    cacheWrite()
  }

  fun getErrorCount(hostname: String): Int {
    return cache[hostname] ?: 0
  }

  private fun cacheWrite() {
    if (writeScheduled.compareAndSet(expectedValue = false, newValue = true)) {
      ioScope.launch {
        delay(1.seconds)
        settingsManager.errorCache = intMapToString(cache)
        writeScheduled.store(false)
      }
    }
  }

  private fun intMapToString(intMap: Map<String, Int>): String {
    try {
      val output = buildString {
        intMap.forEach { (key, num) ->
          if (!key.isEmpty()) {
            if (isNotEmpty()) {
              append("\n")
            }
            append(key)
            append(",")
            append(num)
          } else {
            // if no key, no-op
          }
        }
      }
      return output
    } catch (e: Exception) {
      log.error(e) { "Error converting int map to string, returning empty string: " }
      return ""
    }
  }

  private fun stringToIntMap(string: String): ConcurrentHashMap<String, Int> {
    try {
      val output = ConcurrentHashMap<String, Int>()
      for (line in string.split("\n")) {
        val pair = line.split(",")
        if (pair.size == 2) {
          val key = pair[0]
          val num = pair[1].toInt()
          output[key] = num
        } else {
          // if no key/val pair, no-op
        }
      }
      return output
    } catch (e: Exception) {
      log.error(e) { "Error converting string to int map, returning empty map: " }
      return ConcurrentHashMap<String, Int>()
    }
  }
}
