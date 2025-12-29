package org.fdroid.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import org.fdroid.settings.SettingsManager
import org.fdroid.updates.UpdatesManager
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsManager: SettingsManager,
    updatesManager: UpdatesManager,
) : ViewModel() {
    val dynamicColors = settingsManager.dynamicColorFlow
    val numUpdates = updatesManager.numUpdates
    val hasAppIssues = updatesManager.appsWithIssues.map { !it.isNullOrEmpty() }
}
