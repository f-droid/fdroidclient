package org.fdroid.settings

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private companion object {
        const val KEY_FILTER = "appFilter"
        const val KEY_REPO_LIST = "repoList"
        const val KEY_REPO_DETAILS = "repoDetails"
    }

    private val prefs = context.getSharedPreferences("onboarding", MODE_PRIVATE)

    private val _showFilterOnboarding = Onboarding(KEY_FILTER, prefs)
    val showFilterOnboarding = _showFilterOnboarding.flow

    private val _showRepositoriesOnboarding = Onboarding(KEY_REPO_LIST, prefs)
    val showRepositoriesOnboarding = _showRepositoriesOnboarding.flow

    private val _showRepoDetailsOnboarding = Onboarding(KEY_REPO_DETAILS, prefs)
    val showRepoDetailsOnboarding = _showRepoDetailsOnboarding.flow

    fun onFilterOnboardingSeen() {
        _showFilterOnboarding.onSeen(prefs)
    }

    fun onRepositoriesOnboardingSeen() {
        _showRepositoriesOnboarding.onSeen(prefs)
    }

    fun onRepoDetailsOnboardingSeen() {
        _showRepoDetailsOnboarding.onSeen(prefs)
    }
}

private data class Onboarding(
    val key: String,
    private val _flow: MutableStateFlow<Boolean>,
) {
    constructor(key: String, prefs: SharedPreferences) : this(
        key = key,
        _flow = MutableStateFlow(prefs.getBoolean(key, true)),
    )

    val flow: StateFlow<Boolean> = _flow.asStateFlow()

    fun onSeen(prefs: SharedPreferences) {
        _flow.update { false }
        prefs.edit {
            putBoolean(key, false)
        }
    }
}
