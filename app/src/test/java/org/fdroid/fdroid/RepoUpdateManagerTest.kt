package org.fdroid.fdroid

import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import kotlinx.coroutines.runBlocking
import org.fdroid.CompatibilityChecker
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.Repository
import org.fdroid.database.RepositoryDao
import org.fdroid.fdroid.work.RepoUpdateWorker
import org.fdroid.index.IndexUpdateResult
import org.fdroid.index.RepoManager
import org.fdroid.index.RepoUpdater
import org.fdroid.index.v1.IndexV1Updater
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class RepoUpdateManagerTest {

    private val context: Context = mockk()
    private val db: FDroidDatabase = mockk()
    private val repoManager: RepoManager = mockk()
    private val notificationManager: NotificationManager = mockk()
    private val compatibilityChecker: CompatibilityChecker = mockk()
    private val repoUpdater: RepoUpdater = mockk()
    private val indexV1Updater: IndexV1Updater = mockk()

    private val packageManager: PackageManager = mockk()
    private val preferences: Preferences = mockk()
    private val repositoryDao: RepositoryDao = mockk()

    init {
        // needed for Flow#asLiveData()
        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mockk<Looper> {
            every { thread } returns Thread.currentThread()
        }
        // avoids having to deal with WorkManager here
        mockkObject(RepoUpdateWorker)
        every { RepoUpdateWorker.getAutoUpdateWorkInfo(any()) } returns mockk()

        mockkStatic(Preferences::get)
        every { Preferences.get() } returns preferences

        every { context.packageManager } returns packageManager
        every { context.getString(any(), any()) } returns "foo bar"
        every { db.getRepositoryDao() } returns repositoryDao
    }

    private val repoUpdateManager = RepoUpdateManager(
        context = context,
        db = db,
        repoManager = repoManager,
        notificationManager = notificationManager,
        compatibilityChecker = compatibilityChecker,
        repoUpdater = repoUpdater,
        indexV1Updater = null,
    )

    @Test
    fun testUpdateSingleRepo() {
        val repo: Repository = mockk(relaxed = true)

        every { repoManager.getRepository(1L) } returns repo
        every { preferences.isUpdateNotificationEnabled } returns true
        every { notificationManager.showUpdateRepoNotification(any(), false, null) } just Runs
        every { repoUpdater.update(repo) } returns IndexUpdateResult.Processed
        every { notificationManager.cancelUpdateRepoNotification() } just Runs
        every { repositoryDao.walCheckpoint() } just Runs

        runBlocking {
            repoUpdateManager.isUpdating.test {
                assertFalse(awaitItem()) // not updating
                // do the update now
                assertEquals(IndexUpdateResult.Processed, repoUpdateManager.updateRepo(1L))
                assertTrue(awaitItem()) // now updating
                assertFalse(awaitItem()) // at the end again not updating
            }
        }

        verify {
            notificationManager.cancelUpdateRepoNotification()
            repositoryDao.walCheckpoint()
        }
    }

    @Test
    fun testUpdateSingleForcedV1Repo() {
        val repoUpdateManager = RepoUpdateManager(
            context = context,
            db = db,
            repoManager = repoManager,
            notificationManager = notificationManager,
            compatibilityChecker = compatibilityChecker,
            repoUpdater = repoUpdater,
            indexV1Updater = indexV1Updater,
        )
        val repo: Repository = mockk(relaxed = true)

        every { repoManager.getRepository(1L) } returns repo
        every { preferences.isUpdateNotificationEnabled } returns true
        every { notificationManager.showUpdateRepoNotification(any(), false, null) } just Runs
        every { indexV1Updater.update(repo) } returns IndexUpdateResult.Unchanged
        every { notificationManager.cancelUpdateRepoNotification() } just Runs
        every { repositoryDao.walCheckpoint() } just Runs

        runBlocking {
            repoUpdateManager.isUpdating.test {
                assertFalse(awaitItem()) // not updating
                // do the update now and expect unchanged result
                assertEquals(IndexUpdateResult.Unchanged, repoUpdateManager.updateRepo(1L))
                assertTrue(awaitItem()) // now updating
                assertFalse(awaitItem()) // at the end again not updating
            }
        }

        verify {
            notificationManager.cancelUpdateRepoNotification()
            repositoryDao.walCheckpoint()
        }
    }

    @Test
    fun testUpdateSingleRepoCleansUpException() {
        val repo: Repository = mockk(relaxed = true)

        every { repoManager.getRepository(1L) } returns repo
        every { preferences.isUpdateNotificationEnabled } returns true
        every { notificationManager.showUpdateRepoNotification(any(), false, null) } just Runs
        every { repoUpdater.update(repo) } throws IOException()
        every { notificationManager.cancelUpdateRepoNotification() } just Runs
        every { repositoryDao.walCheckpoint() } just Runs

        runBlocking {
            repoUpdateManager.isUpdating.test {
                assertFalse(awaitItem()) // not updating
                // do the update now
                assertThrows(IOException::class.java) {
                    repoUpdateManager.updateRepo(1L)
                }
                assertTrue(awaitItem()) // now updating
                assertFalse(awaitItem()) // at the end again not updating
            }
        }

        verify {
            notificationManager.cancelUpdateRepoNotification()
            repositoryDao.walCheckpoint()
        }
    }

    @Test
    fun testUpdateReposDoesntDoQuickRecheck() {
        // we did a check just now
        every { preferences.lastUpdateCheck } returns System.currentTimeMillis() - 500

        repoUpdateManager.updateRepos()
    }

    @Test
    fun testUpdateThreeReposOneDisabled() {
        val repo1: Repository = mockk(relaxed = true) {
            every { enabled } returns true
        }
        val repo2: Repository = mockk(relaxed = true) {
            every { enabled } returns false
        }
        val repo3: Repository = mockk(relaxed = true) {
            every { enabled } returns true
        }

        every { preferences.lastUpdateCheck } returns 1337
        every { repositoryDao.getRepositories() } returns listOf(repo1, repo2)
        every { preferences.isUpdateNotificationEnabled } returns true
        every { notificationManager.showUpdateRepoNotification(any(), false, null) } just Runs
        every { repoUpdater.update(repo1) } returns IndexUpdateResult.Unchanged
        every { repoUpdater.update(repo3) } returns IndexUpdateResult.Processed

        every { notificationManager.cancelUpdateRepoNotification() } just Runs
        every { repositoryDao.walCheckpoint() } just Runs
        every { preferences.lastUpdateCheck = any() } just Runs

        runBlocking {
            repoUpdateManager.isUpdating.test {
                assertFalse(awaitItem()) // not updating
                repoUpdateManager.updateRepos()
                assertTrue(awaitItem()) // now updating
                assertFalse(awaitItem()) // at the end again not updating
            }
        }

        verify(exactly = 1) {
            notificationManager.cancelUpdateRepoNotification()
            repositoryDao.walCheckpoint()
        }
        // repo2 is disabled and should not get updated
        verify(exactly = 0) { repoUpdater.update(repo2) }
    }
}
