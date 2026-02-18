package org.fdroid.ui.panic

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import info.guardianproject.panic.Panic
import kotlinx.coroutines.flow.MutableStateFlow
import me.zhanghai.compose.preference.MapPreferences
import me.zhanghai.compose.preference.Preferences
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.listPreference
import me.zhanghai.compose.preference.preferenceCategory
import me.zhanghai.compose.preference.switchPreference
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.BackButton

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PanicSettings(
    prefsFlow: MutableStateFlow<Preferences>,
    state: PanicSettingsState,
    onBackClicked: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    BackButton(onClick = onBackClicked)
                },
                title = {
                    Text(stringResource(R.string.panic_settings))
                },
            )
        },
    ) { paddingValues ->
        ProvidePreferenceLocals(prefsFlow) {
            val res = LocalResources.current
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                switchPreference(
                    key = "pref_panic_exit",
                    defaultValue = true,
                    title = { Text(stringResource(R.string.panic_exit_title)) },
                    summary = { Text(stringResource(R.string.panic_exit_summary)) },
                )
                preferenceCategory(
                    key = "pref_panic_destructive_actions",
                    title = { Text(stringResource(R.string.panic_destructive_actions)) },
                )
                listPreference(
                    key = "pref_panic_app",
                    defaultValue = null,
                    icon = {
                        if (state.selectedPanicApp == null) Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = null,
                            modifier = Modifier.semantics { hideFromAccessibility() },
                        ) else AsyncShimmerImage(
                            model = state.selectedPanicApp.iconModel,
                            error = painterResource(R.drawable.ic_repo_app_default),
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .semantics { hideFromAccessibility() },
                        )
                    },
                    title = { Text(stringResource(R.string.panic_app_setting_title)) },
                    summary = {
                        if (state.selectedPanicApp == null) {
                            Text(stringResource(R.string.panic_app_setting_summary))
                        } else {
                            Text(state.selectedPanicApp.name)
                        }
                    },
                    values = state.panicApps.map { it?.packageName },
                    valueToText = { v ->
                        val noApp = res.getString(R.string.panic_app_setting_none)
                        val s = state.panicApps.find { v == it?.packageName }?.name ?: noApp
                        AnnotatedString(s)
                    }
                )
                switchPreference(
                    key = "pref_panic_reset_repos",
                    defaultValue = false,
                    enabled = { state.actionsEnabled },
                    title = { Text(stringResource(R.string.panic_reset_repos_title)) },
                    summary = { Text(stringResource(R.string.panic_reset_repos_summary)) },
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    FDroidContent {
        val noApp = PanicApp(
            packageName = Panic.PACKAGE_NAME_NONE,
            name = LocalResources.current.getString(R.string.panic_app_setting_none),
        )
        val state = PanicSettingsState(
            panicApps = listOf(noApp),
            selectedPanicApp = noApp,
        )
        PanicSettings(MutableStateFlow(MapPreferences()), state, {})
    }
}
