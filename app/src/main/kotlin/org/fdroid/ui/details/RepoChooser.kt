package org.fdroid.ui.details

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import io.ktor.client.engine.ProxyConfig
import org.fdroid.R
import org.fdroid.database.Repository
import org.fdroid.index.IndexFormatVersion.TWO
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.repositories.RepoIcon
import org.fdroid.ui.utils.FDroidOutlineButton

@Composable
fun RepoChooser(
    repos: List<Repository>,
    currentRepoId: Long,
    preferredRepoId: Long,
    proxy: ProxyConfig?,
    onRepoChanged: (Long) -> Unit,
    onPreferredRepoChanged: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (repos.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val currentRepo = remember(currentRepoId) {
        repos.find { it.repoId == currentRepoId } ?: error("Current repoId not in list")
    }
    val isPreferred = currentRepo.repoId == preferredRepoId
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Box {
            val borderColor = if (isPreferred) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            }
            OutlinedTextField(
                value = TextFieldValue(
                    annotatedString = getRepoString(
                        repo = currentRepo,
                        isPreferred = repos.size > 1 && isPreferred,
                    ),
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
                onValueChange = {},
                label = {
                    if (repos.size == 1) {
                        Text(stringResource(R.string.app_details_repository))
                    } else {
                        Text(stringResource(R.string.app_details_repositories))
                    }
                },
                leadingIcon = {
                    RepoIcon(repo = currentRepo, proxy = proxy, modifier = Modifier.size(24.dp))
                },
                trailingIcon = {
                    if (repos.size > 1) Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(R.string.app_details_repository_expand),
                        tint = if (isPreferred) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                },
                singleLine = false,
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    // hack to enable clickable and look like enabled
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = borderColor,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface,
                    disabledLabelColor = borderColor,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .let {
                        if (repos.size > 1) it.clickable(onClick = { expanded = true }) else it
                    },
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                repos.iterator().forEach { repo ->
                    RepoMenuItem(
                        repo = repo,
                        isPreferred = repo.repoId == preferredRepoId,
                        proxy = proxy,
                        onClick = {
                            onRepoChanged(repo.repoId)
                            expanded = false
                        },
                        modifier = modifier,
                    )
                }
            }
        }
        if (!isPreferred) {
            FDroidOutlineButton(
                text = stringResource(R.string.app_details_repository_button_prefer),
                onClick = { onPreferredRepoChanged(currentRepo.repoId) },
                modifier = Modifier
                    .align(End)
                    .padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun RepoMenuItem(
    repo: Repository,
    isPreferred: Boolean,
    proxy: ProxyConfig?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = getRepoString(repo, isPreferred),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        modifier = modifier,
        onClick = onClick,
        leadingIcon = { RepoIcon(repo, proxy, Modifier.size(24.dp)) }
    )
}

@Composable
private fun getRepoString(repo: Repository, isPreferred: Boolean) = buildAnnotatedString {
    append(repo.getName(LocaleListCompat.getDefault()) ?: "Unknown Repository")
    if (isPreferred) {
        append(" ")
        pushStyle(SpanStyle(fontWeight = Bold))
        append(" ")
        append(stringResource(R.string.app_details_repository_preferred))
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
fun RepoChooserSingleRepoPreview() {
    val repo1 = Repository(1L, "1", 1L, TWO, "null", 1L, 1, 1L)
    FDroidContent {
        RepoChooser(listOf(repo1), 1L, 1L, null, {}, {})
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
fun RepoChooserPreview() {
    val repo1 = Repository(1L, "1", 1L, TWO, "null", 1L, 1, 1L)
    val repo2 = Repository(2L, "2", 2L, TWO, "null", 2L, 2, 2L)
    val repo3 = Repository(3L, "2", 3L, TWO, "null", 3L, 3, 3L)
    FDroidContent {
        RepoChooser(listOf(repo1, repo2, repo3), 1L, 1L, null, {}, {})
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
fun RepoChooserNightPreview() {
    val repo1 = Repository(1L, "1", 1L, TWO, "null", 1L, 1, 1L)
    val repo2 = Repository(2L, "2", 2L, TWO, "null", 2L, 2, 2L)
    val repo3 = Repository(3L, "2", 3L, TWO, "null", 3L, 3, 3L)
    FDroidContent {
        RepoChooser(listOf(repo1, repo2, repo3), 1L, 2L, null, {}, {})
    }
}
