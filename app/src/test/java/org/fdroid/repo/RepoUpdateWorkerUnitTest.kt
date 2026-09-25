package org.fdroid.repo

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.fdroid.NotificationManager
import org.fdroid.history.HistoryManager
import org.fdroid.install.CacheCleaner
import org.junit.Before
import org.junit.Test

internal class RepoUpdateWorkerUnitTest {

  private val context: Context = mockk(relaxed = true)
  private val workerParams: WorkerParameters = mockk(relaxed = true)
  private val repoUpdateManager: RepoUpdateManager = mockk(relaxed = true)
  private val cacheCleaner: CacheCleaner = mockk(relaxed = true)
  private val historyManager: HistoryManager = mockk(relaxed = true)
  private val notificationManager: NotificationManager = mockk(relaxed = true)

  @Before
  fun setUp() {
    every { workerParams.inputData } returns workDataOf()
    every { workerParams.runAttemptCount } returns 0
    coEvery { repoUpdateManager.updateRepos() } just runs
    every { cacheCleaner.clean() } just runs
    every { historyManager.pruneEvents() } just runs
    every { notificationManager.cancelUpdateRepoNotification() } just runs
  }

  @Test
  fun `doWork cancels notification in finally on success`() = runTest {
    val worker =
      RepoUpdateWorker(
        appContext = context,
        workerParams = workerParams,
        repoUpdateManager = repoUpdateManager,
        cacheCleaner = cacheCleaner,
        historyManager = historyManager,
        nm = notificationManager,
      )

    worker.doWork()

    verify(exactly = 1) { notificationManager.cancelUpdateRepoNotification() }
  }

  @Test
  fun `doWork cancels notification in finally on failure`() = runTest {
    every { workerParams.runAttemptCount } returns 5
    coEvery { repoUpdateManager.updateRepos() } throws RuntimeException("boom")

    val worker =
      RepoUpdateWorker(
        appContext = context,
        workerParams = workerParams,
        repoUpdateManager = repoUpdateManager,
        cacheCleaner = cacheCleaner,
        historyManager = historyManager,
        nm = notificationManager,
      )

    worker.doWork()

    verify(exactly = 1) { notificationManager.cancelUpdateRepoNotification() }
  }

  @Test
  fun `updateNow uses enqueueUniqueWork with KEEP for all repos`() {
    mockkObject(WorkManager.Companion)
    val workManager: WorkManager = mockk(relaxed = true)
    every { WorkManager.getInstance(context) } returns workManager

    try {
      RepoUpdateWorker.updateNow(context)

      verify(exactly = 1) {
        workManager.enqueueUniqueWork(
          RepoUpdateWorker.UNIQUE_WORK_NAME_REPO_UPDATE_ALL,
          ExistingWorkPolicy.KEEP,
          any<OneTimeWorkRequest>(),
        )
      }
    } finally {
      unmockkObject(WorkManager.Companion)
    }
  }