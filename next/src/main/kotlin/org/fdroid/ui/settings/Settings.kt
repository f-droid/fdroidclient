package org.fdroid.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.next.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun Settings(onSaveLogcat: (Uri?) -> Unit, onBackClicked: () -> Unit) {
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
        // TODO use implementation("me.zhanghai.compose.preference:preference:2.1.0")
        val launcher = rememberLauncherForActivityResult(CreateDocument("text/plain")) {
            onSaveLogcat(it)
        }
        val context = LocalContext.current
        Button(
            onClick = { launcher.launch(getLogName(context)) },
            modifier = Modifier
                .padding(paddingValues)
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

private fun getLogName(context: Context): String {
    val sdf = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val time = sdf.format(Date())
    return "${context.packageName}-$time.txt"
}

@Preview
@Composable
fun SettingsPreview() {
    FDroidContent {
        Settings({}) { }
    }
}
