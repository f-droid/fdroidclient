package org.fdroid.ui.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import me.zhanghai.compose.preference.Preferences

data class SettingsModel(
  val prefsFlow: MutableStateFlow<Preferences>,
  val nextRepoUpdateFlow: Flow<Long>,
  val nextAppUpdateFlow: Flow<Long>,
)
