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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.fdroid.database.AppVersion
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.index.v2.PackageVersion
import org.fdroid.next.R
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
            item.versions?.forEach { version ->
                Version(
                    version = version,
                    isInstalled = item.installedVersion == version,
                    isSuggested = item.suggestedVersion == version,
                    isInstallable = if (item.installState.showProgress) {
                        false
                    } else {
                        if (item.installedVersion == null) {
                            true
                        } else {
                            // TODO take compatibility and signer into account
                            item.installedVersion.versionCode < version.versionCode
                        }
                    },
                    installAction = { version: AppVersion ->
                        item.actions.installAction(item.app, version)
                    },
                    scrollUp = scrollUp,
                )
            }
        }
    }
}

@Composable
fun Version(
    version: PackageVersion,
    isInstalled: Boolean,
    isSuggested: Boolean,
    isInstallable: Boolean,
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
            Icon(
                imageVector = if (expanded) {
                    Icons.Default.ExpandLess
                } else {
                    Icons.Default.ExpandMore
                },
                contentDescription = null,
            )
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = version.versionName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            R.string.added_x_ago,
                            version.added.asRelativeTimeString(),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (isInstalled) Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_installed),
                        modifier = Modifier.padding(2.dp)
                    )
                }
                if (isSuggested) Badge(
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
        AnimatedVisibility(expanded) {
            Row(
                horizontalArrangement = spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    version.size?.let { size ->
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
                    version.packageManifest.nativecode?.let { nativeCode ->
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
                    version.signer?.let { signer ->
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
                if (isInstallable) {
                    val coroutineScope = rememberCoroutineScope()
                    FDroidOutlineButton(
                        text = stringResource(R.string.menu_install),
                        onClick = {
                            installAction(version as AppVersion)
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
