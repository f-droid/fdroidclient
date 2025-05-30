package org.fdroid.basic.ui.main.details

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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import org.fdroid.basic.R
import org.fdroid.basic.ui.icons.PackageVariant
import org.fdroid.database.Repository


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RepoChooser(
    repos: List<String>,
    currentRepoId: Long,
    preferredRepoId: Long,
    onRepoChanged: (Repository) -> Unit,
    onPreferredRepoChanged: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (repos.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val currentRepo = repos[0]
    val isPreferred = true
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
                    Icon(PackageVariant, null, Modifier.size(24.dp))
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
                        isPreferred = repo == repos[0],
                        onClick = {
                            // onRepoChanged(repo)
                            expanded = false
                        },
                        modifier = modifier,
                    )
                }
            }
        }
        if (!isPreferred) {
            OutlinedButton(
                onClick = { onPreferredRepoChanged(0L) },
                modifier = Modifier
                    .align(End)
                    .padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.app_details_repository_button_prefer))
            }
        }
    }
}

@Composable
private fun RepoMenuItem(
    repo: String,
    isPreferred: Boolean,
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
        leadingIcon = { Icon(PackageVariant, null, Modifier.size(24.dp)) }
    )
}

@Composable
private fun getRepoString(repo: String, isPreferred: Boolean) = buildAnnotatedString {
    append(repo)
    if (isPreferred) {
        append(" ")
        pushStyle(SpanStyle(fontWeight = Bold))
        append(" ")
        append(stringResource(R.string.app_details_repository_preferred))
    }
}
