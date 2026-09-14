package org.fdroid.ui.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Process
import android.os.Process.myUid
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.lang.Runtime.getRuntime
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.fdroid.R
import org.fdroid.repo.RepoUpdateWorker
import org.fdroid.settings.SettingsManager
import org.fdroid.ui.utils.applyNewTheme
import org.fdroid.updates.AppUpdateWorker
import org.fdroid.updates.UpdatesManager

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
  app: Application,
  updatesManager: UpdatesManager,
  private val settingsManager: SettingsManager,
  private val appIconManager: AppIconManager,
) : AndroidViewModel(app) {

  private val log = KotlinLogging.logger {}

  val model =
    SettingsModel(
      prefsFlow = settingsManager.prefsFlow,
      nextRepoUpdateFlow = updatesManager.nextRepoUpdateFlow,
      nextAppUpdateFlow = updatesManager.nextAppUpdateFlow,
      currentAppIconFlow = appIconManager.currentAppIcon,
    )

  init {
    viewModelScope.launch {
      // react to theme changes right away
      settingsManager.themeFlow.drop(1).collect { if (it != null) applyNewTheme(it) }
    }
    viewModelScope.launch {
      // react to repo auto update changes
      settingsManager.repoUpdatesFlow.drop(1).collect { value ->
        RepoUpdateWorker.scheduleOrCancel(application, value)
      }
    }
    viewModelScope.launch {
      // react to app auto update changes
      settingsManager.autoUpdateAppsFlow.drop(1).collect { value ->
        AppUpdateWorker.scheduleOrCancel(application, value)
      }
    }
  }

  fun onChangeAppIcon(appIcon: AppIcon) {
    appIconManager.setAppIcon(appIcon)

    // On Android SDK versions below 26, changing the app icon may require a restart to take effect
    if (SDK_INT < 26) {
      val packageManager = application.packageManager
      val intent = packageManager.getLaunchIntentForPackage(application.packageName)
      val componentName = intent?.component
      val mainIntent = Intent.makeRestartActivityTask(componentName)
      application.startActivity(mainIntent)
      getRuntime().exit(0)
    }
  }

  fun onSaveLogcat(uri: Uri?) =
    viewModelScope.launch(Dispatchers.IO) {
      if (uri == null) {
        sendToast(R.string.export_log_error)
        return@launch
      }
      val command =
        if (SDK_INT < 30) {
          // support for --pid was introduced in SDK 24
          "logcat -d --pid=${Process.myPid()} *:V"
        } else {
          // support for --uid was introduced in SDK 30 and is better,
          // because it gives logs before process death
          "logcat -d --uid=${myUid()} *:V"
        }
      try {
        application.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
          getRuntime().exec(command).inputStream.use { inputStream ->
            // first log command, so we see if it is correct, e.g. has our own pid
            outputStream.write("$command\n\n".toByteArray())
            inputStream.copyTo(outputStream)
          }
        } ?: throw IOException("OutputStream was null")
        sendToast(R.string.export_log_success)
      } catch (e: Exception) {
        log.error(e) { "Error saving logcat " }
        sendToast(R.string.export_log_error)
      }
    }

  private suspend fun sendToast(@StringRes s: Int, duration: Int = LENGTH_SHORT) {
    withContext(Dispatchers.Main) { Toast.makeText(application, s, duration).show() }
  }
}
