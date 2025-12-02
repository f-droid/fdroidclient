package org.fdroid.ui.details

import android.text.format.Formatter
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import org.fdroid.R
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.install.InstallState
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.asRelativeTimeString
import org.fdroid.ui.utils.startActivitySafe
import org.fdroid.ui.utils.testApp

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
            onError = {
                showTopSpacer = true
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
                .padding(bottom = 8.dp)
                .semantics { hideFromAccessibility() },
        )
    }
    // Header
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp),
        horizontalArrangement = spacedBy(8.dp),
        verticalAlignment = CenterVertically,
    ) {
        AsyncShimmerImage(
            model = item.icon,
            contentDescription = "",
            error = painterResource(R.drawable.ic_repo_app_default),
            modifier = Modifier
                .size(64.dp)
                .semantics { hideFromAccessibility() },
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
                        text = stringResource(R.string.author_by, authorName),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            val lastUpdated = item.app.lastUpdated.asRelativeTimeString()
            val version = item.suggestedVersion ?: item.versions?.first()?.version
            val size = version?.size?.let { size ->
                Formatter.formatFileSize(LocalContext.current, size)
            }
            SelectionContainer {
                Text(
                    text = if (size == null) {
                        stringResource(R.string.last_updated, lastUpdated)
                    } else {
                        stringResource(R.string.last_updated_with_size, lastUpdated, size)
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
        proxy = item.proxy,
        onRepoChanged = item.actions.onRepoChanged,
        onPreferredRepoChanged = item.actions.onPreferredRepoChanged,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
    // check user confirmation ON_RESUME to work around Android bug
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentInstallState by rememberUpdatedState(item.installState)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val state = currentInstallState
                if (state is InstallState.UserConfirmationNeeded) {
                    Log.i("AppDetailsHeader", "Resumed. Checking user confirmation... $state")
                    item.actions.checkUserConfirmation(item.app.packageName, state)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    // Main Buttons
    val buttonLineModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
    if (item.mainButtonState == MainButtonState.PROGRESS) {
        Row(
            modifier = buttonLineModifier,
            verticalAlignment = CenterVertically,
        ) {
            Column {
                val strRes = when (item.installState) {
                    is InstallState.Starting -> R.string.status_install_preparing
                    is InstallState.PreApproved -> R.string.status_install_preparing
                    is InstallState.Downloading -> R.string.downloading
                    is InstallState.Installing -> R.string.installing
                    is InstallState.UserConfirmationNeeded -> R.string.installing
                    else -> -1
                }
                if (strRes >= 0) Text(
                    text = stringResource(strRes),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(verticalAlignment = CenterVertically) {
                    if (item.installState is InstallState.Downloading) {
                        val animatedProgress by animateFloatAsState(
                            targetValue = item.installState.progress,
                            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                        )
                        LinearWavyProgressIndicator(
                            stopSize = 0.dp,
                            progress = { animatedProgress },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        LinearWavyProgressIndicator(modifier = Modifier.weight(1f))
                    }
                    var cancelled by remember { mutableStateOf(false) }
                    IconButton(onClick = {
                        if (!cancelled) item.actions.cancelInstall(item.app.packageName)
                        cancelled = true
                    }) {
                        AnimatedVisibility(cancelled) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                        AnimatedVisibility(!cancelled) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = stringResource(R.string.cancel),
                            )
                        }
                    }
                }
            }
        }
    } else if (item.showOpenButton || item.mainButtonState != MainButtonState.NONE) Row(
        horizontalArrangement = spacedBy(8.dp, CenterHorizontally),
        modifier = buttonLineModifier,
    ) {
        if (item.showOpenButton) {
            val context = LocalContext.current
            OutlinedButton(
                onClick = {
                    context.startActivitySafe(item.actions.launchIntent)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.menu_open))
            }
        }
        if (item.mainButtonState != MainButtonState.NONE) {
            // button is for either installing or updating
            Button(
                onClick = {
                    require(item.suggestedVersion != null) {
                        "suggestedVersion was null"
                    }
                    item.actions.installAction(item.app, item.suggestedVersion, item.icon)
                },
                modifier = Modifier.weight(1f)
            ) {
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

@Preview
@Composable
private fun PreviewProgress() {
    FDroidContent {
        Column {
            val app = testApp.copy(installState = InstallState.Starting("", "", "", 23))
            AppDetailsHeader(app, PaddingValues())
        }
    }
}
