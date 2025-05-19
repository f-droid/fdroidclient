package org.fdroid.basic.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.fdroid.basic.ui.main.discover.AppNavigationItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppDetailsManager @Inject constructor() {
    private val _appDetails = MutableStateFlow<AppNavigationItem?>(null)
    val appDetails = _appDetails.asStateFlow()

    fun setAppDetails(app: AppNavigationItem?) {
        _appDetails.value = app
    }
}
