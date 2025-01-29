package org.fdroid.fdroid.views.repos

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Bottom
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import org.fdroid.database.Repository
import org.fdroid.download.Mirror
import org.fdroid.fdroid.FDroidApp
import org.fdroid.fdroid.R
import org.fdroid.fdroid.Utils
import org.fdroid.fdroid.compose.ComposeUtils.FDroidButton
import org.fdroid.fdroid.compose.ComposeUtils.FDroidOutlineButton
import org.fdroid.fdroid.compose.FDroidExpandableRow
import org.fdroid.fdroid.compose.FDroidSwitchRow
import org.fdroid.fdroid.flagEmoji
import org.fdroid.fdroid.ui.theme.FDroidContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoDetailsScreen(
    repo: Repository,
    archiveState: ArchiveState,
    numberOfApps: Int,
    // app bar functions
    onBackClicked: () -> Unit,
    onShareClicked: () -> Unit,
    onShowQrCodeClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onInfoClicked: () -> Unit,
    // other buttons
    onShowAppsClicked: () -> Unit,
    onToggleArchiveClicked: (Boolean) -> Unit,
    onEditCredentialsClicked: () -> Unit,
    // mirror actions
    setMirrorEnabled: (Mirror, Boolean) -> Unit,
    onShareMirror: (Mirror) -> Unit,
    onDeleteMirror: (Mirror) -> Unit,
) {
    val officialMirrors = repo.getAllOfficialMirrors()
    val userMirrors = repo.getAllUserMirrors()
    val disabledMirrors = repo.disabledMirrors.toHashSet()

    Scaffold(topBar = {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBackClicked) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
            },
            title = {
                Text(stringResource(R.string.repo_details))
            },
            actions = {
                IconButton(onClick = onShareClicked) {
                    Icon(Icons.Default.Share, stringResource(R.string.share_repository))
                }
                IconButton(onClick = onShowQrCodeClicked) {
                    Icon(Icons.Default.QrCode, stringResource(R.string.show_repository_qr))
                }
                IconButton(onClick = onDeleteClicked) {
                    Icon(Icons.Default.Delete, stringResource(R.string.delete))
                }
                IconButton(onClick = onInfoClicked) {
                    Icon(Icons.Default.Info, stringResource(R.string.repo_details))
                }
            })
    }) { paddingContent ->
        Box(
            modifier = Modifier.padding(paddingContent)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                GeneralInfoCard(
                    repo,
                    numberOfApps,
                    onShowAppsClicked,
                )
                repo.username?.let {
                    if (!it.isBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        BasicAuthCard(it, onEditCredentialsClicked)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (repo.certificate.isEmpty()) {
                    UnsignedCard()
                } else {
                    FingerprintExpandable(repo)
                }
                // The repo's address is currently also an official mirror.
                // So if there is only one mirror, this is the address => don't show this section.
                // If there are 2 or more official mirrors, it makes sense to allow users
                // to disable the canonical address.
                if (officialMirrors.size > 2) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OfficialMirrors(
                        mirrors = officialMirrors,
                        disabledMirrors = disabledMirrors,
                        setMirrorEnabled = setMirrorEnabled,
                    )
                }
                if (userMirrors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    UserMirrors(
                        mirrors = userMirrors,
                        disabledMirrors = disabledMirrors,
                        setMirrorEnabled = setMirrorEnabled,
                        onShareMirror = onShareMirror,
                        onDeleteMirror = onDeleteMirror,
                    )
                }
                // TODO: Add button to add user mirror?
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(archiveState, onToggleArchiveClicked)
                Spacer(modifier = Modifier.height(16.dp))
            }
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
    val isDevPreview = LocalInspectionMode.current
    val description = if (isDevPreview) {
        LoremIpsum(42).values.joinToString(" ")
    } else {
        repo.getDescription(localeList)
    }?.replace("\n", " ")

    val lastIndexUpdateTime = DateUtils.getRelativeTimeSpanString(
        repo.timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_ALL
    )
    val lastIndexUpdate = stringResource(R.string.repo_last_update_index, lastIndexUpdateTime)

    val lastUpdatedTime = repo.lastUpdated?.let {
        DateUtils.getRelativeTimeSpanString(
            it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL
        )
    } ?: stringResource(R.string.repositories_last_update_never)
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

            if (description != null) {
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
private fun FingerprintExpandable(repo: Repository) {
    val isDevPreview = LocalInspectionMode.current
    val fingerprint: String = if (isDevPreview) {
        Utils.formatFingerprint(LocalContext.current, "A".repeat(64))
    } else {
        repo.fingerprint?.let {
            Utils.formatFingerprint(LocalContext.current, it)
        } ?: stringResource(R.string.unsigned)
    }

    FDroidExpandableRow(
        text = stringResource(R.string.repo_fingerprint),
        imageVectorStart = Icons.Default.Fingerprint,
    ) {
        Text(
            text = fingerprint,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun UnsignedCard() {
    ElevatedCard {
        Text(
            text = stringResource(R.string.repo_unsigned_description),
            color = colorResource(R.color.unsigned),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
        Column(
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            mirrors.forEachIndexed { idx, m ->
                val icon = if (m.isOnion()) {
                    "🧅"
                } else {
                    m.countryCode?.flagEmoji ?: ""
                }
                Row(
                    horizontalArrangement = spacedBy(8.dp),
                    verticalAlignment = CenterVertically,
                ) {
                    Text(
                        text = icon,
                        modifier = Modifier.width(20.dp),
                    )
                    FDroidSwitchRow(
                        text = m.baseUrl,
                        checked = !disabledMirrors.contains(m.baseUrl),
                        onCheckedChange = { checked -> setMirrorEnabled(m, checked) },
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                if (idx < mirrors.size - 1) Divider()
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
            Column(
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                FDroidSwitchRow(
                    text = m.baseUrl,
                    checked = !disabledMirrors.contains(m.baseUrl),
                    onCheckedChange = { checked -> setMirrorEnabled(m, checked) },
                    modifier = Modifier.padding(vertical = 8.dp),
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
                if (idx < mirrors.size - 1) Divider()
            }
        }
    }
}

@Composable
private fun SettingsRow(
    archiveState: ArchiveState,
    onToggleArchiveClicked: (Boolean) -> Unit,
) {
    if (archiveState != ArchiveState.UNKNOWN) {
        FDroidExpandableRow(
            text = stringResource(R.string.menu_settings),
            imageVectorStart = Icons.Default.Settings,
        ) {
            FDroidSwitchRow(
                text = stringResource(R.string.repo_archive_toggle_description),
                checked = archiveState == ArchiveState.ENABLED,
                enabled = true,
                onCheckedChange = onToggleArchiveClicked,
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }
    }
}

/* Previews */

@Composable
@Preview
fun RepoDetailsScreenPreview() {
    val repo = FDroidApp.createSwapRepo("https://example.org/fdroid/repo", "foo bar")
    FDroidContent {
        RepoDetailsScreen(
            repo,
            ArchiveState.ENABLED,
            numberOfApps = 42,
            {}, {}, {}, {}, {}, // app bar
            {}, {}, {}, // other buttons
            { _, _ -> }, { _ -> }, { _ -> } // mirror
        )
    }
}

@Composable
@Preview
fun BasicAuthCardPreview() {
    // TODO set RepositoryPreferences
    FDroidContent {
        BasicAuthCard("username", { })
    }
}

@Composable
@Preview
fun UnsignedCardPreview() {
    FDroidContent {
        UnsignedCard()
    }
}

@Composable
@Preview
fun OfficialMirrorsPreview() {
    FDroidContent {
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
    FDroidContent {
        UserMirrors(
            mirrors = listOf(Mirror("https://mirror.example.com/fdroid/repo")),
            disabledMirrors = HashSet(),
            setMirrorEnabled = { _, _ -> },
            onShareMirror = { _ -> },
            onDeleteMirror = { _ -> },
        )
    }
}
