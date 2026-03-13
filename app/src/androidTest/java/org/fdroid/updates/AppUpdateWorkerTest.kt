package org.fdroid.updates

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
import org.fdroid.NotificationManager.Companion.NOTIFICATION_ID_APP_INSTALLS
import org.fdroid.install.AppInstallManager
import org.fdroid.install.AppState
import org.fdroid.install.AppStateCategory
import org.fdroid.install.InstallNotificationState
import org.fdroid.settings.SettingsConstants.AutoUpdateValues
import org.fdroid.ui.utils.canStartForegroundService
import org.fdroid.updates.AppUpdateWorker.Companion.MAX_RUN_ATTEMPTS
import org.fdroid.updates.AppUpdateWorker.Companion.UNIQUE_WORK_NAME_APP_UPDATE
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class AppUpdateWorkerTest {

  private lateinit var context: Context
  private val workerParams: WorkerParameters = mockk(relaxed = true)
  private val notificationManager: NotificationManager = mockk(relaxed = true)
  private val updatesManager: UpdatesManager = mockk(relaxed = true)
  private val appInstallManager: AppInstallManager = mockk(relaxed = true)

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
    every { notificationManager.cancelAppUpdatesAvailableNotification() } just runs
    coEvery { updatesManager.updateAll(false) } just runs
    every { notificationManager.showInstallSuccessNotification(any()) } just runs
  }

  @Test
  fun schedulesWorkForAlwaysSetting() {
    AppUpdateWorker.scheduleOrCancel(context, AutoUpdateValues.Always)

    val infos =
      WorkManager.getInstance(context)
        .getWorkInfosForUniqueWork(UNIQUE_WORK_NAME_APP_UPDATE)
        .get(5, TimeUnit.SECONDS)

    assertEquals(1, infos.size)
    assertEquals(WorkInfo.State.ENQUEUED, infos.first().state)
  }

  @Test
  fun cancelsWorkForNeverSetting() {
    AppUpdateWorker.scheduleOrCancel(context, AutoUpdateValues.Always)
    AppUpdateWorker.scheduleOrCancel(context, AutoUpdateValues.Never)

    val infos =
      WorkManager.getInstance(context)
        .getWorkInfosForUniqueWork(UNIQUE_WORK_NAME_APP_UPDATE)
        .get(5, TimeUnit.SECONDS)

    assertEquals(1, infos.size)
    assertEquals(WorkInfo.State.CANCELLED, infos.first().state)
  }

  @Test
  fun doWorkReturnsSuccessAndShowsNotificationWhenAppsWereInstalled() = runTest {
    val appState =
      AppState(
        packageName = "com.example.app",
        category = AppStateCategory.INSTALLED,
        name = "Example",
        installVersionName = "1.0",
        currentVersionName = "0.9",
      )
    val apps = listOf(appState)
    val installedState =
      InstallNotificationState(apps = apps, numBytesDownloaded = 0, numTotalBytes = 0)

    every { appInstallManager.installNotificationState } returns installedState

    val worker =
      AppUpdateWorker(context, workerParams, notificationManager, updatesManager, appInstallManager)

    val result = worker.doWork()

    assertEquals(ListenableWorker.Result.success(), result)
    verify(exactly = 1) {
      notificationManager.cancelAppUpdatesAvailableNotification()
      notificationManager.showInstallSuccessNotification(installedState)
    }
    coVerify(exactly = 1) { updatesManager.updateAll(false) }
  }

  @Test
  fun doWorkRetriesWhenUpdateAllThrowsAndNotTooManyAttempts() = runTest {
    every { workerParams.runAttemptCount } returns MAX_RUN_ATTEMPTS
    coEvery { updatesManager.updateAll(false) } throws RuntimeException("boom")

    val worker =
      AppUpdateWorker(context, workerParams, notificationManager, updatesManager, appInstallManager)

    val result = worker.doWork()

    assertEquals(ListenableWorker.Result.retry(), result)
  }

  @Test
  fun doWorkFailsWhenUpdateAllThrowsAndTooManyAttempts() = runTest {
    every { workerParams.runAttemptCount } returns MAX_RUN_ATTEMPTS + 1
    coEvery { updatesManager.updateAll(false) } throws RuntimeException("boom")

    val worker =
      AppUpdateWorker(context, workerParams, notificationManager, updatesManager, appInstallManager)

    val result = worker.doWork()

    assertEquals(ListenableWorker.Result.failure(), result)
  }

  @Test
  fun getForegroundInfoReturnsExpectedNotificationId() = runTest {
    val builder: NotificationCompat.Builder = mockk()
    val notification: Notification = mockk(relaxed = true)
    every { notificationManager.getAppInstallNotification(any()) } returns builder
    every { builder.build() } returns notification

    val worker =
      AppUpdateWorker(context, workerParams, notificationManager, updatesManager, appInstallManager)

    val info = worker.getForegroundInfo()

    assertEquals(NOTIFICATION_ID_APP_INSTALLS, info.notificationId)
    assertIs<Notification>(info.notification)
  }
}
