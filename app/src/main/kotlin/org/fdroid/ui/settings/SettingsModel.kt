package org.fdroid.ui.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import me.zhanghai.compose.preference.Preferences

data class SettingsModel(
  val prefsFlow: MutableStateFlow<Preferences>,
  val nextRepoUpdateFlow: Flow<Long>,
  val nextAppUpdateFlow: Flow<Long>,
  val currentAppIconFlow: StateFlow<AppIcon>,
)
