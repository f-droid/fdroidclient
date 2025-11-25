package org.fdroid.ui.details

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.fdroid.R
import org.fdroid.database.AppVersion
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.ui.utils.ExpandIconChevron
import org.fdroid.ui.utils.ExpandableSection
import org.fdroid.ui.utils.FDroidOutlineButton
import org.fdroid.ui.utils.asRelativeTimeString
import org.fdroid.ui.utils.testApp

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun Versions(
    item: AppDetailsItem,
    scrollUp: suspend () -> Unit,
) {
    ExpandableSection(
        icon = rememberVectorPainter(Icons.Default.AccessTime),
        title = stringResource(R.string.versions),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier) {
            item.versions?.forEach { versionItem ->
                Version(
                    item = versionItem,
                    installAction = { version: AppVersion ->
                        item.actions.installAction(item.app, version, item.icon)
                    },
                    scrollUp = scrollUp,
                )
            }
        }
    }
}

@Composable
fun Version(
    item: VersionItem,
    installAction: (AppVersion) -> Unit,
    scrollUp: suspend () -> Unit,
) {
    val isPreview = LocalInspectionMode.current
    var expanded by rememberSaveable { mutableStateOf(isPreview) }
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Row(
            horizontalArrangement = spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .clickable {
                    expanded = !expanded
                }
        ) {
            ExpandIconChevron(expanded)
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.version.versionName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            R.string.added_x_ago,
                            item.version.added.asRelativeTimeString(),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (item.isInstalled) Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_installed),
                        modifier = Modifier.padding(2.dp)
                    )
                }
                if (item.isSuggested) Badge(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_suggested),
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = expanded,
            modifier = Modifier
                .semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Row(
                horizontalArrangement = spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (!item.isCompatible || !item.isSignerCompatible) Text(
                        text = if (!item.isCompatible) {
                            stringResource(R.string.app_details_incompatible_version)
                        } else {
                            stringResource(R.string.app_details_incompatible_signer)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    item.version.size?.let { size ->
                        Text(
                            text = stringResource(
                                R.string.size_colon,
                                Formatter.formatFileSize(LocalContext.current, size)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    val sdkString = buildString {
                        item.version.packageManifest.minSdkVersion?.let { sdk ->
                            append(stringResource(R.string.sdk_min_version, sdk))
                        }
                        item.version.packageManifest.targetSdkVersion?.let { sdk ->
                            if (isNotEmpty()) append(" ")
                            append(stringResource(R.string.sdk_target_version, sdk))
                        }
                        item.version.packageManifest.maxSdkVersion?.let { sdk ->
                            if (isNotEmpty()) append(" ")
                            append(stringResource(R.string.sdk_max_version, sdk))
                        }
                    }
                    if (sdkString.isNotEmpty()) Text(
                        text = stringResource(R.string.sdk_versions_colon, sdkString),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    item.version.packageManifest.nativecode?.let { nativeCode ->
                        if (nativeCode.isNotEmpty()) {
                            Text(
                                text = stringResource(
                                    R.string.architectures_colon,
                                    nativeCode.joinToString(", ")
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    item.version.signer?.let { signer ->
                        Text(
                            text = stringResource(
                                R.string.signer_colon,
                                signer.sha256[0].substring(0..15)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (item.showInstallButton) {
                    val coroutineScope = rememberCoroutineScope()
                    FDroidOutlineButton(
                        text = stringResource(R.string.menu_install),
                        onClick = {
                            installAction(item.version as AppVersion)
                            coroutineScope.launch {
                                scrollUp()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun VersionsPreview() {
    FDroidContent {
        Versions(testApp) {}
    }
}
