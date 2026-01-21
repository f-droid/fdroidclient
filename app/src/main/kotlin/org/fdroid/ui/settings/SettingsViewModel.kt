package org.fdroid.ui.settings

import android.app.Application
import android.net.Uri
import android.os.Process
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import java.io.IOException
import java.lang.Runtime.getRuntime
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    app: Application,
    updatesManager: UpdatesManager,
    private val settingsManager: SettingsManager,
) : AndroidViewModel(app) {

    private val log = KotlinLogging.logger {}

    val model = SettingsModel(
        prefsFlow = settingsManager.prefsFlow,
        nextRepoUpdateFlow = updatesManager.nextRepoUpdateFlow,
        nextAppUpdateFlow = updatesManager.nextAppUpdateFlow,
    )

    init {
        viewModelScope.launch {
            // react to theme changes right away
            settingsManager.themeFlow.drop(1).collect {
                if (it != null) applyNewTheme(it)
            }
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

    fun onSaveLogcat(uri: Uri?) = viewModelScope.launch(Dispatchers.IO) {
        if (uri == null) {
            sendToast(R.string.export_log_error)
            return@launch
        }
        // support for --pid was introduced in SDK 24
        val command = "logcat -d --pid=" + Process.myPid() + " *:V"
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
        withContext(Dispatchers.Main) {
            Toast.makeText(application, s, duration).show()
        }
    }

}
