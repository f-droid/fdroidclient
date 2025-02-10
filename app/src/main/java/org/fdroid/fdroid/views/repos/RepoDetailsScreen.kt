package org.fdroid.fdroid.views.repos

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Bottom
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import org.fdroid.database.DUMMY_TEST_REPO
import org.fdroid.database.Repository
import org.fdroid.download.Mirror
import org.fdroid.fdroid.R
import org.fdroid.fdroid.Utils
import org.fdroid.fdroid.asRelativeTimeString
import org.fdroid.fdroid.compose.ComposeUtils.FDroidButton
import org.fdroid.fdroid.compose.ComposeUtils.FDroidOutlineButton
import org.fdroid.fdroid.compose.FDroidExpandableRow
import org.fdroid.fdroid.compose.FDroidSwitchRow
import org.fdroid.fdroid.copy
import org.fdroid.fdroid.flagEmoji
import org.fdroid.fdroid.ui.theme.FDroidContent

interface RepoDetailsScreenCallbacks {
    // app bar functions
    fun onBackClicked()
    fun onShareClicked()
    fun onShowQrCodeClicked()
    fun onDeleteClicked()
    fun onInfoClicked()

    // other buttons
    fun onShowAppsClicked()
    fun onToggleArchiveClicked(enabled: Boolean)
    fun onEditCredentialsClicked()

    // mirror actions
    fun setMirrorEnabled(mirror: Mirror, enabled: Boolean)
    fun onShareMirror(mirror: Mirror)
    fun onDeleteMirror(mirror: Mirror)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoDetailsScreen(
    repo: Repository,
    archiveState: ArchiveState,
    numberOfApps: Int,
    callbacks: RepoDetailsScreenCallbacks,
) {
    val officialMirrors = repo.allOfficialMirrors
    val userMirrors = repo.allUserMirrors
    val disabledMirrors = repo.disabledMirrors.toHashSet()

    Scaffold(topBar = {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = callbacks::onBackClicked) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
            },
            title = {
                Text(stringResource(R.string.repo_details))
            },
            actions = {
                IconButton(onClick = callbacks::onShareClicked) {
                    Icon(Icons.Default.Share, stringResource(R.string.share_repository))
                }
                IconButton(onClick = callbacks::onShowQrCodeClicked) {
                    Icon(Icons.Default.QrCode, stringResource(R.string.show_repository_qr))
                }
                IconButton(onClick = callbacks::onDeleteClicked) {
                    Icon(Icons.Default.Delete, stringResource(R.string.delete))
                }
                IconButton(onClick = callbacks::onInfoClicked) {
                    Icon(Icons.Default.Info, stringResource(R.string.repo_details))
                }
            })
    }) { paddingContent ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // bottom padding is applied at the end, so that we draw behind the navigation bar
                .padding(paddingContent.copy(bottom = 0.dp))
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            GeneralInfoCard(repo, numberOfApps, callbacks::onShowAppsClicked)
            repo.username?.let {
                if (!it.isBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    BasicAuthCard(it, callbacks::onEditCredentialsClicked)
                }
            }
            repo.fingerprint?.let {
                FingerprintExpandable(it)
            }
            // The repo's address is currently also an official mirror.
            // So if there is only one mirror, this is the address => don't show this section.
            // If there are 2 or more official mirrors, it makes sense to allow users
            // to disable the canonical address.
            if (officialMirrors.size > 2) {
                OfficialMirrors(
                    mirrors = officialMirrors,
                    disabledMirrors = disabledMirrors,
                    setMirrorEnabled = callbacks::setMirrorEnabled,
                )
            }
            if (userMirrors.isNotEmpty()) {
                UserMirrors(
                    mirrors = userMirrors,
                    disabledMirrors = disabledMirrors,
                    setMirrorEnabled = callbacks::setMirrorEnabled,
                    onShareMirror = callbacks::onShareMirror,
                    onDeleteMirror = callbacks::onDeleteMirror,
                )
            }
            // TODO: Add button to add user mirror?
            SettingsRow(archiveState, callbacks::onToggleArchiveClicked)
            Spacer(
                modifier = Modifier
                    .height(16.dp + paddingContent.calculateBottomPadding())
            )
        }
    }
}

@Composable
private fun GeneralInfoCard(
    repo: Repository,
    numberOfApps: Int,
    onShowAppsClicked: () -> Unit,
) {
    val localeList = LocaleListCompat.getDefault()
    val description = repo.getDescription(localeList)?.replace("\n", " ")

    val lastIndexTime = if (repo.timestamp < 0) {
        stringResource(R.string.repositories_last_update_never)
    } else {
        repo.timestamp.asRelativeTimeString()
    }
    val lastIndexUpdate = stringResource(R.string.repo_last_update_index, lastIndexTime)

    val lastUpdatedTime = repo.lastUpdated?.asRelativeTimeString()
        ?: stringResource(R.string.repositories_last_update_never)
    val lastUpdated = stringResource(R.string.repo_last_update_check, lastUpdatedTime)

    ElevatedCard {
        Column(
            verticalArrangement = spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                horizontalArrangement = spacedBy(16.dp),
            ) {
                RepoIcon(repo, Modifier.size(48.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = repo.getName(localeList) ?: "Unknown Repository",
                        maxLines = 1,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = repo.address.replaceFirst("https://", ""),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.repo_num_apps_text,
                            numberOfApps,
                            numberOfApps,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = lastIndexUpdate,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = lastUpdated,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (repo.enabled) FDroidButton(
                stringResource(R.string.repo_num_apps_button),
                modifier = Modifier.align(End),
                onClick = onShowAppsClicked,
            )

            if (description?.isNotBlank() == true) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun BasicAuthCard(
    username: String,
    onEditCredentialsClicked: () -> Unit,
) {
    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.repo_basic_auth_title),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.repo_basic_auth_username, username),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.repo_basic_auth_password),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                FDroidOutlineButton(
                    text = stringResource(R.string.repo_basic_auth_edit),
                    onClick = onEditCredentialsClicked,
                    imageVector = Icons.Default.Edit,
                    modifier = Modifier.align(Bottom),
                )
            }
        }
    }
}

@Composable
private fun FingerprintExpandable(
    fingerprint: String,
) {
    FDroidExpandableRow(
        text = stringResource(R.string.repo_fingerprint),
        imageVectorStart = Icons.Default.Fingerprint,
    ) {
        Text(
            text = Utils.formatFingerprint(LocalContext.current, fingerprint),
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun OfficialMirrors(
    mirrors: List<Mirror>,
    disabledMirrors: HashSet<String>,
    setMirrorEnabled: (Mirror, Boolean) -> Unit,
) {
    FDroidExpandableRow(
        text = stringResource(R.string.repo_official_mirrors),
        imageVectorStart = Icons.Default.Public,
    ) {
        Column {
            mirrors.forEachIndexed { idx, m ->
                val icon = if (m.isOnion()) {
                    "🧅"
                } else {
                    m.countryCode?.flagEmoji ?: ""
                }
                FDroidSwitchRow(
                    text = m.baseUrl,
                    leadingContent = {
                        Text(
                            text = icon,
                            modifier = Modifier.width(20.dp),
                        )
                    },
                    checked = !disabledMirrors.contains(m.baseUrl),
                    onCheckedChange = { checked -> setMirrorEnabled(m, checked) },
                )
                if (idx < mirrors.size - 1) HorizontalDivider()
            }
        }
    }
}

@Composable
private fun UserMirrors(
    mirrors: List<Mirror>,
    disabledMirrors: HashSet<String>,
    setMirrorEnabled: (Mirror, Boolean) -> Unit,
    onShareMirror: (Mirror) -> Unit,
    onDeleteMirror: (Mirror) -> Unit,
) {
    FDroidExpandableRow(
        text = stringResource(R.string.repo_user_mirrors),
        imageVectorStart = Icons.Default.Dns,
    ) {
        mirrors.forEachIndexed { idx, m ->
            Column {
                FDroidSwitchRow(
                    text = m.baseUrl,
                    checked = !disabledMirrors.contains(m.baseUrl),
                    onCheckedChange = { checked -> setMirrorEnabled(m, checked) },
                )
                Row(
                    horizontalArrangement = spacedBy(16.dp),
                ) {
                    FDroidOutlineButton(
                        text = stringResource(R.string.menu_share),
                        imageVector = Icons.Default.Share,
                        onClick = { onShareMirror(m) },
                    )
                    FDroidOutlineButton(
                        text = stringResource(R.string.delete),
                        imageVector = Icons.Default.Delete,
                        onClick = { onDeleteMirror(m) },
                        color = Color.Red,
                    )
                }
                if (idx < mirrors.size - 1) HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SettingsRow(
    archiveState: ArchiveState,
    onToggleArchiveClicked: (Boolean) -> Unit,
) {
    FDroidExpandableRow(
        text = stringResource(R.string.menu_settings),
        imageVectorStart = Icons.Default.Settings,
    ) {
        if (archiveState == ArchiveState.UNKNOWN) {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.repo_archive_unknown))
                Spacer(modifier = Modifier.height(8.dp))
                FDroidOutlineButton(
                    text = stringResource(R.string.repo_archive_check),
                    onClick = { onToggleArchiveClicked(true) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else if (archiveState == ArchiveState.LOADING) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            FDroidSwitchRow(
                text = stringResource(R.string.repo_archive_toggle_description),
                checked = archiveState == ArchiveState.ENABLED,
                enabled = true,
                onCheckedChange = onToggleArchiveClicked,
            )
        }
    }
}

/* Previews */

private val emptyCallbacks = object : RepoDetailsScreenCallbacks {
    // app bar
    override fun onBackClicked() {}
    override fun onShareClicked() {}
    override fun onShowQrCodeClicked() {}
    override fun onDeleteClicked() {}
    override fun onInfoClicked() {}

    // other buttons
    override fun onShowAppsClicked() {}
    override fun onToggleArchiveClicked(enabled: Boolean) {}
    override fun onEditCredentialsClicked() {}

    // mirror
    override fun setMirrorEnabled(mirror: Mirror, enabled: Boolean) {}
    override fun onShareMirror(mirror: Mirror) {}
    override fun onDeleteMirror(mirror: Mirror) {}
}

@Composable
@Preview
fun RepoDetailsScreenPreview() {
    FDroidContent(pureBlack = true) {
        RepoDetailsScreen(
            repo = DUMMY_TEST_REPO,
            archiveState = ArchiveState.ENABLED,
            numberOfApps = 42,
            callbacks = emptyCallbacks,
        )
    }
}

@Composable
@Preview
fun BasicAuthCardPreview() {
    // TODO set RepositoryPreferences
    FDroidContent(pureBlack = true) {
        BasicAuthCard("username", { })
    }
}

@Composable
@Preview
fun OfficialMirrorsPreview() {
    FDroidContent(pureBlack = true) {
        OfficialMirrors(
            mirrors = listOf(Mirror("https://mirror.example.com/fdroid/repo")),
            disabledMirrors = HashSet(),
            setMirrorEnabled = { _, _ -> },
        )
    }
}

@Composable
@Preview
fun UserMirrorsPreview() {
    FDroidContent(pureBlack = true) {
        UserMirrors(
            mirrors = listOf(Mirror("https://mirror.example.com/fdroid/repo")),
            disabledMirrors = HashSet(),
            setMirrorEnabled = { _, _ -> },
            onShareMirror = { _ -> },
            onDeleteMirror = { _ -> },
        )
    }
}
