package org.fdroid.basic.ui.main

import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole.Detail
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.fdroid.basic.R
import org.fdroid.fdroid.ui.theme.FDroidContent

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
fun Repositories(
    repositories: List<Repository>,
    currentRepository: Repository?,
    onBackClicked: () -> Unit,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<Repository>()
    val scope = rememberCoroutineScope()
    BackHandler(enabled = navigator.canNavigateBack()) {
        scope.launch {
            navigator.navigateBack()
        }
    }
    val isDetailVisible = navigator.scaffoldValue[Detail] == PaneAdaptedValue.Expanded
    val listState = rememberLazyListState()
    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            navigationIcon = {
                                IconButton(onClick = onBackClicked) {
                                    Icon(Icons.AutoMirrored.Default.ArrowBack, "back")
                                }
                            },
                            title = {
                                Text(stringResource(R.string.app_details_repositories))
                            },
                        )
                    },
                ) { paddingValues ->
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = spacedBy(8.dp),
                        modifier = Modifier
                            .padding(paddingValues)
                            .then(
                                if (currentRepository == null) Modifier
                                else Modifier.selectableGroup()
                            ),
                    ) {
                        val localeList = LocaleListCompat.getDefault()
                        val onItemClick: (Repository) -> Unit = {
                            scope.launch { navigator.navigateTo(Detail, it) }
                        }
                        items(repositories) { repoItem ->
                            val isSelected = currentRepository == repoItem
                            val interactionModifier = if (currentRepository == null) {
                                Modifier.clickable(
                                    onClick = { onItemClick(repoItem) }
                                )
                            } else {
                                Modifier.selectable(
                                    selected = isSelected,
                                    onClick = { onItemClick(repoItem) }
                                )
                            }
                            ListItem(
                                leadingContent = {
                                    Icon(
                                        Icons.Filled.Android,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        contentDescription = null,
                                    )
                                },
                                headlineContent = {
                                    Text(repoItem.getName(localeList) ?: "Unknown repo")
                                },
                                supportingContent = {
                                    Text(repoItem.address)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .then(interactionModifier),
                            )
                        }
                    }
                }
            }
        },
        detailPane = {
            AnimatedPane {
                navigator.currentDestination?.contentKey?.let {
                    Column(verticalArrangement = spacedBy(16.dp)) {
                        Text(it.getName(LocaleListCompat.getDefault()) ?: "Unknown repo")
                        Text("This will basically be the repo details screen from latest client")
                    }
                } ?: Text(
                    text = "No repository selected",
                    modifier = Modifier.padding(16.dp),
                )
            }
        },
    )
}

@Preview
@PreviewScreenSizes
@Composable
fun RepositoriesPreview() {
    FDroidContent {
        val repos = listOf(
            Repository(
                address = "http://example.org",
                timestamp = System.currentTimeMillis(),
                lastUpdated = null,
                weight = 2,
                enabled = true,
                name = "My first repository",
            )
        )
        Repositories(repos, repos[0]) { }
    }
}

@Parcelize
data class Repository(
    val address: String,
    val timestamp: Long,
    val lastUpdated: Long?,
    val weight: Int,
    val enabled: Boolean,
    private val name: String,
) : Parcelable {
    fun getName(localeList: LocaleListCompat): String? = name
}
