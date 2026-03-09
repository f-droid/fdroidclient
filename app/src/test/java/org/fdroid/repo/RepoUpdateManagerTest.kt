package org.fdroid.repo

import android.content.Context
import app.cash.turbine.test
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.fdroid.CompatibilityChecker
import org.fdroid.NotificationManager
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.Repository
import org.fdroid.database.RepositoryDao
import org.fdroid.index.IndexUpdateResult
import org.fdroid.index.RepoManager
import org.fdroid.index.RepoUpdater
import org.fdroid.install.InstalledAppsCache
import org.fdroid.settings.SettingsManager
import org.fdroid.updates.AppUpdateWorker
import org.fdroid.updates.UpdatesManager
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class RepoUpdateManagerTest {

  private val context: Context = mockk(relaxed = true)
  private val db: FDroidDatabase = mockk()
  private val repositoryDao: RepositoryDao = mockk()
  private val repoManager: RepoManager = mockk()
  private val settingsManager: SettingsManager = mockk()
  private val notificationManager: NotificationManager = mockk()
  private val compatibilityChecker: CompatibilityChecker = mockk()
  private val repoUpdater: RepoUpdater = mockk()
  private val installedAppsCache: InstalledAppsCache = mockk()

  init {
    // Mock calls into WorkManager which are used at UpdatesManager construction
    mockkObject(RepoUpdateWorker)
    every { RepoUpdateWorker.getAutoUpdateWorkInfo(any()) } returns flowOf(null)
    mockkObject(AppUpdateWorker)
    every { AppUpdateWorker.getAutoUpdateWorkInfo(any()) } returns flowOf(null)

    every { db.getRepositoryDao() } returns repositoryDao
    every { context.getString(any(), any()) } returns "repo update"
    every { settingsManager.isFirstStart } returns false
    every { installedAppsCache.installedApps } returns MutableStateFlow(emptyMap())
  }

  // The UpdatesManager needs a complex mock, because loadUpdates() accesses installedAppsCache
  // which is a private field to the class. No other solution was found to, so this one it is.
  private val updatesManager =
    spyk(
      UpdatesManager(
        context = mockk(relaxed = true),
        dbAppChecker = mockk(relaxed = true),
        settingsManager = mockk(relaxed = true),
        repoManager = mockk(relaxed = true),
        installedAppsCache = installedAppsCache,
        notificationManager = mockk(relaxed = true),
        updateInstaller = mockk(relaxed = true),
        coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
      )
    ) {
      every { loadUpdates() } returns mockk(relaxed = true)
    }

  private val repoUpdateManager =
    RepoUpdateManager(
      context = context,
      db = db,
      repoManager = repoManager,
      updatesManager = updatesManager,
      settingsManager = settingsManager,
      downloaderFactory = mockk(relaxed = true),
      notificationManager = notificationManager,
      compatibilityChecker = compatibilityChecker,
      repoUpdater = repoUpdater,
    )

  @Test
  fun `updating a single repo toggles isUpdating state, cleans up, checks for updates`() = runTest {
    val repo: Repository = mockk(relaxed = true)

    every { repoManager.getRepository(1L) } returns repo
    every { notificationManager.showUpdateRepoNotification(any(), false, null) } just runs
    every { repoUpdater.update(repo) } returns IndexUpdateResult.Processed
    every { notificationManager.cancelUpdateRepoNotification() } just runs
    every { repositoryDao.walCheckpoint() } just runs

    repoUpdateManager.isUpdating.test {
      assertFalse(awaitItem()) // not updating
      // do the update now
      assertEquals(IndexUpdateResult.Processed, repoUpdateManager.updateRepo(1L))
      assertTrue(awaitItem()) // now updating
      assertFalse(awaitItem()) // at the end again not updating
    }

    verify {
      notificationManager.showUpdateRepoNotification(any(), false, null)
      repositoryDao.walCheckpoint()
      updatesManager.loadUpdates()
      notificationManager.cancelUpdateRepoNotification()
    }
  }

  @Test
  fun `test updating single unchanged repo`() = runTest {
    val repo: Repository = mockk(relaxed = true)

    every { repoManager.getRepository(1L) } returns repo
    every { notificationManager.showUpdateRepoNotification(any(), false, null) } just runs
    every { repoUpdater.update(repo) } returns IndexUpdateResult.Unchanged
    every { notificationManager.cancelUpdateRepoNotification() } just runs
    every { repositoryDao.walCheckpoint() } just runs

    clearUpdateManagerMocks()

    repoUpdateManager.isUpdating.test {
      assertFalse(awaitItem()) // not updating
      assertEquals(IndexUpdateResult.Unchanged, repoUpdateManager.updateRepo(1L))
      assertTrue(awaitItem()) // now updating
      assertFalse(awaitItem()) // at the end again not updating
    }

    verify {
      notificationManager.cancelUpdateRepoNotification()
      repositoryDao.walCheckpoint()
    }
    verify(exactly = 0) { updatesManager.loadUpdates() }
  }

  @Test
  fun `update single repo exception still cleans up`() = runTest {
    val repo: Repository = mockk(relaxed = true)

    every { repoManager.getRepository(1L) } returns repo
    every { notificationManager.showUpdateRepoNotification(any(), false, null) } just runs
    every { repoUpdater.update(repo) } throws RuntimeException("boom")
    every { notificationManager.cancelUpdateRepoNotification() } just runs
    every { repositoryDao.walCheckpoint() } just runs

    repoUpdateManager.isUpdating.test {
      assertFalse(awaitItem()) // not updating
      assertFailsWith<RuntimeException> { repoUpdateManager.updateRepo(1L) }
      assertTrue(awaitItem()) // now updating
      assertFalse(awaitItem()) // at the end again not updating
    }

    verify {
      notificationManager.cancelUpdateRepoNotification()
      repositoryDao.walCheckpoint()
    }
  }

  @Test
  fun `update single repo returns NotFound when repo is missing`() = runTest {
    every { repoManager.getRepository(404L) } returns null

    assertEquals(IndexUpdateResult.NotFound, repoUpdateManager.updateRepo(404L))

    verify(exactly = 0) { notificationManager.showUpdateRepoNotification(any(), any(), any()) }
    verify(exactly = 0) { repositoryDao.walCheckpoint() }
  }

  @Test
  fun `updateRepos does not do quick recheck`() = runTest {
    every { settingsManager.lastRepoUpdate } returns (System.currentTimeMillis() - 500)

    repoUpdateManager.updateRepos()

    verify(exactly = 0) { repositoryDao.getRepositories() }
  }

  @Test
  fun `updateRepos updates enabled repos only`() = runTest {
    val repo1: Repository = mockk(relaxed = true) { every { enabled } returns true }
    val repo2: Repository = mockk(relaxed = true) { every { enabled } returns false }
    val repo3: Repository = mockk(relaxed = true) { every { enabled } returns true }

    every { settingsManager.lastRepoUpdate } returns 1337L
    every { repositoryDao.getRepositories() } returns listOf(repo1, repo2, repo3)
    every { notificationManager.showUpdateRepoNotification(any(), false, null) } just runs
    every { repoUpdater.update(repo1) } returns IndexUpdateResult.Unchanged
    every { repoUpdater.update(repo3) } returns IndexUpdateResult.Processed
    every { notificationManager.cancelUpdateRepoNotification() } just runs
    every { repositoryDao.walCheckpoint() } just runs
    every { settingsManager.lastRepoUpdate = any() } just runs

    repoUpdateManager.isUpdating.test {
      assertFalse(awaitItem())
      repoUpdateManager.updateRepos()
      assertTrue(awaitItem())
      assertFalse(awaitItem())
    }

    verify(exactly = 0) { repoUpdater.update(repo2) }
    verify(exactly = 1) {
      repoUpdater.update(repo1)
      repoUpdater.update(repo3)
      notificationManager.cancelUpdateRepoNotification()
      repositoryDao.walCheckpoint()
    }
  }

  @Test
  fun `updateRepos does not set lastRepoUpdate on first start when nothing was processed`() =
    runTest {
      val repo: Repository = mockk(relaxed = true) { every { enabled } returns true }

      every { settingsManager.lastRepoUpdate } returns 1337L
      every { settingsManager.isFirstStart } returns true
      every { repositoryDao.getRepositories() } returns listOf(repo)
      every { notificationManager.showUpdateRepoNotification(any(), false, null) } just runs
      every { repoUpdater.update(repo) } returns IndexUpdateResult.Unchanged
      every { notificationManager.cancelUpdateRepoNotification() } just runs
      every { repositoryDao.walCheckpoint() } just runs

      repoUpdateManager.updateRepos()

      verify(exactly = 0) { settingsManager.lastRepoUpdate = any() }
    }

  @Test
  fun `updateRepos sets lastRepoUpdate on first start when a repo was processed`() = runTest {
    val repo: Repository = mockk(relaxed = true) { every { enabled } returns true }

    every { settingsManager.lastRepoUpdate } returns 1337L
    every { settingsManager.isFirstStart } returns true
    every { repositoryDao.getRepositories() } returns listOf(repo)
    every { notificationManager.showUpdateRepoNotification(any(), false, null) } just runs
    every { repoUpdater.update(repo) } returns IndexUpdateResult.Processed
    every { notificationManager.cancelUpdateRepoNotification() } just runs
    every { repositoryDao.walCheckpoint() } just runs
    every { settingsManager.lastRepoUpdate = any() } just runs

    clearUpdateManagerMocks()

    repoUpdateManager.updateRepos()

    verify(exactly = 1) { settingsManager.lastRepoUpdate = any() }
    verify(exactly = 1) { updatesManager.loadUpdates() }
  }

  @Test
  fun `updateRepos shows app update notification when processed repos find app updates`() =
    runTest {
      val repo: Repository = mockk(relaxed = true) { every { enabled } returns true }

      every { settingsManager.lastRepoUpdate } returns 1337L
      every { repositoryDao.getRepositories() } returns listOf(repo)
      every { notificationManager.showUpdateRepoNotification(any(), false, null) } just runs
      every { repoUpdater.update(repo) } returns IndexUpdateResult.Processed
      every { notificationManager.cancelUpdateRepoNotification() } just runs
      every { repositoryDao.walCheckpoint() } just runs
      every { settingsManager.lastRepoUpdate = any() } just runs
      every { updatesManager.numUpdates } returns MutableStateFlow(2)
      every { updatesManager.notificationStates } returns mockk(relaxed = true)
      every { notificationManager.showAppUpdatesAvailableNotification(any()) } just runs

      clearUpdateManagerMocks()

      repoUpdateManager.updateRepos()

      verify(exactly = 1) { updatesManager.loadUpdates() }
      verify(exactly = 1) { notificationManager.showAppUpdatesAvailableNotification(any()) }
    }

  @Test
  fun `updateRepos does not show app update notification when processed repos find no app updates`() =
    runTest {
      val repo: Repository = mockk(relaxed = true) { every { enabled } returns true }

      every { settingsManager.lastRepoUpdate } returns 1337L
      every { repositoryDao.getRepositories() } returns listOf(repo)
      every { notificationManager.showUpdateRepoNotification(any(), false, null) } just runs
      every { repoUpdater.update(repo) } returns IndexUpdateResult.Processed
      every { notificationManager.cancelUpdateRepoNotification() } just runs
      every { repositoryDao.walCheckpoint() } just runs
      every { settingsManager.lastRepoUpdate = any() } just runs
      every { updatesManager.numUpdates } returns MutableStateFlow(0)

      clearUpdateManagerMocks()

      repoUpdateManager.updateRepos()

      verify(exactly = 1) { updatesManager.loadUpdates() }
      verify(exactly = 0) { notificationManager.showAppUpdatesAvailableNotification(any()) }
    }

  @Test
  fun `updateRepo error result does not trigger loadUpdates and still cleans up`() = runTest {
    val repo: Repository = mockk(relaxed = true)
    val error = IndexUpdateResult.Error(RuntimeException("update failed"))

    every { repoManager.getRepository(1L) } returns repo
    every { notificationManager.showUpdateRepoNotification(any(), false, null) } just runs
    every { repoUpdater.update(repo) } returns error
    every { notificationManager.cancelUpdateRepoNotification() } just runs
    every { repositoryDao.walCheckpoint() } just runs

    clearUpdateManagerMocks()

    assertEquals(error, repoUpdateManager.updateRepo(1L))

    // Verify loadUpdates was not called during updateRepo
    verify(exactly = 0) { updatesManager.loadUpdates() }
    verify(exactly = 1) {
      notificationManager.cancelUpdateRepoNotification()
      repositoryDao.walCheckpoint()
    }
  }

  /**
   * Workaround for [verify] calls trying to take installedAppsCache into account for
   * [UpdatesManager.loadUpdates].
   */
  private fun clearUpdateManagerMocks() {
    clearMocks(
      updatesManager,
      installedAppsCache,
      answers = false,
      recordedCalls = true,
      childMocks = false,
    )
  }
}
