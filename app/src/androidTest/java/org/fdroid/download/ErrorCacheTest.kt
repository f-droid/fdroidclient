package org.fdroid.download

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.fdroid.settings.SettingsManager
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ErrorCacheTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val settings = SettingsManager(context)

  @Test
  fun initTest() {
    settings.errorCache =
      "mirror.zero.org,0\n" +
        "mirror.one.org,1\n" +
        "mirror.two.org,2\n" +
        "mirror.three.org,3\n" +
        "mirror.four.org,4\n" +
        "mirror.five.org,5\n"
    val cache = ErrorCache(settings)

    // check initial values
    assertEquals(cache.getErrorCount("mirror.one.org"), 1)
    assertEquals(cache.getErrorCount("mirror.two.org"), 2)

    // check default value for unknown keys
    assertEquals(cache.getErrorCount("mirror.foo.org"), 0)

    // check incrementing known keys
    cache.incrementErrorCount("mirror.three.org")
    cache.incrementErrorCount("mirror.three.org")
    assertEquals(cache.getErrorCount("mirror.three.org"), 5)

    // check incrementing unknown keys
    cache.incrementErrorCount("mirror.foo.org")
    cache.incrementErrorCount("mirror.foo.org")
    assertEquals(cache.getErrorCount("mirror.foo.org"), 2)
  }

  @Test
  fun badInitTest() {
    settings.errorCache =
      "mirror.one.org,1\n" + "mirror.long.org,1,2\n" + "mirror.short.org\n" + "\n"
    val cache = ErrorCache(settings)

    // the cache should ignore entries that don't contain a standard key/value pair
    assertEquals(cache.getErrorCount("mirror.long.org"), 0)
    assertEquals(cache.getErrorCount("mirror.short.org"), 0)

    // the cache should still contain any valid key/value pairs
    assertEquals(cache.getErrorCount("mirror.one.org"), 1)
  }

  @Test
  fun exceptionInitTest() {
    settings.errorCache = "mirror.one.org,1\n" + "mirror.bad.org,foo\n" + "mirror.empty.org,\n"
    val cache = ErrorCache(settings)

    // unparseable entries that cause an exception should result in an empty initial cache
    assertEquals(cache.getErrorCount("mirror.bad.org"), 0)
    assertEquals(cache.getErrorCount("mirror.empty.org"), 0)

    // if the initial cache is empty, it won't contain any of the valid key/value pairs
    assertEquals(cache.getErrorCount("mirror.one.org"), 0)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun writeTest() {
    runTest {
      settings.errorCache = ""
      val cache = ErrorCache(settings, backgroundScope)

      // increment error count for a new key, should cause expected write
      cache.incrementErrorCount("mirror.foo.org")
      cache.incrementErrorCount("mirror.foo.org")
      cache.incrementErrorCount("mirror.bar.org")

      // wait for delayed write
      advanceTimeBy(1001.milliseconds)

      // confirm local cache updated
      assertEquals(cache.getErrorCount("mirror.foo.org"), 2)
      assertEquals(cache.getErrorCount("mirror.bar.org"), 1)
      // confirm the string version of the cache was formatted as expected
      assertEquals(settings.errorCache, "mirror.foo.org,2\nmirror.bar.org,1")
    }
  }
}
