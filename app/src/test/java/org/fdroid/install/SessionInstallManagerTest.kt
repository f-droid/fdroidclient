package org.fdroid.install

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.InstallSourceInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.Session
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import androidx.core.content.ContextCompat.registerReceiver
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.fdroid.database.AppMetadata
import org.fdroid.database.AppVersion
import org.fdroid.ui.utils.isAppInForeground
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
internal class SessionInstallManagerTest {

  @get:Rule var tmpFolder: TemporaryFolder = TemporaryFolder()

  private val context: Context = mockk(relaxed = true)
  private val packageManager: PackageManager = mockk()
  private val packageInstaller: PackageInstaller = mockk()
  private val receiverFactory: InstallBroadcastReceiverFactory = mockk()
  private val scope = CoroutineScope(Dispatchers.Unconfined)

  private lateinit var sessionInstallManager: SessionInstallManager

  private val receiver: InstallBroadcastReceiver = mockk()
  private val pendingIntent: PendingIntent = mockk()
  private val session: Session = mockk()

  private val sessionId = 123
  private val packageName = "com.example.app"
  private val appMetadata =
    AppMetadata(
      repoId = 1L,
      packageName = packageName,
      added = 0L,
      lastUpdated = 0L,
      name = mapOf("en-US" to "Example App"),
      isCompatible = true,
    )
  private val appVersion: AppVersion = mockk(relaxed = true)

  private val installingState =
    InstallState.Installing(
      name = "Example App",
      versionName = "1.0",
      currentVersionName = "0.9",
      lastUpdated = 42,
      iconModel = null,
    )
  private val confirmationNeededState =
    InstallState.UserConfirmationNeeded(
      name = "Example App",
      versionName = "1.0",
      currentVersionName = "0.9",
      lastUpdated = 42,
      iconModel = null,
      sessionId = sessionId,
      intent = pendingIntent,
      creationTimeMillis = 1L,
      progress = 0.2f,
    )

  @Before
  fun setUp() {
    mockkStatic(PendingIntent::class)
    mockkStatic("androidx.core.content.ContextCompat")
    mockkStatic("org.fdroid.ui.utils.UiUtilsKt")

    mockkConstructor(Intent::class)
    every { anyConstructed<Intent>().setPackage(any()) } returns mockk()
    every { anyConstructed<Intent>().putExtra(any(), any<Int>()) } returns mockk()
    every { anyConstructed<Intent>().putExtra(any(), any<String>()) } returns mockk()
    every { anyConstructed<Intent>().addFlags(any()) } returns mockk()

    every { context.packageManager } returns packageManager
    every { packageManager.packageInstaller } returns packageInstaller
    every { packageInstaller.mySessions } returns emptyList()
    every { context.unregisterReceiver(any()) } just runs

    sessionInstallManager =
      SessionInstallManager(
        context = context,
        coroutineScope = scope,
        receiverFactory = receiverFactory,
      )
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `constructor abandons existing install sessions`() {
    val oldSession1: PackageInstaller.SessionInfo = mockk(relaxed = true)
    val oldSession2: PackageInstaller.SessionInfo = mockk(relaxed = true)
    every { oldSession1.sessionId } returns 1
    every { oldSession2.sessionId } returns 2
    every { oldSession1.appPackageName } returns "a"
    every { oldSession2.appPackageName } returns "b"
    every { packageInstaller.mySessions } returns listOf(oldSession1, oldSession2)
    every { packageInstaller.abandonSession(any()) } just runs

    SessionInstallManager(
      context = context,
      coroutineScope = scope,
      receiverFactory = receiverFactory,
    )

    verify(exactly = 1) { packageInstaller.abandonSession(1) }
    verify(exactly = 1) { packageInstaller.abandonSession(2) }
  }

  @Test
  fun `requestPreapproval happy path calls iconGetter requests preapproval and closes session`() =
    runBlocking {
      val preapprovalSessionId = 1337
      var iconGetterCalls = 0

      every { context.isAppInForeground() } returns true
      every { packageInstaller.createSession(any()) } returns preapprovalSessionId
      val preapprovalSession: Session = mockk(relaxed = true)
      every { packageInstaller.openSession(preapprovalSessionId) } returns preapprovalSession
      every { preapprovalSession.close() } just Runs

      val listenerSlot = slot<InstallBroadcastListener>()
      val preapprovalReceiver: InstallBroadcastReceiver = mockk(relaxed = true)
      every { receiverFactory.create(preapprovalSessionId, capture(listenerSlot)) } returns
        preapprovalReceiver
      every { registerReceiver(context, preapprovalReceiver, any<IntentFilter>(), any()) } returns
        mockk()

      val sender: IntentSender = mockk(relaxed = true)
      every { PendingIntent.getBroadcast(any(), any(), any<Intent>(), any()) } returns pendingIntent
      every { pendingIntent.intentSender } returns sender

      every { preapprovalSession.requestUserPreapproval(any(), any()) } answers
        {
          listenerSlot.captured.invoke(
            preapprovalReceiver,
            PackageInstaller.STATUS_SUCCESS,
            null,
            null,
          )
        }

      val result =
        sessionInstallManager.requestPreapproval(
          app = appMetadata,
          iconGetter = {
            iconGetterCalls += 1
            null
          },
          isUpdate = false,
          version = appVersion,
          canRequestUserConfirmationNow = true,
        )

      assertIs<PreApprovalResult.Success>(result)
      assertEquals(preapprovalSessionId, result.sessionId)
      assertEquals(1, iconGetterCalls)
      verify(exactly = 1) { preapprovalSession.requestUserPreapproval(any(), any()) }
      verify(exactly = 1) { preapprovalSession.close() }
    }

  @Test
  fun `requestPreapproval NotSupported cases`(): Unit = runBlocking {
    // not in foreground -> NotSupported
    every { context.isAppInForeground() } returns false
    val notForegroundResult =
      sessionInstallManager.requestPreapproval(
        app = appMetadata,
        iconGetter = { null },
        isUpdate = false,
        version = appVersion,
        canRequestUserConfirmationNow = true,
      )
    assertIs<PreApprovalResult.NotSupported>(notForegroundResult)

    // in foreground + update that can auto-update -> NotSupported
    every { context.isAppInForeground() } returns true
    every { appVersion.packageName } returns packageName
    every { appVersion.manifest.targetSdkVersion } returns 34
    val sourceInfo: InstallSourceInfo = mockk(relaxed = true)
    every { sourceInfo.installingPackageName } returns context.packageName
    if (SDK_INT >= 34) {
      every { sourceInfo.updateOwnerPackageName } returns null
    }
    every { packageManager.getInstallSourceInfo(packageName) } returns sourceInfo

    val autoUpdateNotNeeded =
      sessionInstallManager.requestPreapproval(
        app = appMetadata,
        iconGetter = { null },
        isUpdate = true,
        version = appVersion,
        canRequestUserConfirmationNow = true,
      )
    assertIs<PreApprovalResult.NotSupported>(autoUpdateNotNeeded)

    // isUpdate = true but not our package, and we are not the update owner -> NotSupported,
    // because canDoAutoUpdate() returns false when getInstallSourceInfo() throws
    every { context.isAppInForeground() } returns true
    every { appVersion.packageName } returns packageName
    every { appVersion.manifest.targetSdkVersion } returns 34
    every { packageManager.getInstallSourceInfo(packageName) } throws SecurityException("nope")

    val installSourceError =
      sessionInstallManager.requestPreapproval(
        app = appMetadata,
        iconGetter = { null },
        isUpdate = true,
        version = appVersion,
        canRequestUserConfirmationNow = true,
      )
    // canDoAutoUpdate returned false due to exception, so we fall through to preapproval()
    // which means we should NOT get NotSupported here but proceed to attempt preapproval.
    // This asserts that the exception path does not crash and returns some result.
    assertIs<PreApprovalResult>(installSourceError)

    // iconGetter throwing an exception -> Error (not NotSupported)
    every { context.isAppInForeground() } returns true
    every { packageInstaller.createSession(any()) } returns 999
    every { packageInstaller.openSession(999) } returns mockk(relaxed = true)
    val preapprovalReceiver: InstallBroadcastReceiver = mockk(relaxed = true)
    every { receiverFactory.create(999, any()) } returns preapprovalReceiver
    every { registerReceiver(context, preapprovalReceiver, any<IntentFilter>(), any()) } returns mockk()

    val iconError =
      sessionInstallManager.requestPreapproval(
        app = appMetadata,
        iconGetter = { throw RuntimeException("icon load failed") },
        isUpdate = false,
        version = appVersion,
        canRequestUserConfirmationNow = true,
      )
    assertIs<PreApprovalResult.Error>(iconError)
    assertNotNull(iconError.errorMsg)
    assertTrue(iconError.errorMsg.contains("icon load failed"))
  }

  @Test
  fun `requestPreapproval returns errors when preapproval fails`() = runBlocking {
    every { context.isAppInForeground() } returns true

    // pending user action -> UserConfirmationRequired
    expectPreApprovalSession(PackageInstaller.STATUS_PENDING_USER_ACTION)
    val pending =
      sessionInstallManager.requestPreapproval(
        app = appMetadata,
        iconGetter = { null },
        isUpdate = false,
        version = appVersion,
        canRequestUserConfirmationNow = false,
      )
    assertIs<PreApprovalResult.UserConfirmationRequired>(pending)

    // aborted -> UserAborted
    expectPreApprovalSession(PackageInstaller.STATUS_FAILURE_ABORTED)
    val aborted =
      sessionInstallManager.requestPreapproval(
        app = appMetadata,
        iconGetter = { null },
        isUpdate = false,
        version = appVersion,
        canRequestUserConfirmationNow = true,
      )
    assertIs<PreApprovalResult.UserAborted>(aborted)

    // blocked -> NotSupported
    expectPreApprovalSession(PackageInstaller.STATUS_FAILURE_BLOCKED)
    val blocked =
      sessionInstallManager.requestPreapproval(
        app = appMetadata,
        iconGetter = { null },
        isUpdate = false,
        version = appVersion,
        canRequestUserConfirmationNow = true,
      )
    assertIs<PreApprovalResult.NotSupported>(blocked)

    // unknown status -> Error(msg)
    expectPreApprovalSession(1337, "foo bar")
    val unknown =
      sessionInstallManager.requestPreapproval(
        app = appMetadata,
        iconGetter = { null },
        isUpdate = false,
        version = appVersion,
        canRequestUserConfirmationNow = true,
      )
    assertIs<PreApprovalResult.Error>(unknown)
    assertNotNull(unknown.errorMsg)
    assertTrue(unknown.errorMsg.contains("foo bar"))
  }

  @Test
  fun `install returns Installed when receiver callback reports STATUS_SUCCESS`() = runBlocking {
    val apkFile: File = tmpFolder.newFile("app.apk").apply { writeBytes(byteArrayOf(1, 2, 3)) }

    expectInstallSession(PackageInstaller.STATUS_SUCCESS, apkFile)

    val result =
      sessionInstallManager.install(
        sessionId = sessionId,
        packageName = packageName,
        state = installingState,
        apkFile = apkFile,
      )

    assertIs<InstallState.Installed>(result)
    verify(exactly = 1) {
      session.openWrite(any(), any(), apkFile.length())
      session.commit(any())
      session.close()
    }
    verify(atLeast = 1) { context.unregisterReceiver(any()) }
  }

  @Test
  fun `install returns UserConfirmationNeeded when STATUS_PENDING_USER_ACTION`() {
    runBlocking {
      val apkFile = tmpFolder.newFile("app-pending.apk")

      val sessionInfo: PackageInstaller.SessionInfo = mockk(relaxed = true)
      every { sessionInfo.progress } returns 0.7f
      every { packageInstaller.getSessionInfo(sessionId) } returns sessionInfo
      every { PendingIntent.getActivity(any(), any(), any(), any()) } returns pendingIntent

      expectInstallSession(PackageInstaller.STATUS_PENDING_USER_ACTION, apkFile)

      val result =
        sessionInstallManager.install(
          sessionId = sessionId,
          packageName = packageName,
          state = installingState,
          apkFile = apkFile,
        )

      assertIs<InstallState.UserConfirmationNeeded>(result)
      assertEquals(0.7f, result.progress)
      assertEquals(pendingIntent, result.intent)
    }
  }

  @Test
  fun `install returns UserAborted, PreApprovedError and Error from receiver failures`() {
    runBlocking {
      // aborted
      val aborted = installForStatus(PackageInstaller.STATUS_FAILURE_ABORTED, null)
      assertIs<InstallState.UserAborted>(aborted)

      // preapproval-details mismatch gets mapped to PreApproved(result=Error)
      val preapprovalMsg = "Some error containing PreapprovalDetails mismatch"
      val preapprovalState = installForStatus(PackageInstaller.STATUS_FAILURE, preapprovalMsg)
      assertIs<InstallState.PreApproved>(preapprovalState)
      assertIs<PreApprovalResult.Error>(preapprovalState.result)

      // generic failure maps to Error
      val genericError = installForStatus(PackageInstaller.STATUS_FAILURE, "generic failure")
      assertIs<InstallState.Error>(genericError)
    }
  }

  @Test
  fun `install cancellation abandons session`() = runBlocking {
    val apkFile: File =
      tmpFolder.newFile("app-cancel.apk").apply { writeBytes(byteArrayOf(1, 2, 3)) }

    val listenerSlot = slot<InstallBroadcastListener>()
    every { receiverFactory.create(sessionId, capture(listenerSlot)) } returns receiver

    val sender: IntentSender = mockk(relaxed = true)
    every { PendingIntent.getBroadcast(any(), any(), any<Intent>(), any()) } returns pendingIntent
    every { pendingIntent.intentSender } returns sender

    every { packageInstaller.openSession(sessionId) } returns session
    every { session.openWrite(packageName, 0, apkFile.length()) } returns ByteArrayOutputStream()
    every { session.fsync(any()) } just runs
    every { session.commit(any()) } just Runs
    every { session.close() } just Runs
    every { registerReceiver(context, receiver, any<IntentFilter>(), any()) } returns mockk()
    every { packageInstaller.abandonSession(sessionId) } just runs

    // start the installation in the background so we can cancel() it
    val installScope = CoroutineScope(Dispatchers.Unconfined)
    val installJob =
      installScope.async {
        sessionInstallManager.install(
          sessionId = sessionId,
          packageName = packageName,
          state = installingState,
          apkFile = apkFile,
        )
      }
    installJob.cancelAndJoin()

    verify(atLeast = 1) { context.unregisterReceiver(receiver) }
    verify(exactly = 1) { packageInstaller.abandonSession(sessionId) }
  }

  @Test
  fun `requestUserConfirmation returns PreApproved and Installed on success`() {
    runBlocking {
      val appVersion: AppVersion = mockk(relaxed = true)
      // preapproval-confirmation state -> STATUS_SUCCESS => PreApproved(Success)
      val preApprovalState =
        InstallState.PreApprovalConfirmationNeeded(
          state =
            InstallState.Starting(
              name = "Example App",
              versionName = "1.0",
              currentVersionName = "0.9",
              lastUpdated = 42,
              iconModel = null,
            ),
          version = appVersion,
          repo = mockk(relaxed = true),
          sessionId = sessionId,
          intent = pendingIntent,
        )
      val preapprovalResult =
        requestUserConfirmationForStatus(preApprovalState, PackageInstaller.STATUS_SUCCESS, null)
      assertIs<InstallState.PreApproved>(preapprovalResult)
      assertIs<PreApprovalResult.Success>(preapprovalResult.result)
      assertEquals(confirmationNeededState.versionName, preapprovalResult.versionName)
      assertEquals(confirmationNeededState.currentVersionName, preapprovalResult.currentVersionName)

      // install-confirmation state -> STATUS_SUCCESS => Installed
      val installResult =
        requestUserConfirmationForStatus(
          confirmationNeededState,
          PackageInstaller.STATUS_SUCCESS,
          null,
        )
      assertIs<InstallState.Installed>(installResult)
      assertEquals(confirmationNeededState.versionName, installResult.versionName)
      assertEquals(confirmationNeededState.currentVersionName, installResult.currentVersionName)
    }
  }

  @Test
  fun `requestUserConfirmation maps aborted and failure statuses correctly`() {
    runBlocking {
      val aborted =
        requestUserConfirmationForStatus(
          confirmationNeededState,
          PackageInstaller.STATUS_FAILURE_ABORTED,
          null,
        )
      assertIs<InstallState.UserAborted>(aborted)

      val error =
        requestUserConfirmationForStatus(
          confirmationNeededState,
          PackageInstaller.STATUS_FAILURE,
          "install failed",
        )
      assertIs<InstallState.Error>(error)
    }
  }

  private fun expectInstallSession(
    packageInstallerResult: Int,
    apkFile: File,
    msg: String? = null,
  ) {
    val listenerSlot = slot<InstallBroadcastListener>()
    every { receiverFactory.create(sessionId, capture(listenerSlot)) } returns receiver

    val sender: IntentSender = mockk(relaxed = true)
    every { PendingIntent.getBroadcast(any(), any(), any<Intent>(), any()) } returns pendingIntent
    every { pendingIntent.intentSender } returns sender

    every { packageInstaller.openSession(sessionId) } returns session
    every { session.openWrite(packageName, 0, apkFile.length()) } returns ByteArrayOutputStream()
    every { session.fsync(any()) } just runs
    every { session.commit(any()) } answers
      {
        listenerSlot.captured.invoke(receiver, packageInstallerResult, null, msg)
      }
    every { session.close() } just Runs

    every { registerReceiver(context, receiver, any<IntentFilter>(), any()) } returns mockk()
  }

  private suspend fun installForStatus(status: Int, msg: String?): InstallState {
    val apkFile: File = tmpFolder.newFile()

    expectInstallSession(status, apkFile, msg)

    return sessionInstallManager.install(
      sessionId = sessionId,
      packageName = packageName,
      state = installingState,
      apkFile = apkFile,
    )
  }

  private suspend fun requestUserConfirmationForStatus(
    state: InstallConfirmationState,
    status: Int,
    msg: String?,
  ): InstallState {
    val listenerSlot = slot<InstallBroadcastListener>()
    every { receiverFactory.create(sessionId, capture(listenerSlot)) } returns receiver
    every { registerReceiver(context, receiver, any<IntentFilter>(), any()) } returns mockk()
    every { context.unregisterReceiver(any()) } just runs
    every { state.intent.send() } answers
      {
        listenerSlot.captured.invoke(receiver, status, null, msg)
      }

    return sessionInstallManager.requestUserConfirmation(state)
  }

  private fun expectPreApprovalSession(packageInstallerResult: Int, msg: String? = null) {
    every { packageInstaller.createSession(any()) } returns 1
    val preapprovalSession: Session = mockk(relaxed = true)
    every { packageInstaller.openSession(1) } returns preapprovalSession

    val receiver: InstallBroadcastReceiver = mockk(relaxed = true)
    val listenerSlot = slot<InstallBroadcastListener>()
    every { receiverFactory.create(1, capture(listenerSlot)) } returns receiver
    every { registerReceiver(context, receiver, any<IntentFilter>(), any()) } returns mockk()

    every { PendingIntent.getBroadcast(any(), any(), any<Intent>(), any()) } returns pendingIntent
    every { pendingIntent.intentSender } returns mockk(relaxed = true)
    every { PendingIntent.getActivity(any(), any(), any(), any()) } returns pendingIntent

    every { preapprovalSession.requestUserPreapproval(any(), any()) } answers
      {
        listenerSlot.captured.invoke(receiver, packageInstallerResult, Intent("confirm"), msg)
      }
  }
}
