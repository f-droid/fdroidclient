package org.fdroid.repo

import android.app.Notification
import android.content.Context
import android.os.Build.VERSION.SDK_INT
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import org.fdroid.NotificationManager
import org.fdroid.NotificationManager.Companion.NOTIFICATION_ID_REPO_UPDATE
import org.fdroid.history.HistoryManager
import org.fdroid.install.CacheCleaner
import org.fdroid.repo.RepoUpdateWorker.Companion.MAX_RUN_ATTEMPTS
import org.fdroid.repo.RepoUpdateWorker.Companion.UNIQUE_WORK_NAME_REPO_AUTO_UPDATE
import org.fdroid.settings.SettingsConstants.AutoUpdateValues
import org.fdroid.ui.utils.canStartForegroundService
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class RepoUpdateWorkerTest {

  private lateinit var context: Context
  private val workerParams: WorkerParameters = mockk(relaxed = true)
  private val repoUpdateManager: RepoUpdateManager = mockk(relaxed = true)
  private val cacheCleaner: CacheCleaner = mockk(relaxed = true)
  private val historyManager: HistoryManager = mockk(relaxed = true)
  private val notificationManager: NotificationManager = mockk(relaxed = true)

  @Before
  fun setUp() {
    // MockKAgentException: Mocking static is supported starting from Android P
    assumeTrue(SDK_INT >= 28)

    context = ApplicationProvider.getApplicationContext()
    WorkManagerTestInitHelper.initializeTestWorkManager(
      context,
      Configuration.Builder().setExecutor { it.run() }.build(),
    )

    mockkStatic("org.fdroid.ui.utils.UiUtilsKt")
    every { canStartForegroundService(any()) } returns false

    coEvery { repoUpdateManager.updateRepos() } just runs
    every { repoUpdateManager.updateRepo(any()) } returns mockk(relaxed = true)
    every { cacheCleaner.clean() } just runs
    every { historyManager.pruneEvents() } just runs
  }

  @Test
  fun schedulesWorkForAlwaysSetting() {
    RepoUpdateWorker.scheduleOrCancel(context, AutoUpdateValues.Always)

    val infos =
      WorkManager.getInstance(context)
        .getWorkInfosForUniqueWork(UNIQUE_WORK_NAME_REPO_AUTO_UPDATE)
        .get(5, TimeUnit.SECONDS)

    assertEquals(1, infos.size)
    assertEquals(WorkInfo.State.ENQUEUED, infos.first().state)
  }

  @Test
  fun cancelsWorkForNeverSetting() {
    RepoUpdateWorker.scheduleOrCancel(context, AutoUpdateValues.Always)
    RepoUpdateWorker.scheduleOrCancel(context, AutoUpdateValues.Never)

    val infos =
      WorkManager.getInstance(context)
        .getWorkInfosForUniqueWork(UNIQUE_WORK_NAME_REPO_AUTO_UPDATE)
        .get(5, TimeUnit.SECONDS)

    assertEquals(1, infos.size)
    assertEquals(WorkInfo.State.CANCELLED, infos.first().state)
  }

  @Test
  fun doWorkReturnsSuccessAndUpdatesAllRepos() = runTest {
    every { workerParams.inputData } returns workDataOf()

    val worker =
      RepoUpdateWorker(
        appContext = context,
        workerParams = workerParams,
        repoUpdateManager = repoUpdateManager,
        cacheCleaner = cacheCleaner,
        historyManager = historyManager,
        nm = notificationManager,
      )

    val result = worker.doWork()

    assertEquals(ListenableWorker.Result.success(), result)
    coVerify(exactly = 1) {
      repoUpdateManager.updateRepos()
      cacheCleaner.clean(any())
      historyManager.pruneEvents()
    }
    // did not update a single repo, only all repos above
    verify(exactly = 0) { repoUpdateManager.updateRepo(any()) }
  }

  @Test
  fun doWorkUpdatesSingleRepoWhenRepoIdIsProvided() = runTest {
    every { workerParams.inputData } returns workDataOf("repoId" to 42L)

    val worker =
      RepoUpdateWorker(
        appContext = context,
        workerParams = workerParams,
        repoUpdateManager = repoUpdateManager,
        cacheCleaner = cacheCleaner,
        historyManager = historyManager,
        nm = notificationManager,
      )

    val result = worker.doWork()

    assertEquals(ListenableWorker.Result.success(), result)
    verify(exactly = 1) {
      repoUpdateManager.updateRepo(42L)
      cacheCleaner.clean(any())
      historyManager.pruneEvents()
    }
    coVerify(exactly = 0) { repoUpdateManager.updateRepos() }
  }

  @Test
  fun doWorkRetriesWhenUpdateThrowsAndRunAttemptNotExceeded() = runTest {
    every { workerParams.inputData } returns workDataOf()
    every { workerParams.runAttemptCount } returns MAX_RUN_ATTEMPTS
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

    val result = worker.doWork()

    assertEquals(ListenableWorker.Result.retry(), result)
    coVerify(exactly = 0) { cacheCleaner.clean() }
    coVerify(exactly = 0) { historyManager.pruneEvents() }
  }

  @Test
  fun doWorkFailsWhenUpdateThrowsAndRunAttemptCountIsGreaterThan3() = runTest {
    every { workerParams.inputData } returns workDataOf()
    every { workerParams.runAttemptCount } returns MAX_RUN_ATTEMPTS + 1
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

    val result = worker.doWork()

    assertEquals(ListenableWorker.Result.failure(), result)
    verify(exactly = 0) { cacheCleaner.clean() }
    verify(exactly = 0) { historyManager.pruneEvents() }
  }

  @Test
  fun getForegroundInfoReturnsExpectedNotificationId() = runTest {
    val builder: NotificationCompat.Builder = mockk()
    val notification: Notification = mockk(relaxed = true)
    every { notificationManager.getRepoUpdateNotification() } returns builder
    every { builder.build() } returns notification

    val worker =
      RepoUpdateWorker(
        appContext = context,
        workerParams = workerParams,
        repoUpdateManager = repoUpdateManager,
        cacheCleaner = cacheCleaner,
        historyManager = historyManager,
        nm = notificationManager,
      )

    val info = worker.getForegroundInfo()

    assertEquals(NOTIFICATION_ID_REPO_UPDATE, info.notificationId)
    assertIs<Notification>(info.notification)
  }
}
