package org.fdroid.install

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_PACKAGE_ADDED
import android.content.Intent.ACTION_PACKAGE_REMOVED
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_SIGNATURES
import android.net.Uri
import androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
import androidx.core.content.ContextCompat.registerReceiver
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@Suppress("DEPRECATION")
@OptIn(ExperimentalCoroutinesApi::class)
internal class InstalledAppsCacheTest {

  private val context: Context = mockk(relaxed = true)
  private val packageManager: PackageManager = mockk()
  private val ioScope = CoroutineScope(Dispatchers.Unconfined)

  @Before
  fun setUp() {
    mockkStatic("androidx.core.content.ContextCompat")
    mockkConstructor(IntentFilter::class)
    every { anyConstructed<IntentFilter>().addAction(any()) } returns mockk()
    every { anyConstructed<IntentFilter>().addDataScheme(any()) } returns mockk()

    every { context.packageManager } returns packageManager
    every { registerReceiver(any(), any(), any<IntentFilter>(), RECEIVER_NOT_EXPORTED) } returns
      null
  }

  @Test
  fun `installed apps load initially and isInstalled reflects cache`() = runBlocking {
    val app1 = packageInfo("org.example.a")
    val app2 = packageInfo("org.example.b")
    every { packageManager.getInstalledPackages(GET_SIGNATURES) } returns listOf(app1, app2)

    val cache = InstalledAppsCache(context, ioScope)

    assertEquals(2, cache.installedApps.value?.size)
    assertTrue(cache.isInstalled("org.example.a"))
    assertTrue(cache.isInstalled("org.example.b"))
    assertFalse(cache.isInstalled("org.example.missing"))
    verify(exactly = 1) { registerReceiver(context, cache, any(), RECEIVER_NOT_EXPORTED) }
    verify(exactly = 1) { packageManager.getInstalledPackages(GET_SIGNATURES) }
  }

  @Test
  fun `onReceive add and remove intents update installedApps and isInstalled`() = runBlocking {
    every { packageManager.getInstalledPackages(GET_SIGNATURES) } returns emptyList()

    val cache = InstalledAppsCache(context, ioScope)

    val added = packageInfo("org.example.new")
    every { packageManager.getPackageInfo("org.example.new", GET_SIGNATURES) } returns added

    // app gets added
    cache.onReceive(context, packageChangedIntent(ACTION_PACKAGE_ADDED, "org.example.new"))
    assertTrue(cache.isInstalled("org.example.new"))
    assertEquals(1, cache.installedApps.value?.size)

    // app gets removed
    cache.onReceive(context, packageChangedIntent(ACTION_PACKAGE_REMOVED, "org.example.new"))
    assertFalse(cache.isInstalled("org.example.new"))
    assertEquals(0, cache.installedApps.value?.size)
  }

  @Test
  fun `intents with replacing true are ignored`() = runBlocking {
    val app = packageInfo("org.example.replace")
    every { packageManager.getInstalledPackages(GET_SIGNATURES) } returns listOf(app)

    val cache = InstalledAppsCache(context, ioScope)

    cache.onReceive(
      context,
      packageChangedIntent(
        action = ACTION_PACKAGE_REMOVED,
        packageName = "org.example.replace",
        replacing = true,
      ),
    )

    assertTrue(cache.isInstalled("org.example.replace"))
    assertEquals(1, cache.installedApps.value?.size)
  }

  @Test
  fun `onReceive add intent ignores late broadcast when app is already gone`() = runBlocking {
    val existing = packageInfo("org.example.existing")
    every { packageManager.getInstalledPackages(GET_SIGNATURES) } returns listOf(existing)
    every { packageManager.getPackageInfo("org.example.gone", GET_SIGNATURES) } throws
      PackageManager.NameNotFoundException("gone")

    val cache = InstalledAppsCache(context, ioScope)

    cache.onReceive(context, packageChangedIntent(ACTION_PACKAGE_ADDED, "org.example.gone"))

    assertEquals(1, cache.installedApps.value?.size)
    assertTrue(cache.isInstalled("org.example.existing"))
    assertFalse(cache.isInstalled("org.example.gone"))
  }

  @Test
  fun `cache is null until installed apps have loaded`() = runTest {
    val app = packageInfo("org.example.installed")
    every { packageManager.getInstalledPackages(GET_SIGNATURES) } returns listOf(app)
    every { packageManager.getPackageInfo(app.packageName, GET_SIGNATURES) } returns app
    every { packageManager.getPackageInfo("org.example.missing", GET_SIGNATURES) } throws
      PackageManager.NameNotFoundException("missing")

    val cache = InstalledAppsCache(context, backgroundScope)

    cache.installedApps.test {
      // initial flow emission is null
      assertNull(awaitItem())
      // installed check still works, because it falls back to PackageManager when the cache is null
      assertTrue(cache.isInstalled(app.packageName))
      assertFalse(cache.isInstalled("org.example.missing"))
      runCurrent()
      // after things have loaded, we get our cache map
      assertEquals(mapOf(app.packageName to app), awaitItem())
      expectNoEvents()
    }
  }

  @Test
  fun `an empty load publishes an empty cache`() = runTest {
    every { packageManager.getInstalledPackages(GET_SIGNATURES) } returns emptyList()
    val cache = InstalledAppsCache(context, backgroundScope)

    assertNull(cache.installedApps.value)
    runCurrent()
    assertEquals(emptyMap(), cache.installedApps.value)
  }

  @Test
  fun `broadcasts during initial load are applied to the completed cache`() = runTest {
    val removed = packageInfo("org.example.removed")
    val retained = packageInfo("org.example.retained")
    val added = packageInfo("org.example.added")
    every { packageManager.getPackageInfo(added.packageName, GET_SIGNATURES) } returns added
    lateinit var cache: InstalledAppsCache
    every { packageManager.getInstalledPackages(GET_SIGNATURES) } answers
      {
        cache.onReceive(context, packageChangedIntent(ACTION_PACKAGE_REMOVED, removed.packageName))
        cache.onReceive(context, packageChangedIntent(ACTION_PACKAGE_ADDED, added.packageName))
        assertNull(cache.installedApps.value)
        listOf(removed, retained)
      }
    cache = InstalledAppsCache(context, backgroundScope)

    assertNull(cache.installedApps.value)
    runCurrent()
    assertEquals(
      mapOf(retained.packageName to retained, added.packageName to added),
      cache.installedApps.value,
    )
  }

  private fun packageInfo(packageName: String) =
    PackageInfo().apply { this.packageName = packageName }

  private fun packageChangedIntent(
    action: String,
    packageName: String,
    replacing: Boolean = false,
  ): Intent {
    val intent: Intent = mockk()
    every { intent.`package` } returns null
    every { intent.action } returns action
    every { intent.data } returns mockk<Uri> { every { schemeSpecificPart } returns packageName }
    every { intent.getBooleanExtra(Intent.EXTRA_REPLACING, false) } returns replacing
    return intent
  }
}
