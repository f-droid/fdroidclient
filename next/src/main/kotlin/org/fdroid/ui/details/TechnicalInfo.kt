package org.fdroid.ui.details

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.ui.utils.testApp

@Composable
fun TechnicalInfo(item: AppDetailsItem) {
    val items = mutableMapOf(
        stringResource(R.string.package_name) to item.app.packageName
    )
    if (item.installedVersionCode != null) {
        items[stringResource(R.string.installed_version)] =
            "${item.installedVersionName} (${item.installedVersionCode})"
    }
    Column(
        verticalArrangement = spacedBy(4.dp),
        modifier = Modifier
            .padding(start = 16.dp, bottom = 16.dp),
    ) {
        items.forEach { (name, content) ->
            Row(horizontalArrangement = spacedBy(2.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium
                )
                SelectionContainer {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    FDroidContent {
        TechnicalInfo(testApp)
    }
}
