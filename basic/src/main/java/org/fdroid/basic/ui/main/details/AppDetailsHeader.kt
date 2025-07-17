package org.fdroid.basic.ui.main.details

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.fdroid.basic.R
import org.fdroid.basic.details.AppDetailsItem
import org.fdroid.basic.details.MainButtonState
import org.fdroid.basic.details.testApp
import org.fdroid.basic.ui.asRelativeTimeString
import org.fdroid.fdroid.ui.theme.FDroidContent

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun AppDetailsHeader(
    item: AppDetailsItem,
    innerPadding: PaddingValues,
) {
    var showTopSpacer by rememberSaveable(item.app.packageName) { mutableStateOf(true) }
    if (showTopSpacer) {
        Spacer(modifier = Modifier.padding(innerPadding))
    }
    item.featureGraphic?.let { featureGraphic ->
        AsyncImage(
            model = featureGraphic,
            contentDescription = "",
            contentScale = ContentScale.FillWidth,
            onSuccess = {
                showTopSpacer = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 196.dp)
                .graphicsLayer { alpha = 0.5f }
                .drawWithContent {
                    val colors = listOf(
                        Color.Black,
                        Color.Transparent
                    )
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(colors),
                        blendMode = BlendMode.DstIn
                    )
                }
                .padding(bottom = 8.dp),
            error = rememberVectorPainter(Icons.Default.Error),
        )
    }
    // Header
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp),
        horizontalArrangement = spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = item.icon,
            contentDescription = "",
            error = painterResource(R.drawable.ic_repo_app_default),
            modifier = Modifier.size(64.dp),
        )
        Column {
            SelectionContainer {
                Text(
                    item.name,
                    style = MaterialTheme.typography.headlineMediumEmphasized
                )
            }
            item.app.authorName?.let { authorName ->
                SelectionContainer {
                    Text(
                        text = "By $authorName",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            val lastUpdated = item.app.lastUpdated.asRelativeTimeString()
            val version = item.suggestedVersion ?: item.versions?.first()
            val size = version?.size?.let { size ->
                Formatter.formatFileSize(LocalContext.current, size)
            }
            SelectionContainer {
                Text(
                    text = if (size == null) {
                        "Last updated: $lastUpdated"
                    } else {
                        "Last updated: $lastUpdated ($size)"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
    // Summary
    item.summary?.let { summary ->
        SelectionContainer {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyLargeEmphasized,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
    // Repo Chooser
    RepoChooser(
        repos = item.repositories,
        currentRepoId = item.app.repoId,
        preferredRepoId = item.preferredRepoId,
        onRepoChanged = {},
        onPreferredRepoChanged = {},
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
    // Main Buttons
    if (item.showOpenButton || item.mainButtonState != MainButtonState.NONE) Row(
        horizontalArrangement = spacedBy(8.dp, CenterHorizontally),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (item.showOpenButton) {
            val context = LocalContext.current
            OutlinedButton(
                onClick = {
                    context.startActivity(item.actions.launchIntent)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.menu_open))
            }
        }
        if (item.mainButtonState != MainButtonState.NONE) {
            Button(onClick = {}, modifier = Modifier.weight(1f)) {
                if (item.mainButtonState == MainButtonState.INSTALL) {
                    Text(stringResource(R.string.menu_install))
                } else if (item.mainButtonState == MainButtonState.UPDATE) {
                    Text(stringResource(R.string.app__install_downloaded_update))
                }
            }
        }
    }
}

@Preview
@Composable
fun AppDetailsHeaderPreview() {
    FDroidContent {
        Column {
            AppDetailsHeader(testApp, PaddingValues())
        }
    }
}
