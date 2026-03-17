package org.fdroid.ui.search

import android.database.sqlite.SQLiteException
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.fdroid.database.AppDao
import org.fdroid.database.AppSearchItem
import org.fdroid.database.Category
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.Repository
import org.fdroid.database.RepositoryDao
import org.fdroid.download.PackageName
import org.fdroid.index.RepoManager
import org.fdroid.install.InstalledAppsCache
import org.fdroid.settings.SettingsManager
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class SearchManagerTest {

  @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

  private val db: FDroidDatabase = mockk()
  private val appDao: AppDao = mockk()
  private val repositoryDao: RepositoryDao = mockk()
  private val repoManager: RepoManager = mockk()
  private val settingsManager: SettingsManager = mockk()
  private val installedAppsCache: InstalledAppsCache = mockk()
  private val categoriesLiveData = MutableLiveData<List<Category>>(emptyList())

  private val repo: Repository = mockk(relaxed = true)

  init {
    Dispatchers.setMain(Dispatchers.Unconfined)

    every { db.getAppDao() } returns appDao
    every { db.getRepositoryDao() } returns repositoryDao
    every { repositoryDao.getLiveCategories() } returns categoriesLiveData
    every { settingsManager.proxyConfig } returns null
    every { repoManager.getRepository(any()) } returns repo
    every { installedAppsCache.isInstalled(any()) } returns false
  }

  private val searchManager =
    SearchManager(
      db = db,
      repoManager = repoManager,
      settingsManager = settingsManager,
      installedAppsCache = installedAppsCache,
      ioDispatcher = Dispatchers.Unconfined,
    )

  @Test
  fun searchCrashMeThrows() = runTest {
    assertFailsWith<IllegalStateException> { searchManager.search("CrashMe") }
  }

  @Test
  fun searchBuildsPrefixQueryForSingleWord() = runTest {
    val querySlot = slot<String>()
    coEvery { appDao.getAppSearchItems(capture(querySlot)) } returns emptyList()

    searchManager.search("foo")

    assertEquals("foo*", querySlot.captured)
  }

  @Test
  fun searchBuildsComplexQueryForMultipleWordsAndSanitizesQuotes() = runTest {
    val querySlot = slot<String>()
    coEvery { appDao.getAppSearchItems(capture(querySlot)) } returns emptyList()

    searchManager.search("foo \"bar\"")

    assertEquals("foo* bar* OR foobar* OR \"foo* bar*\"", querySlot.captured)
  }

  @Test
  fun searchBuildsCjkQueryBySplittingIdeographicCharacters() = runTest {
    val querySlot = slot<String>()
    coEvery { appDao.getAppSearchItems(capture(querySlot)) } returns emptyList()

    searchManager.search("測試")

    assertEquals("測* 試* OR \"測\u200B試*\" OR 測試*", querySlot.captured)
  }

  @Test
  fun searchBuildsMultiWordCjkQuery() = runTest {
    val querySlot = slot<String>()
    coEvery { appDao.getAppSearchItems(capture(querySlot)) } returns emptyList()

    searchManager.search("測試 艾星")

    assertEquals("測* 試* 艾* 星* OR \"測\u200B試*\" \"艾\u200B星*\" OR 測試* 艾星*", querySlot.captured)
  }

  @Test
  fun searchUsesLocalIconForInstalledApps() = runTest {
    val item = buildSearchItem(packageName = "com.example.installed", repoId = 1L)
    coEvery { appDao.getAppSearchItems(any()) } returns listOf(item)
    every { installedAppsCache.isInstalled("com.example.installed") } returns true

    searchManager.search("installed")

    val results = searchManager.searchResults.value
    assertNotNull(results)
    assertEquals(1, results.apps.size)
    val listItem = results.apps.first()
    assertTrue(listItem.isInstalled)
    assertIs<PackageName>(listItem.iconModel)
    assertEquals("com.example.installed", listItem.iconModel.packageName)
  }

  @Test
  fun searchIsEmptyWhenThrowsSQLiteException() = runTest {
    coEvery { appDao.getAppSearchItems(any()) } throws SQLiteException("boom")

    searchManager.search("boom")

    val results = searchManager.searchResults.value
    assertNotNull(results)
    assertTrue(results.apps.isEmpty())
  }

  @Test
  fun searchFiltersCategoriesUsingNormalizedMatching() = runTest {
    val category = Category(repoId = 1L, id = "coffee", name = mapOf("en-US" to "Café"))
    categoriesLiveData.value = listOf(category)
    coEvery { appDao.getAppSearchItems(any()) } returns emptyList()

    searchManager.search("cafe")

    val results = searchManager.searchResults.value
    assertNotNull(results)
    assertEquals(1, results.categories.size)
    assertEquals("Café", results.categories.first().name)
  }

  @Test
  fun onSearchClearedResetsResultsToNull() = runTest {
    coEvery { appDao.getAppSearchItems(any()) } returns emptyList()

    searchManager.search("foo")
    assertNotNull(searchManager.searchResults.value)

    searchManager.onSearchCleared()

    assertNull(searchManager.searchResults.value)
  }

  private fun buildSearchItem(
    packageName: String,
    repoId: Long = 1L,
    appName: String = "Test App",
    summary: String = "Test Summary",
    lastUpdated: Long = 1234L,
    categories: List<String>? = null,
  ): AppSearchItem {
    val item: AppSearchItem = mockk()
    every { item.repoId } returns repoId
    every { item.packageName } returns packageName
    every { item.lastUpdated } returns lastUpdated
    every { item.name } returns mapOf("en-US" to appName)
    every { item.summary } returns mapOf("en-US" to summary)
    every { item.categories } returns categories
    every { item.getIcon(any()) } returns null
    return item
  }
}
