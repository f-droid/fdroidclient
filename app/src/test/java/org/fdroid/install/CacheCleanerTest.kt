package org.fdroid.install

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

internal class CacheCleanerTest {

  @get:Rule var tmpFolder: TemporaryFolder = TemporaryFolder()

  private val context: Context = mockk(relaxed = true)
  private lateinit var cacheDir: File
  private lateinit var cacheCleaner: CacheCleaner

  private val now = TimeUnit.DAYS.toMillis(5)

  @Before
  fun setUp() {
    cacheDir = tmpFolder.newFolder("cache")
    every { context.cacheDir } returns cacheDir
    cacheCleaner = CacheCleaner(context)
  }

  @Test
  fun `clean deletes only old sha256 hash files`() {
    val oldHash = newFile(name = "a".repeat(64), lastModified = now - DELETE_OLDER_THAN_MILLIS)
    val newHash = newFile(name = "b".repeat(64), lastModified = now - DELETE_OLDER_THAN_MILLIS + 1)
    val oldNonHash = newFile(name = "not-a-hash.apk", lastModified = now - DELETE_OLDER_THAN_MILLIS)
    val oldHashDir =
      File(cacheDir, "c".repeat(64)).apply {
        mkdirs()
        setLastModified(now - DELETE_OLDER_THAN_MILLIS)
      }

    cacheCleaner.clean(now)

    assertFalse(oldHash.exists())
    assertTrue(newHash.exists())
    assertTrue(oldNonHash.exists())
    assertTrue(oldHashDir.exists())
  }

  @Test
  fun `clean does not throw when cacheDir listFiles returns null`() {
    val nullListingCacheDir: File = mockk()
    val customContext: Context = mockk(relaxed = true)
    every { customContext.cacheDir } returns nullListingCacheDir
    every { nullListingCacheDir.listFiles() } returns null

    val cleaner = CacheCleaner(customContext)

    cleaner.clean()
  }

  private fun newFile(name: String, lastModified: Long): File {
    return File(cacheDir, name).apply {
      writeText("x")
      setLastModified(lastModified)
    }
  }
}
