package org.fdroid.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor() {

    // TODO read this value from persistent storage
    private val _showFilterOnboarding = MutableStateFlow(true)
    val showFilterOnboarding = _showFilterOnboarding.asStateFlow()

    fun onFilterOnboardingSeen() {
        _showFilterOnboarding.value = false
        // TODO persist
    }

}
