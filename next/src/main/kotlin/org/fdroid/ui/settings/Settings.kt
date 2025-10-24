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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import me.zhanghai.compose.preference.MapPreferences
import me.zhanghai.compose.preference.Preferences
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.listPreference
import me.zhanghai.compose.preference.preference
import me.zhanghai.compose.preference.preferenceCategory
import org.fdroid.R
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_THEME
import org.fdroid.settings.SettingsConstants.PREF_KEY_THEME
import org.fdroid.utils.getLogName

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun Settings(
    prefsFlow: MutableStateFlow<Preferences>,
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
        ProvidePreferenceLocals(prefsFlow) {
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
                        )
                    },
                    summary = { Text(text = "${themeToString(it)}") },
                )
                if (SDK_INT >= 26) preference(
                    key = "notifications",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                        )
                    },
                    title = { Text(stringResource(R.string.notification_title)) },
                    summary = { Text(stringResource(R.string.notification_summary)) },
                    onClick = {
                        val intent = Intent(ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    },
                )
                if (SDK_INT >= 33) preference(
                    key = "languages",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                        )
                    },
                    title = { Text(stringResource(R.string.pref_language)) },
                    summary = { Text(stringResource(R.string.pref_language_summary)) },
                    onClick = {
                        val intent = Intent(ACTION_APP_LOCALE_SETTINGS).apply {
                            setData(Uri.fromParts("package", context.packageName, null))
                        }
                        context.startActivity(intent)
                    },
                )
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
        Settings(MutableStateFlow(MapPreferences()), {}, { })
    }
}
