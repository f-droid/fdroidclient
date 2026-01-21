package org.fdroid.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.provider.Settings.ACTION_APP_LOCALE_SETTINGS
import android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
import android.provider.Settings.EXTRA_APP_PACKAGE
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SystemSecurityUpdate
import androidx.compose.material.icons.filled.SystemSecurityUpdateWarning
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.UpdateDisabled
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import me.zhanghai.compose.preference.MapPreferences
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.listPreference
import me.zhanghai.compose.preference.preference
import me.zhanghai.compose.preference.preferenceCategory
import me.zhanghai.compose.preference.rememberPreferenceState
import me.zhanghai.compose.preference.switchPreference
import org.fdroid.R
import org.fdroid.settings.SettingsConstants.AutoUpdateValues
import org.fdroid.settings.SettingsConstants.AutoUpdateValues.Always
import org.fdroid.settings.SettingsConstants.AutoUpdateValues.Never
import org.fdroid.settings.SettingsConstants.AutoUpdateValues.OnlyWifi
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_AUTO_UPDATES
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_DYNAMIC_COLORS
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_PROXY
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_REPO_UPDATES
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_THEME
import org.fdroid.settings.SettingsConstants.PREF_KEY_AUTO_UPDATES
import org.fdroid.settings.SettingsConstants.PREF_KEY_DYNAMIC_COLORS
import org.fdroid.settings.SettingsConstants.PREF_KEY_PROXY
import org.fdroid.settings.SettingsConstants.PREF_KEY_REPO_UPDATES
import org.fdroid.settings.SettingsConstants.PREF_KEY_THEME
import org.fdroid.settings.toAutoUpdateValue
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.asRelativeTimeString
import org.fdroid.ui.utils.startActivitySafe
import org.fdroid.utils.getLogName
import java.lang.System.currentTimeMillis
import java.util.concurrent.TimeUnit.HOURS

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun Settings(
    model: SettingsModel,
    onSaveLogcat: (Uri?) -> Unit,
    onBackClicked: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                title = {
                    Text(stringResource(R.string.menu_settings))
                },
            )
        },
    ) { paddingValues ->
        val launcher = rememberLauncherForActivityResult(CreateDocument("text/plain")) {
            onSaveLogcat(it)
        }
        val context = LocalContext.current
        val res = LocalResources.current
        ProvidePreferenceLocals(model.prefsFlow) {
            val showProxyError = remember { mutableStateOf(false) }
            val proxyState = rememberPreferenceState(PREF_KEY_PROXY, PREF_DEFAULT_PROXY)
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                preferenceCategory(
                    key = "pref_category_display",
                    title = { Text(stringResource(R.string.display)) },
                )
                val themeToString = { value: String ->
                    AnnotatedString(
                        when (value) {
                            "light" -> res.getString(R.string.theme_light)
                            "dark" -> res.getString(R.string.theme_dark)
                            "followSystem" -> res.getString(R.string.theme_follow_system)
                            else -> error("Unknown value: $value")
                        }
                    )
                }
                listPreference(
                    key = PREF_KEY_THEME,
                    values = listOf(
                        "light",
                        "dark",
                        "followSystem",
                    ),
                    valueToText = themeToString,
                    defaultValue = PREF_DEFAULT_THEME,
                    title = { Text(text = stringResource(R.string.theme)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.BrightnessMedium,
                            contentDescription = null,
                            modifier = Modifier.semantics { hideFromAccessibility() },
                        )
                    },
                    summary = { Text(text = "${themeToString(it)}") },
                )
                if (SDK_INT >= 31) switchPreference(
                    key = PREF_KEY_DYNAMIC_COLORS,
                    defaultValue = PREF_DEFAULT_DYNAMIC_COLORS,
                    title = {
                        Text(stringResource(R.string.pref_dyn_colors_title))
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ColorLens,
                            contentDescription = null,
                            modifier = Modifier.semantics { hideFromAccessibility() },
                        )
                    },
                    summary = {
                        Text(text = stringResource(R.string.pref_dyn_colors_summary))
                    },
                )
                if (SDK_INT >= 33) preference(
                    key = "languages",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            modifier = Modifier.semantics { hideFromAccessibility() },
                        )
                    },
                    title = { Text(stringResource(R.string.pref_language)) },
                    summary = { Text(stringResource(R.string.pref_language_summary)) },
                    onClick = {
                        val intent = Intent(ACTION_APP_LOCALE_SETTINGS).apply {
                            setData(Uri.fromParts("package", context.packageName, null))
                        }
                        context.startActivitySafe(intent)
                    },
                )
                if (SDK_INT >= 26) preference(
                    key = "notifications",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.semantics { hideFromAccessibility() },
                        )
                    },
                    title = { Text(stringResource(R.string.notification_title)) },
                    summary = { Text(stringResource(R.string.notification_summary)) },
                    onClick = {
                        val intent = Intent(ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivitySafe(intent)
                    },
                )
                preferenceCategory(
                    key = "pref_category_updates",
                    title = { Text(stringResource(R.string.updates)) },
                )
                listPreference(
                    key = PREF_KEY_REPO_UPDATES,
                    defaultValue = PREF_DEFAULT_REPO_UPDATES,
                    title = {
                        Text(stringResource(R.string.pref_repo_updates_title))
                    },
                    icon = { strValue ->
                        if (strValue != Never.name) Icon(
                            imageVector = Icons.Default.SystemSecurityUpdate,
                            contentDescription = null,
                            modifier = Modifier.semantics { hideFromAccessibility() },
                        ) else Icon(
                            imageVector = Icons.Default.SystemSecurityUpdateWarning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { hideFromAccessibility() },
                        )
                    },
                    summary = { strValue ->
                        if (strValue != Never.name) {
                            val nextUpdate =
                                model.nextRepoUpdateFlow.collectAsState(Long.MAX_VALUE).value
                            val nextUpdateStr = if (nextUpdate == Long.MAX_VALUE) {
                                stringResource(
                                    R.string.auto_update_time,
                                    stringResource(R.string.repositories_last_update_never)
                                )
                            } else if (nextUpdate - currentTimeMillis() <= 0) {
                                stringResource(R.string.auto_update_time_past)
                            } else {
                                stringResource(
                                    R.string.auto_update_time,
                                    nextUpdate.asRelativeTimeString()
                                )
                            }
                            val s = if (strValue == OnlyWifi.name) {
                                stringResource(R.string.pref_repo_updates_summary_only_wifi)
                            } else if (strValue == Always.name) {
                                stringResource(R.string.pref_repo_updates_summary_always)
                            } else error("Unknown value: $strValue")
                            Text(s + "\n" + nextUpdateStr)
                        } else {
                            Text(
                                text = stringResource(R.string.pref_repo_updates_summary_never),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                    values = AutoUpdateValues.entries.map { it.name },
                    valueToText = { value: String ->
                        AnnotatedString(
                            when (value.toAutoUpdateValue()) {
                                OnlyWifi -> res.getString(R.string.pref_auto_updates_only_wifi)
                                Always -> res.getString(R.string.pref_auto_updates_only_always)
                                Never -> res.getString(R.string.pref_auto_updates_only_never)
                            }
                        )
                    },
                )
                listPreference(
                    key = PREF_KEY_AUTO_UPDATES,
                    defaultValue = PREF_DEFAULT_AUTO_UPDATES,
                    title = {
                        Text(stringResource(R.string.update_auto_install))
                    },
                    icon = { strValue ->
                        Icon(
                            imageVector = if (strValue != Never.name) {
                                Icons.Default.Update
                            } else {
                                Icons.Default.UpdateDisabled
                            },
                            contentDescription = null,
                            modifier = Modifier.semantics { hideFromAccessibility() },
                        )
                    },
                    summary = { strValue ->
                        val s = if (strValue != Never.name) {
                            val nextUpdate =
                                model.nextAppUpdateFlow.collectAsState(Long.MAX_VALUE).value
                            val nextUpdateStr = if (nextUpdate == Long.MAX_VALUE) {
                                stringResource(
                                    R.string.auto_update_time,
                                    stringResource(R.string.repositories_last_update_never)
                                )
                            } else if (nextUpdate - currentTimeMillis() <= 0) {
                                stringResource(R.string.auto_update_time_past)
                            } else {
                                stringResource(
                                    R.string.auto_update_time,
                                    nextUpdate.asRelativeTimeString()
                                )
                            }
                            val s = if (strValue == OnlyWifi.name) {
                                stringResource(R.string.pref_auto_updates_summary_only_wifi)
                            } else if (strValue == Always.name) {
                                stringResource(R.string.pref_auto_updates_summary_always)
                            } else error("Unknown value: $strValue")
                            s + "\n" + nextUpdateStr
                        } else {
                            stringResource(R.string.pref_auto_updates_summary_never)
                        }
                        Text(s)
                    },
                    values = AutoUpdateValues.entries.map { it.name },
                    valueToText = { value: String ->
                        AnnotatedString(
                            when (value.toAutoUpdateValue()) {
                                OnlyWifi -> res.getString(R.string.pref_auto_updates_only_wifi)
                                Always -> res.getString(R.string.pref_auto_updates_only_always)
                                Never -> res.getString(R.string.pref_auto_updates_only_never)
                            }
                        )
                    },
                )
                preferenceCategory(
                    key = "pref_category_network",
                    title = { Text(stringResource(R.string.pref_category_network)) },
                )
                preferenceProxy(proxyState, showProxyError)
                item {
                    OutlinedButton(
                        onClick = { launcher.launch("${getLogName(context)}.txt") },
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.Save, null)
                        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                        Text(
                            text = stringResource(R.string.pref_export_log_title),
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun SettingsPreview() {
    FDroidContent {
        val model = SettingsModel(
            prefsFlow = MutableStateFlow(MapPreferences()),
            nextRepoUpdateFlow = MutableStateFlow(Long.MAX_VALUE),
            nextAppUpdateFlow = MutableStateFlow(currentTimeMillis() - HOURS.toMillis(12)),
        )
        Settings(model, {}, { })
    }
}
