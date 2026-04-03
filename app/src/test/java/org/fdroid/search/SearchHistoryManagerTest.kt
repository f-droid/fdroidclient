package org.fdroid.search

import android.content.Context
import android.content.Context.MODE_PRIVATE
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.FileNotFoundException
import java.io.FileOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

internal class SearchHistoryManagerTest {

  @get:Rule val tempFolder = TemporaryFolder()

  private val context: Context = mockk()
  private val manager = SearchHistoryManager(context)

  @Test
  fun testGetSavedSearchesSortedByNewest() {
    val file = tempFolder.newFile()
    val unsorted =
      listOf(
        SavedSearch(time = 10L, query = "a"),
        SavedSearch(time = 30L, query = "c"),
        SavedSearch(time = 20L, query = "b"),
      )
    file.writeText(Json.encodeToString(unsorted))
    every { context.openFileInput(any()) } answers { file.inputStream() }

    val saved = manager.getSavedSearches()

    assertEquals(listOf(30L, 20L, 10L), saved.map { it.time })
    assertEquals(listOf("c", "b", "a"), saved.map { it.query })
  }

  @Test
  fun testSaveGetAndClear() {
    val file = tempFolder.newFile()
    file.writeText("[]")
    every { context.openFileInput(any()) } answers { file.inputStream() }
    every { context.openFileOutput(any(), MODE_PRIVATE) } answers { FileOutputStream(file, false) }
    every { context.deleteFile(any()) } returns true

    val savedFromSave = manager.saveSearchQuery("foo")
    val savedFromDisk = manager.getSavedSearches()

    assertEquals(1, savedFromSave.size)
    assertEquals("foo", savedFromSave.first().query)
    assertEquals(1, savedFromDisk.size)
    assertEquals("foo", savedFromDisk.first().query)

    assertTrue(manager.clearAll())
    verify { context.deleteFile(any()) }
  }

  @Test
  fun testSaveSearchQueryDeduplicatesExistingQuery() {
    val file = tempFolder.newFile()
    val existing =
      listOf(SavedSearch(time = 200L, query = "foo"), SavedSearch(time = 100L, query = "bar"))
    file.writeText(Json.encodeToString(existing))
    every { context.openFileInput(any()) } answers { file.inputStream() }
    every { context.openFileOutput(any(), MODE_PRIVATE) } answers { FileOutputStream(file, false) }

    val saved = manager.saveSearchQuery("foo")

    assertEquals(2, saved.size)
    assertEquals(1, saved.count { it.query == "foo" })
    assertEquals(1, saved.count { it.query == "bar" })
  }

  @Test
  fun testSaveSearchQueryCapsSavedEntriesAtMax() {
    val maxSearches = 3
    val cappedManager = SearchHistoryManager(context, maxSearches)
    val file = tempFolder.newFile()
    val existing =
      listOf(
        SavedSearch(time = 400L, query = "q4"),
        SavedSearch(time = 300L, query = "q3"),
        SavedSearch(time = 200L, query = "q2"),
        SavedSearch(time = 100L, query = "q1"),
      )
    file.writeText(Json.encodeToString(existing))
    every { context.openFileInput(any()) } answers { file.inputStream() }
    every { context.openFileOutput(any(), MODE_PRIVATE) } answers { FileOutputStream(file, false) }

    val savedFromSave = cappedManager.saveSearchQuery("new-query")
    val savedFromDisk = cappedManager.getSavedSearches()

    assertEquals(maxSearches, savedFromSave.size)
    assertEquals(maxSearches, savedFromDisk.size)
    assertTrue(savedFromSave.any { it.query == "new-query" })
    assertTrue(savedFromDisk.any { it.query == "new-query" })
  }

  @Test
  fun testGetSavedSearchesReturnsEmptyForMissingFile() {
    every { context.openFileInput(any()) } throws FileNotFoundException()

    val saved = manager.getSavedSearches()

    assertTrue(saved.isEmpty())
    verify(exactly = 0) { context.deleteFile(any()) }
  }

  @Test
  fun testGetSavedSearchesCorruptFileClearsAndReturnsEmpty() {
    val file = tempFolder.newFile()
    file.writeText("not valid json")
    every { context.openFileInput(any()) } answers { file.inputStream() }
    every { context.deleteFile(any()) } returns true

    val saved = manager.getSavedSearches()

    assertTrue(saved.isEmpty())
    verify(exactly = 1) { context.deleteFile(any()) }
  }

  @Test
  fun testClearAllReturnsFalseWhenDeleteFails() {
    every { context.deleteFile(any()) } returns false

    assertEquals(false, manager.clearAll())
    verify(exactly = 1) { context.deleteFile(any()) }
  }
}
