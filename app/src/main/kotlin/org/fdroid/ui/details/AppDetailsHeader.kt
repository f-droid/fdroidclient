package org.fdroid.ui.details

import android.text.format.Formatter
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.BadgedBox
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import org.fdroid.download.NetworkState
import org.fdroid.install.InstallState
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.InstalledBadge
import org.fdroid.ui.utils.MeteredConnectionDialog
import org.fdroid.ui.utils.OfflineBar
import org.fdroid.ui.utils.asRelativeTimeString
import org.fdroid.ui.utils.startActivitySafe
import org.fdroid.ui.utils.testApp

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun AppDetailsHeader(
    item: AppDetailsItem,
    innerPadding: PaddingValues,
) {
    Box {
        Spacer(modifier = Modifier.padding(top = innerPadding.calculateTopPadding()))
        item.featureGraphic?.let { featureGraphic ->
            AsyncImage(
                model = featureGraphic,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
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
                    .semantics { hideFromAccessibility() },
            )
        }
    }
    var showMeteredDialog by remember { mutableStateOf(false) }
    // Offline bar, if no internet
    if (!item.networkState.isOnline) {
        OfflineBar(modifier = Modifier.absoluteOffset(y = (-8).dp))
    }
    // Header
    val version = item.suggestedVersion ?: item.versions?.first()?.version
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp),
        horizontalArrangement = spacedBy(16.dp),
        verticalAlignment = CenterVertically,
    ) {
        BadgedBox(badge = { if (item.installedVersionCode != null) InstalledBadge() }) {
            AsyncShimmerImage(
                model = item.icon,
                contentDescription = "",
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_repo_app_default),
                modifier = Modifier
                    .size(64.dp)
                    .clip(MaterialTheme.shapes.large)
                    .semantics { hideFromAccessibility() },
            )
        }
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
    var numChecks by remember { mutableStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val state = currentInstallState
                if (state is InstallState.UserConfirmationNeeded && numChecks < 3) {
                    Log.i(
                        "AppDetailsHeader",
                        "Resumed ($numChecks). Checking user confirmation... $state"
                    )
                    // there's annoying installer bugs where it doesn't tell us about errors
                    // and we would run into infinite UI loops here, so there's a counter.
                    @Suppress("AssignedValueIsNeverRead")
                    numChecks += 1
                    item.actions.checkUserConfirmation(state)
                } else if (state is InstallState.UserConfirmationNeeded) {
                    // we tried three times, so cancel install now
                    Log.i("AppDetailsHeader", "Cancel installation")
                    item.actions.cancelInstall()
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
                    is InstallState.Waiting -> R.string.status_install_preparing
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
                        if (!cancelled) item.actions.cancelInstall()
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
                    if (item.networkState.isMetered) {
                        showMeteredDialog = true
                    } else {
                        require(item.suggestedVersion != null) {
                            "suggestedVersion was null"
                        }
                        item.actions.installAction(item.app, item.suggestedVersion, item.icon)
                    }
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
    if (showMeteredDialog) MeteredConnectionDialog(
        numBytes = version?.size,
        onConfirm = {
            require(item.suggestedVersion != null) { "suggestedVersion was null" }
            item.actions.installAction(item.app, item.suggestedVersion, item.icon)
        },
        onDismiss = { showMeteredDialog = false },
    )
}

@Preview
@Composable
fun AppDetailsHeaderPreview() {
    FDroidContent {
        Column {
            AppDetailsHeader(testApp, PaddingValues(top = 16.dp))
        }
    }
}

@Preview
@Composable
private fun PreviewProgress() {
    FDroidContent(dynamicColors = true) {
        Column {
            val app = testApp.copy(
                installState = InstallState.Starting("", "", "", 23),
                networkState = NetworkState(true, isMetered = true),
            )
            AppDetailsHeader(app, PaddingValues(top = 16.dp))
        }
    }
}
