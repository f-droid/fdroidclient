package org.fdroid.install

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.EXTRA_PACKAGE_NAME
import android.content.pm.PackageInstaller.EXTRA_SESSION_ID
import android.content.pm.PackageInstaller.SessionParams
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.icu.util.ULocale
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
import androidx.core.content.ContextCompat.registerReceiver
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import mu.KotlinLogging
import org.fdroid.LocaleChooser.getBestLocale
import org.fdroid.database.AppMetadata
import org.fdroid.database.AppVersion
import org.fdroid.ui.utils.isAppInForeground
import org.fdroid.utils.IoDispatcher

@Singleton
class SessionInstallManager
@Inject
constructor(
  @param:ApplicationContext private val context: Context,
  @param:IoDispatcher private val coroutineScope: CoroutineScope,
) {

  private val log = KotlinLogging.logger {}
  private val installer = context.packageManager.packageInstaller

  companion object {
    private const val ACTION_INSTALL = "org.fdroid.install.SessionInstallManager.install"

    /**
     * If this returns true, we can use [SessionParams.setRequireUserAction] with false, thus
     * updating the app with the given targetSdk without user action.
     */
    fun isAutoUpdateSupported(targetSdk: Int): Boolean {
      if (SDK_INT < 31) return false // not supported below Android 12

      if (SDK_INT == 31 && targetSdk >= 29) return true
      if (SDK_INT == 32 && targetSdk >= 29) return true
      if (SDK_INT == 33 && targetSdk >= 30) return true
      if (SDK_INT == 34 && targetSdk >= 31) return true
      if (SDK_INT == 35 && targetSdk >= 33) return true
      // This needs to be adjusted as new Android versions are released
      // https://developer.android.com/reference/android/content/pm/PackageInstaller.SessionParams#setRequireUserAction(int)
      // https://cs.android.com/android/platform/superproject/+/android-16.0.0_r2:frameworks/base/services/core/java/com/android/server/pm/PackageInstallerSession.java;l=329;drc=73caa0299d9196ddeefe4f659f557fb880f6536d
      // current code requires targetSdk 34 on SDK 36+
      return SDK_INT >= 36 && targetSdk >= 34
    }
  }

  init {
    // abandon old sessions, because there's a limit
    // that will throw IllegalStateException when we try to open new sessions
    coroutineScope.launch {
      for (session in installer.mySessions) {
        log.debug { "Abandon session ${session.sessionId} for ${session.appPackageName}" }
        try {
          installer.abandonSession(session.sessionId)
        } catch (e: SecurityException) {
          log.error(e) { "Error abandoning session: " }
        }
      }
    }
  }

  /** Requests installation pre-approval (if available on this device). */
  suspend fun requestPreapproval(
    app: AppMetadata,
    iconGetter: suspend () -> Bitmap?,
    isUpdate: Boolean,
    version: AppVersion,
    canRequestUserConfirmationNow: Boolean,
  ): PreApprovalResult {
    return if (!context.isAppInForeground()) {
      log.info { "App not in foreground, pre-approval for ${app.packageName} not supported." }
      PreApprovalResult.NotSupported
    } else if (isUpdate && canDoAutoUpdate(version)) {
      // should not be needed, so we say not supported
      log.info { "Can do auto-update pre-approval for ${app.packageName} not needed." }
      PreApprovalResult.NotSupported
    } else if (SDK_INT >= 34) {
      log.info { "Requesting pre-approval for ${app.packageName}..." }
      try {
        val icon = iconGetter()
        preapproval(app, icon, canRequestUserConfirmationNow)
      } catch (e: Exception) {
        log.error(e) { "Error requesting pre-approval for ${app.packageName}: " }
        PreApprovalResult.Error("${e::class.java.simpleName} ${e.message}")
      }
    } else {
      PreApprovalResult.NotSupported
    }
  }

  @RequiresApi(34)
  private suspend fun preapproval(
    app: AppMetadata,
    icon: Bitmap?,
    canRequestUserConfirmationNow: Boolean,
  ): PreApprovalResult = suspendCancellableCoroutine { cont ->
    val params = getSessionParams(app.packageName)
    val sessionId = installer.createSession(params)
    log.info { "Opened session $sessionId for ${app.packageName}" }
    val name = app.name.getBestLocale(LocaleListCompat.getDefault()) ?: ""

    val receiver =
      InstallBroadcastReceiver(sessionId) { status, intent, msg ->
        when (status) {
          PackageInstaller.STATUS_SUCCESS -> {
            cont.resume(PreApprovalResult.Success(sessionId))
            context.unregisterReceiver(this)
          }
          PackageInstaller.STATUS_PENDING_USER_ACTION -> {
            val flags = FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getActivity(context, sessionId, intent, flags)
            // There should be no bugs on Android versions where this is supported,
            // and we should be in the foreground right now,
            // so fire up intent here and now.
            if (canRequestUserConfirmationNow) {
              log.info { "Sending pre-approval intent for ${app.packageName}: $intent" }
              pendingIntent.send()
            } else {
              log.info { "Can not ask pre-approval for ${app.packageName}: $intent" }
              val s = PreApprovalResult.UserConfirmationRequired(sessionId, pendingIntent)
              cont.resume(s)
              context.unregisterReceiver(this)
            }
          }
          else -> {
            val result =
              when (status) {
                PackageInstaller.STATUS_FAILURE_ABORTED -> PreApprovalResult.UserAborted
                PackageInstaller.STATUS_FAILURE_BLOCKED -> PreApprovalResult.NotSupported
                else -> PreApprovalResult.Error(msg)
              }
            cont.resume(result)
            context.unregisterReceiver(this)
          }
        }
      }
    registerReceiver(context, receiver, IntentFilter(ACTION_INSTALL), RECEIVER_NOT_EXPORTED)
    cont.invokeOnCancellation {
      log.info { "Pre-approval for ${app.packageName} cancelled." }
      context.unregisterReceiver(receiver)
    }

    installer.openSession(sessionId).use { session ->
      log.info { "app name locales: ${app.name} using: ${ULocale.getDefault()}" }
      val details =
        PackageInstaller.PreapprovalDetails.Builder()
          .setPackageName(app.packageName)
          .setLabel(name)
          .setLocale(ULocale.getDefault()) // TODO get the real one used for label
          .apply { if (icon != null) setIcon(icon) }
          .build()
      val sender = getInstallIntentSender(sessionId, app.packageName)
      session.requestUserPreapproval(details, sender)
    }
  }

  @WorkerThread
  @SuppressLint("RequestInstallPackagesPolicy")
  suspend fun install(
    sessionId: Int?,
    packageName: String,
    state: InstallStateWithInfo,
    apkFile: File,
  ): InstallState = suspendCancellableCoroutine { cont ->
    val size = apkFile.length()
    log.info { "Installing ${apkFile.name} with size $size bytes" }

    val sessionId =
      try {
        if (sessionId == null) {
          val params = getSessionParams(packageName, size)
          installer.createSession(params)
        } else {
          sessionId
        }
      } catch (e: Exception) {
        log.error(e) { "Error when creating session: " }
        val s = InstallState.Error("${e::class.java.simpleName} ${e.message}", state)
        cont.resume(s)
        return@suspendCancellableCoroutine
      }
    // set-up receiver for install result
    val receiver =
      InstallBroadcastReceiver(sessionId) { status, intent, msg ->
        context.unregisterReceiver(this)
        when (status) {
          PackageInstaller.STATUS_SUCCESS -> {
            val newState =
              InstallState.Installed(
                name = state.name,
                versionName = state.versionName,
                currentVersionName = state.currentVersionName,
                lastUpdated = state.lastUpdated,
                iconModel = state.iconModel,
              )
            cont.resume(newState)
          }
          PackageInstaller.STATUS_PENDING_USER_ACTION -> {
            val flags =
              if (SDK_INT >= 31) {
                FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
              } else {
                FLAG_UPDATE_CURRENT
              }
            val pendingIntent = PendingIntent.getActivity(context, sessionId, intent, flags)
            val progress =
              installer.getSessionInfo(sessionId)?.progress
                ?: error("No session info for $sessionId")
            cont.resume(
              InstallState.UserConfirmationNeeded(
                state = state,
                sessionId = sessionId,
                intent = pendingIntent,
                progress = progress,
              )
            )
          }
          else -> {
            if (status == PackageInstaller.STATUS_FAILURE_ABORTED) {
              cont.resume(InstallState.UserAborted)
            } else if (
              status == PackageInstaller.STATUS_FAILURE &&
                msg != null &&
                msg.contains("PreapprovalDetails")
            ) {
              val newState =
                InstallState.PreApproved(
                  name = state.name,
                  versionName = state.versionName,
                  currentVersionName = state.currentVersionName,
                  lastUpdated = state.lastUpdated,
                  iconModel = state.iconModel,
                  result = PreApprovalResult.Error(msg),
                )
              cont.resume(newState)
            } else {
              cont.resume(InstallState.Error(msg, state))
            }
          }
        }
      }
    registerReceiver(context, receiver, IntentFilter(ACTION_INSTALL), RECEIVER_NOT_EXPORTED)
    cont.invokeOnCancellation {
      log.info { "App installation was cancelled, unregistering broadcast receiver..." }
      context.unregisterReceiver(receiver)
      try {
        installer.abandonSession(sessionId)
      } catch (e: SecurityException) {
        // this can happen if the cancellation came too late and session already concluded
        log.warn(e) { "Error while abandoning session: " }
      }
    }
    // do the actual installation
    try {
      installer.openSession(sessionId).use { session ->
        apkFile.inputStream().use { inputStream ->
          session.openWrite(packageName, 0, size).use { outputStream ->
            inputStream.copyTo(outputStream)
            session.fsync(outputStream)
          }
        }
        val sender = getInstallIntentSender(sessionId, packageName)
        log.info { "Committing session..." }
        session.commit(sender)
      }
    } catch (e: Exception) {
      log.error(e) { "Error during install session: " }
      cont.resume(InstallState.Error("${e::class.java.simpleName} ${e.message}", state))
    }
  }

  suspend fun requestUserConfirmation(state: InstallConfirmationState): InstallState =
    suspendCancellableCoroutine { cont ->
      val isPreApproval = state is InstallState.PreApprovalConfirmationNeeded
      val receiver =
        InstallBroadcastReceiver(state.sessionId) { status, _, msg ->
          context.unregisterReceiver(this)
          when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
              val newState =
                if (isPreApproval)
                  InstallState.PreApproved(
                    name = state.name,
                    versionName = state.versionName,
                    currentVersionName = state.currentVersionName,
                    lastUpdated = state.lastUpdated,
                    iconModel = state.iconModel,
                    result = PreApprovalResult.Success(state.sessionId),
                  )
                else
                  InstallState.Installed(
                    name = state.name,
                    versionName = state.versionName,
                    currentVersionName = state.currentVersionName,
                    lastUpdated = state.lastUpdated,
                    iconModel = state.iconModel,
                  )
              cont.resume(newState)
            }
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
              error("Got STATUS_PENDING_USER_ACTION again")
            }
            else -> {
              if (status == PackageInstaller.STATUS_FAILURE_ABORTED) {
                cont.resume(InstallState.UserAborted)
              } else {
                cont.resume(InstallState.Error(msg, state))
              }
            }
          }
        }
      registerReceiver(context, receiver, IntentFilter(ACTION_INSTALL), RECEIVER_NOT_EXPORTED)
      cont.invokeOnCancellation { context.unregisterReceiver(receiver) }
      state.intent.send()
    }

  private fun getSessionParams(packageName: String, size: Long? = null): SessionParams {
    val params = SessionParams(SessionParams.MODE_FULL_INSTALL)
    params.setAppPackageName(packageName)
    size?.let { params.setSize(it) }
    params.setInstallLocation(PackageInfo.INSTALL_LOCATION_AUTO)
    if (SDK_INT >= 26) {
      params.setInstallReason(PackageManager.INSTALL_REASON_USER)
    }
    if (SDK_INT >= 31) {
      params.setRequireUserAction(SessionParams.USER_ACTION_NOT_REQUIRED)
    }
    if (SDK_INT >= 33) {
      params.setPackageSource(PackageInstaller.PACKAGE_SOURCE_STORE)
    }
    if (SDK_INT >= 34) {
      // Once the update ownership enforcement is enabled,
      // the other installers will need the user action to update the package
      // even if the installers have been granted the INSTALL_PACKAGES permission.
      // The update ownership enforcement can only be enabled on initial installation.
      // Set this to true on package update is a no-op.
      params.setRequestUpdateOwnership(true)
    }
    return params
  }

  private fun canDoAutoUpdate(version: AppVersion): Boolean {
    if (SDK_INT < 31) return false
    val targetSdkVersion = version.manifest.targetSdkVersion ?: return false
    // docs:
    // https://developer.android.com/reference/android/content/pm/PackageInstaller.SessionParams#setRequireUserAction(int)
    return if (isAutoUpdateSupported(targetSdkVersion)) {
      val ourPackageName = context.packageName
      if (ourPackageName == version.packageName) return true
      val sourceInfo =
        try {
          context.packageManager.getInstallSourceInfo(version.packageName)
        } catch (e: Exception) {
          log.error(e) { "Could not get package info: " }
          return false
        }
      if (SDK_INT >= 34 && sourceInfo.updateOwnerPackageName == ourPackageName) {
        true
      } else if (sourceInfo.installingPackageName == ourPackageName) {
        true
      } else {
        false
      }
    } else {
      false
    }
  }

  private fun getInstallIntentSender(sessionId: Int, packageName: String): IntentSender {
    // Don't use a different action for preapproval and installation,
    // because Android sometimes sends installation broadcasts to preapproval intent.
    val broadcastIntent =
      Intent(ACTION_INSTALL).apply {
        setPackage(context.packageName)
        putExtra(EXTRA_SESSION_ID, sessionId)
        putExtra(EXTRA_PACKAGE_NAME, packageName)
        addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
      }
    // intent flag needs to be mutable, otherwise the intent has no extras
    val flags = if (SDK_INT >= 31) FLAG_UPDATE_CURRENT or FLAG_MUTABLE else FLAG_UPDATE_CURRENT
    val pendingIntent = PendingIntent.getBroadcast(context, sessionId, broadcastIntent, flags)
    return pendingIntent.intentSender
  }
}
