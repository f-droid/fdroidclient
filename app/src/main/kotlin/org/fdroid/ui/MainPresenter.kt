package org.fdroid.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_DYNAMIC_COLORS
import org.fdroid.ui.apps.AppWithIssueItem

data class MainModel(
  val dynamicColors: Boolean,
  val numUpdates: Int,
  val hasAppIssues: Boolean,
)

@Composable
fun MainPresenter(
  dynamicColorsFlow: Flow<Boolean>,
  numUpdatesFlow: StateFlow<Int>,
  appsWithIssuesFlow: StateFlow<List<AppWithIssueItem>?>,
): MainModel {
  return MainModel(
    dynamicColors = dynamicColorsFlow.collectAsState(initial = PREF_DEFAULT_DYNAMIC_COLORS).value,
    numUpdates = numUpdatesFlow.collectAsState().value,
    hasAppIssues = !appsWithIssuesFlow.collectAsState().value.isNullOrEmpty(),
  )
}
