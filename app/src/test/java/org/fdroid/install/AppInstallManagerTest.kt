package org.fdroid.install

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.result.ActivityResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException
import org.fdroid.NotificationManager
import org.fdroid.database.AppMetadata
import org.fdroid.database.AppVersion
import org.fdroid.database.Repository
import org.fdroid.download.Downloader
import org.fdroid.download.DownloaderFactory
import org.fdroid.download.PackageName
import org.fdroid.download.getUri
import org.fdroid.fdroid.ProgressListener
import org.fdroid.history.HistoryManager
import org.fdroid.history.InstallEvent
import org.fdroid.index.v2.FileV1
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

internal class AppInstallManagerTest {

  @get:Rule var tmpFolder: TemporaryFolder = TemporaryFolder()

  private val context: Context = mockk(relaxed = true)
  private val downloaderFactory: DownloaderFactory = mockk(relaxed = true)
  private val sessionInstallManager: SessionInstallManager = mockk()
  private val notificationManager: NotificationManager = mockk()
  private val historyManager: HistoryManager = mockk(relaxed = true)
  private val iconResolver: InstallIconResolver = mockk(relaxed = true)
  private val scope = CoroutineScope(Dispatchers.Unconfined)

  private lateinit var appInstallManager: AppInstallManager

  private val packageInstaller: PackageInstaller = mockk()
  private val packageManager: PackageManager = mockk()
  private val downloader: Downloader = mockk(relaxed = true)

  private val packageName = "com.example.app"
  private val installedVersionName = "0.9"
  private val appMetadata =
    AppMetadata(
      repoId = 1L,
      packageName = packageName,
      added = 0L,
      lastUpdated = 0L,
      name = mapOf("en-US" to "Example App"),
      isCompatible = true,
    )
  private val version: AppVersion = mockk {
    every { packageName } returns this@AppInstallManagerTest.packageName
    every { versionName } returns "1.0"
    every { added } returns 1234L
    every { file } returns FileV1(name = "app.apk", sha256 = "abc", size = 1L)
  }
  private val repo: Repository =
    mockk(relaxed = true) { every { address } returns "https://f-droid.org/repo" }
  private val pendingIntent: PendingIntent = mockk(relaxed = true)
  private val userConfirmationNeeded =
    InstallState.UserConfirmationNeeded(
      name = "Example App",
      versionName = "1.0",
      currentVersionName = null,
      lastUpdated = 1234L,
      iconModel = null,
      sessionId = 42,
      intent = pendingIntent,
      creationTimeMillis = 1L,
      progress = 0.5f,
    )
  private val installedState =
    InstallState.Installed(
      name = "Example App",
      versionName = "1.0",
      currentVersionName = installedVersionName,
      lastUpdated = 1234L,
      iconModel = null,
    )

  @Before
  fun setUp() {
    mockkObject(AppInstallService.Companion)
    every { AppInstallService.isServiceRunning } returns true
    mockkStatic(::getUri)

    val cacheDir = tmpFolder.newFolder()
    every { context.cacheDir } returns cacheDir
    every { context.stopService(any()) } returns true
    every { context.packageManager } returns packageManager
    every { packageManager.packageInstaller } returns packageInstaller
    every { notificationManager.showAppInstallNotification(any()) } just runs
    every { notificationManager.cancelAppInstallNotification() } just runs
    every { downloaderFactory.create(any(), any(), any(), any()) } returns downloader
    every { getUri(any(), any()) } returns mockk(relaxed = true)

    appInstallManager =
      AppInstallManager(
        context = context,
        downloaderFactory = downloaderFactory,
        sessionInstallManager = sessionInstallManager,
        notificationManager = notificationManager,
        iconResolver = iconResolver,
        historyManager = historyManager,
        scope = scope,
      )
  }

  @After
  fun tearDown() {
    unmockkObject(AppInstallService.Companion)
    unmockkStatic(::getUri)
  }

  @Test
  fun `install downloads and records history when successful`() = runBlocking {
    coEvery { sessionInstallManager.requestPreapproval(any(), any(), any(), any(), any()) } returns
      PreApprovalResult.NotSupported
    coEvery { sessionInstallManager.install(any(), any(), any(), any()) } returns installedState

    val result = installApp()
    assertIs<InstallState.Installed>(result)
    assertIs<InstallState.Installed>(appInstallManager.getAppFlow(packageName).first())
    verify(atLeast = 1) {
      downloader.download()
      notificationManager.showAppInstallNotification(any())
      context.stopService(any<Intent>())
      historyManager.append(
        match<InstallEvent> {
          it.packageName == packageName &&
            it.versionName == "1.0" &&
            it.oldVersionName == installedVersionName
        }
      )
    }
  }

  @Test
  fun `install returns UserAborted when preapproval is aborted`() = runBlocking {
    coEvery { sessionInstallManager.requestPreapproval(any(), any(), any(), any(), any()) } returns
      PreApprovalResult.UserAborted

    val result = installApp()

    assertIs<InstallState.UserAborted>(result)
    assertIs<InstallState.UserAborted>(appInstallManager.getAppFlow(packageName).first())
    coVerify(exactly = 0) { sessionInstallManager.install(any(), any(), any(), any()) }
  }

  @Test
  fun `install returns Error when preapproval fails`() = runBlocking {
    coEvery { sessionInstallManager.requestPreapproval(any(), any(), any(), any(), any()) } returns
      PreApprovalResult.Error("preapproval failed")

    val result = installApp()

    assertIs<InstallState.Error>(result)
    assertEquals("preapproval failed", result.msg)
    coVerify(exactly = 0) { sessionInstallManager.install(any(), any(), any(), any()) }
  }

  @Test
  fun `install returns Error when download fails`() = runBlocking {
    coEvery { sessionInstallManager.requestPreapproval(any(), any(), any(), any(), any()) } returns
      PreApprovalResult.NotSupported
    every { downloader.download() } throws IOException("download boom")

    val result = installApp()

    assertIs<InstallState.Error>(result)
    assertEquals("Download failed: IOException download boom", result.msg)
    coVerify(exactly = 0) { sessionInstallManager.install(any(), any(), any(), any()) }
  }

  @Test
  fun `install handles unexpected job failures and stops service`() = runBlocking {
    coEvery { sessionInstallManager.requestPreapproval(any(), any(), any(), any(), any()) } throws
      IllegalStateException("boom")

    val result = installApp(currentVersionName = null)
    assertIs<InstallState.Error>(result)

    verifyOrder {
      notificationManager.showAppInstallNotification(any())
      context.stopService(any<Intent>())
      notificationManager.cancelAppInstallNotification()
    }
  }

  @Test
  fun `install with null appMetadata or repo clears Waiting state and stops service`() =
    runBlocking {
      appInstallManager.setWaitingState(
        packageName = packageName,
        name = "Example App",
        versionName = "1.0",
        currentVersionName = "0.9",
        lastUpdated = 1234L,
      )

      // randomly pass null appMetadata or repo to trigger the error state
      val metadata = if (Random.nextBoolean()) null else appMetadata
      val result =
        installApp(
          appMetadata = metadata,
          currentVersionName = "0.9",
          repo =
            if (metadata == null) {
              // if metadata is null, the error state will be triggered, so randomize nullability
              if (Random.nextBoolean()) null else repo
            } else {
              null
            },
        )

      assertIs<InstallState.Error>(result)
      assertIs<InstallState.Error>(appInstallManager.getAppFlow(packageName).first())
      verify(atLeast = 1) { context.stopService(any<Intent>()) }
    }

  @Test
  fun `install retries without session when pre-approved session reports PreApprovalError`() =
    runBlocking {
      coEvery {
        sessionInstallManager.requestPreapproval(any(), any(), any(), any(), any())
      } returns PreApprovalResult.NotSupported
      // first install call returns PreApproved with an error
      val preApprovalError = PreApprovalResult.Error("label mismatch")
      val preApprovedWithError =
        InstallState.PreApproved(
          name = "Example App",
          versionName = "1.0",
          currentVersionName = installedVersionName,
          lastUpdated = 1234L,
          iconModel = null,
          result = preApprovalError,
        )
      // second call succeeds, note below are returned two different results
      coEvery { sessionInstallManager.install(any(), any(), any(), any()) } returnsMany
        listOf(preApprovedWithError, installedState)

      val result = installApp()

      assertIs<InstallState.Installed>(result)
      // install must have been called twice: once with the session, once without (null session)
      coVerify(exactly = 2) { sessionInstallManager.install(any(), any(), any(), any()) }
    }

  @Test
  fun `second install call while in progress returns current state and does not restart flow`() =
    runBlocking {
      coEvery {
        sessionInstallManager.requestPreapproval(any(), any(), any(), any(), any())
      } returns PreApprovalResult.NotSupported
      coEvery { sessionInstallManager.install(any(), any(), any(), any()) } returns
        userConfirmationNeeded

      val firstResult = installApp()
      assertIs<InstallState.UserConfirmationNeeded>(firstResult)

      // calling install again for same app should gracefully return the current state
      // without triggering a new installation flow
      val secondResult = installApp()
      assertIs<InstallState.UserConfirmationNeeded>(secondResult)
      assertEquals(firstResult, secondResult)
      coVerify(exactly = 1) {
        sessionInstallManager.requestPreapproval(any(), any(), any(), any(), any())
        sessionInstallManager.install(any(), any(), any(), any())
      }
    }

  @Test
  fun `download progress updates Downloading state in installNotificationState`() = runBlocking {
    var notificationStateDuringDownload: InstallNotificationState? = null
    val listenerSlot = slot<ProgressListener>()
    every { downloader.setListener(capture(listenerSlot)) } just runs
    every { downloader.download() } answers
      {
        // emit a progress update mid-download and capture the notification state,
        // before the installation completes and the state transitions away from Downloading
        listenerSlot.captured.onProgress(bytesRead = 500L, totalBytes = 1000L)
        notificationStateDuringDownload = appInstallManager.installNotificationState
      }
    coEvery { sessionInstallManager.requestPreapproval(any(), any(), any(), any(), any()) } returns
      PreApprovalResult.NotSupported
    coEvery { sessionInstallManager.install(any(), any(), any(), any()) } returns installedState

    installApp()

    // the notification state captured mid-download must reflect the Downloading progress
    val state = checkNotNull(notificationStateDuringDownload)
    assertEquals(500L, state.numBytesDownloaded)
    assertEquals(1000L, state.numTotalBytes)
    assertEquals(50, state.percent)
    assertTrue(state.isInProgress)
    assertTrue(state.isInstallingSomeApp)
  }

  @Test
  fun `AppInstallService gets started when not already running`() = runBlocking {
    // service is not running at the start of this install
    every { AppInstallService.isServiceRunning } returns false andThen true
    every { context.startService(any()) } returns null

    coEvery { sessionInstallManager.requestPreapproval(any(), any(), any(), any(), any()) } returns
      PreApprovalResult.NotSupported
    coEvery { sessionInstallManager.install(any(), any(), any(), any()) } returns installedState

    installApp()

    // startService must get called while the installation was in progress
    verify(exactly = 1) { context.startService(any()) }
  }

  @Test
  fun `requestPreApprovalConfirmation success continues to install and records history`() =
    runBlocking {
      val userConfirmationRequired = PreApprovalResult.UserConfirmationRequired(42, pendingIntent)
      coEvery {
        sessionInstallManager.requestPreapproval(any(), any(), any(), any(), any())
      } returns userConfirmationRequired

      // install can do pre-approval, but we need confirmation from user first
      val installResult = installApp()
      assertIs<InstallState.PreApprovalConfirmationNeeded>(installResult)

      // pre-approval will succeed
      val preApprovedState =
        InstallState.PreApproved(
          name = "Example App",
          versionName = version.versionName,
          currentVersionName = installedVersionName,
          lastUpdated = version.added,
          iconModel = null,
          result = PreApprovalResult.Success(42),
        )
      coEvery {
        sessionInstallManager.requestUserConfirmation(any<InstallConfirmationState>())
      } returns preApprovedState
      coEvery { sessionInstallManager.install(any(), any(), any(), any()) } returns installedState

      // after successful pre-approval, the app was installed successfully
      val confirmationResult =
        appInstallManager.requestPreApprovalConfirmation(packageName, installResult)
      assertIs<InstallState.Installed>(confirmationResult)
      assertIs<InstallState.Installed>(appInstallManager.getAppFlow(packageName).first())
      verify(atLeast = 1) {
        downloader.download()
        historyManager.append(match<InstallEvent> { it.packageName == packageName })
      }
    }

  @Test
  fun `requestPreApprovalConfirmation returns null when state is not PreApprovalConfirmationNeeded`() =
    runBlocking {
      // put the app into a Waiting state instead of PreApprovalConfirmationNeeded
      appInstallManager.setWaitingState(
        packageName = packageName,
        name = "Example App",
        versionName = "1.0",
        currentVersionName = installedVersionName,
        lastUpdated = 1234L,
      )
      val fakeConfirmationState =
        InstallState.PreApprovalConfirmationNeeded(
          state =
            InstallState.Starting(
              name = "Example App",
              versionName = "1.0",
              currentVersionName = installedVersionName,
              lastUpdated = 1234L,
              iconModel = null,
            ),
          version = version,
          repo = repo,
          sessionId = 42,
          intent = pendingIntent,
        )

      val result =
        appInstallManager.requestPreApprovalConfirmation(packageName, fakeConfirmationState)

      assertEquals(null, result)
      coVerify(exactly = 0) { sessionInstallManager.requestUserConfirmation(any()) }
    }

  @Test
  fun `requestPreApprovalConfirmation handles exceptions and clears stale confirmation state`() =
    runBlocking {
      val preApprovalConfirmation = PreApprovalResult.UserConfirmationRequired(42, pendingIntent)
      coEvery {
        sessionInstallManager.requestPreapproval(any(), any(), any(), any(), any())
      } returns preApprovalConfirmation

      val installResult = installApp(currentVersionName = null)
      assertIs<InstallState.PreApprovalConfirmationNeeded>(installResult)

      coEvery {
        sessionInstallManager.requestUserConfirmation(any<InstallConfirmationState>())
      } throws IllegalStateException("confirm boom")

      // requesting pre-approval throws exception, so we get error state
      val confirmationResult =
        appInstallManager.requestPreApprovalConfirmation(packageName, installResult)
      assertIs<InstallState.Error>(confirmationResult)
      assertIs<InstallState.Error>(appInstallManager.getAppFlow(packageName).first())
      verify(atLeast = 1) { context.stopService(any<Intent>()) }
    }

  @Test
  @Suppress("UNCHECKED_CAST")
  fun `icon for pre-approval gets fetched from IconResolver`() = runBlocking {
    coEvery {
      sessionInstallManager.requestPreapproval(any(), any(), any(), any(), any())
    } coAnswers
      {
        // we can't really test that iconGetter gets called, so we grab it and call it ourselves
        val iconGetter = args[1] as suspend () -> Bitmap?
        iconGetter()
        PreApprovalResult.NotSupported
      }
    coEvery { sessionInstallManager.install(any(), any(), any(), any()) } returns installedState

    installApp(iconModel = PackageName(packageName, iconDownloadRequest = null))

    coVerify(atLeast = 1) { iconResolver.resolve(any()) }
  }

  @Test
  fun `requestUserConfirmation completes install and records success`() = runBlocking {
    coEvery { sessionInstallManager.requestPreapproval(any(), any(), any(), any(), any()) } returns
      PreApprovalResult.NotSupported
    coEvery { sessionInstallManager.install(any(), any(), any(), any()) } returns
      userConfirmationNeeded
    val installResult = installApp()
    assertIs<InstallState.UserConfirmationNeeded>(installResult)

    coEvery { sessionInstallManager.requestUserConfirmation(installResult) } returns installedState

    val result = appInstallManager.requestUserConfirmation(packageName, installResult)
    assertIs<InstallState.Installed>(result)
    assertIs<InstallState.Installed>(appInstallManager.getAppFlow(packageName).first())
    verify(atLeast = 1) {
      historyManager.append(match<InstallEvent> { it.packageName == packageName })
    }
  }

  @Test
  fun `requestUserConfirmation returns null when current state is not confirmation`() =
    runBlocking {
      appInstallManager.setWaitingState(
        packageName = packageName,
        name = "Example App",
        versionName = "1.0",
        currentVersionName = "0.9",
        lastUpdated = 1234L,
      )

      val result = appInstallManager.requestUserConfirmation(packageName, userConfirmationNeeded)

      assertEquals(null, result)
      coVerify(exactly = 0) { sessionInstallManager.requestUserConfirmation(any()) }
    }

  @Test
  fun `checkUserConfirmation with missing session transitions to UserAborted and stops service`() =
    runBlocking {
      coEvery {
        sessionInstallManager.requestPreapproval(any(), any(), any(), any(), any())
      } returns PreApprovalResult.NotSupported
      coEvery { sessionInstallManager.install(any(), any(), any(), any()) } returns
        userConfirmationNeeded

      // installing the app will need user confirmation
      installApp(currentVersionName = null)
      assertIs<InstallState.UserConfirmationNeeded>(
        appInstallManager.getAppFlow(packageName).first()
      )

      every { packageInstaller.getSessionInfo(42) } returns null

      // checking user confirmation fails due to missing session above, so we get into error state
      appInstallManager.checkUserConfirmation(packageName, userConfirmationNeeded)
      assertIs<InstallState.Error>(appInstallManager.getAppFlow(packageName).first())
      verify(atLeast = 1) { context.stopService(any<Intent>()) }
    }

  @Test
  fun `checkUserConfirmation resends confirmation intent only if session did not progress`(): Unit =
    runBlocking {
      coEvery {
        sessionInstallManager.requestPreapproval(any(), any(), any(), any(), any())
      } returns PreApprovalResult.NotSupported
      coEvery { sessionInstallManager.install(any(), any(), any(), any()) } returns
        userConfirmationNeeded
      val confirmationState = installApp()
      assertIs<InstallState.UserConfirmationNeeded>(confirmationState)

      val stalledSessionInfo: PackageInstaller.SessionInfo = mockk(relaxed = true)
      every { stalledSessionInfo.progress } returns userConfirmationNeeded.progress
      every { packageInstaller.getSessionInfo(42) } returns stalledSessionInfo

      // if session was stalled (progress did not advance), the confirmation intent gets send
      appInstallManager.checkUserConfirmation(packageName, confirmationState)
      verify(exactly = 1) { pendingIntent.send() }

      val progressedSessionInfo: PackageInstaller.SessionInfo = mockk(relaxed = true)
      every { progressedSessionInfo.progress } returns userConfirmationNeeded.progress + 0.1f
      every { packageInstaller.getSessionInfo(42) } returns progressedSessionInfo

      // if session progressed (progress higher now), the confirmation intent does not get send
      // because the user already confirmed and the installation is in progress
      appInstallManager.checkUserConfirmation(packageName, confirmationState)
      verify(exactly = 1) { pendingIntent.send() } // still only 1 send from above
      assertIs<InstallState.UserConfirmationNeeded>(
        appInstallManager.getAppFlow(packageName).first()
      )
    }

  @Test
  fun `onUninstallResult maps result codes and records history only for successful uninstall`() =
    runBlocking {
      val uninstallIntent: Intent = mockk(relaxed = true)
      every { uninstallIntent.getIntExtra(any(), any()) } returns 1337

      val okResult = ActivityResult(Activity.RESULT_OK, uninstallIntent)
      val abortedResult = ActivityResult(Activity.RESULT_FIRST_USER, uninstallIntent)
      val otherResult = ActivityResult(Activity.RESULT_CANCELED, uninstallIntent)

      // activity results that are not OK map to UserAborted
      val aborted = appInstallManager.onUninstallResult(packageName, "Example App", abortedResult)
      assertIs<InstallState.UserAborted>(aborted)
      val other = appInstallManager.onUninstallResult(packageName, "Example App", otherResult)
      assertIs<InstallState.UserAborted>(other)

      // no history events were recorded
      verify(exactly = 0) { historyManager.append(any()) }

      // an OK result maps to Uninstalled and records an uninstallation event in history
      val ok = appInstallManager.onUninstallResult(packageName, "Example App", okResult)
      assertIs<InstallState.Uninstalled>(ok)
      verify(exactly = 1) {
        historyManager.append(
          match { event ->
            event.packageName == packageName && event is org.fdroid.history.UninstallEvent
          }
        )
      }
    }

  @Test
  fun `cleanUp keeps in-progress states and removes terminal states`() = runBlocking {
    appInstallManager.setWaitingState(
      packageName = packageName,
      name = "Example App",
      versionName = "1.0",
      currentVersionName = "0.9",
      lastUpdated = 1234L,
    )
    // waiting is in-progress, so cleanup must not remove it
    appInstallManager.cleanUp(packageName)
    assertIs<InstallState.Waiting>(appInstallManager.getAppFlow(packageName).first())

    // after a terminal state, cleanup should remove the state entirely
    val uninstallIntent: Intent = mockk(relaxed = true)
    every { uninstallIntent.getIntExtra(any(), any()) } returns 1337
    appInstallManager.onUninstallResult(
      packageName = packageName,
      name = "Example App",
      activityResult = ActivityResult(Activity.RESULT_OK, uninstallIntent),
    )
    appInstallManager.cleanUp(packageName)

    assertIs<InstallState.Unknown>(appInstallManager.getAppFlow(packageName).first())
    verify(atLeast = 1) { context.stopService(any<Intent>()) }
  }

  @Test
  fun `cancel stops the running install job and produces UserAborted state`(): Unit = runBlocking {
    // make preapproval block until canceled
    coEvery {
      sessionInstallManager.requestPreapproval(any(), any(), any(), any(), any())
    } coAnswers
      {
        // simulate long-running preapproval that gets canceled
        delay(TimeUnit.SECONDS.toMillis(30))
        PreApprovalResult.NotSupported
      }

    // start the installation in the background so we can cancel() it
    val installScope = CoroutineScope(Dispatchers.Unconfined)
    val installJob =
      installScope.async {
        appInstallManager.install(
          appMetadata = appMetadata,
          version = version,
          currentVersionName = installedVersionName,
          repo = repo,
          iconModel = null,
          canAskPreApprovalNow = false,
        )
      }

    // cancel the job and ensure result is UserAborted
    appInstallManager.cancel(packageName)
    val result = installJob.await()
    assertIs<InstallState.UserAborted>(result)
  }

  private suspend fun installApp(
    appMetadata: AppMetadata? = this.appMetadata,
    version: AppVersion = this.version,
    currentVersionName: String? = installedVersionName,
    repo: Repository? = this.repo,
    iconModel: Any? = null,
    canAskPreApprovalNow: Boolean = false,
  ): InstallState {
    return appInstallManager.install(
      appMetadata = appMetadata,
      version = version,
      currentVersionName = currentVersionName,
      repo = repo,
      iconModel = iconModel,
      canAskPreApprovalNow = canAskPreApprovalNow,
    )
  }
}
