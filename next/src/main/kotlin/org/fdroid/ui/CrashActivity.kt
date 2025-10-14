package org.fdroid.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.acra.dialog.CrashReportDialogHelper
import org.fdroid.R
import org.fdroid.fdroid.ui.theme.FDroidContent

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val helper = CrashReportDialogHelper(this, intent)
        setContent {
            FDroidContent {
                CrashContent(
                    onCancel = {
                        helper.cancelReports()
                        finishAfterTransition()
                    },
                    onSend = { comment, userEmail ->
                        helper.sendCrash(comment, userEmail)
                        finishAfterTransition()
                    },
                    modifier = Modifier.safeContentPadding()
                )
            }
        }
    }
}

@Composable
private fun CrashContent(
    onCancel: () -> Unit,
    onSend: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_crash),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            contentDescription = null, // decorative element
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .aspectRatio(1f)
                .padding(vertical = 16.dp)
        )
        val textFieldState = rememberTextFieldState()
        Column(verticalArrangement = spacedBy(16.dp)) {
            Text(
                text = stringResource(R.string.crash_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                text = stringResource(R.string.crash_report_text),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
            )
            TextField(
                state = textFieldState,
                placeholder = { Text(stringResource(R.string.crash_report_comment_hint)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            OutlinedButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
            Button(onClick = {
                onSend(textFieldState.text.toString(), "")
            }) {
                Text(stringResource(R.string.crash_report_button_send))
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    FDroidContent {
        CrashContent({}, { _, _ -> })
    }
}
