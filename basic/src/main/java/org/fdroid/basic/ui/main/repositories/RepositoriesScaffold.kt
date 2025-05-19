package org.fdroid.basic.ui.main.repositories

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.UI_MODE_TYPE_NORMAL
import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole.Detail
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.defaultDragHandleSemantics
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
fun RepositoriesScaffold(
    repositories: List<Repository>,
    currentRepository: Repository?,
    onRepositorySelected: (Repository) -> Unit,
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
                    RepositoriesList(
                        repositories = repositories,
                        currentRepository = if (isDetailVisible) currentRepository else null,
                        onRepositorySelected = {
                            onRepositorySelected(it)
                            scope.launch { navigator.navigateTo(Detail, it) }
                        },
                        modifier = Modifier
                            .padding(paddingValues),
                    )
                }
            }
        },
        detailPane = {
            AnimatedPane {
                Column(
                    verticalArrangement = spacedBy(16.dp),
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .safeDrawingPadding(),
                ) {
                    currentRepository?.let {
                        Text(it.getName(LocaleListCompat.getDefault()) ?: "Unknown repo")
                        Text("This will basically be the repo details screen from latest client")
                    } ?: Text(text = "No repository selected")
                }
            }
        },
        paneExpansionDragHandle = { state ->
            val interactionSource = remember { MutableInteractionSource() }
            VerticalDragHandle(
                modifier =
                    Modifier.paneExpansionDraggable(
                        state,
                        LocalMinimumInteractiveComponentSize.current,
                        interactionSource,
                        state.defaultDragHandleSemantics(),
                    ),
                interactionSource = interactionSource
            )
        },
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
    )
}

@Preview(
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL,
)
@PreviewScreenSizes
@Composable
fun RepositoriesScaffoldPreview() {
    FDroidContent {
        val repos = listOf(
            Repository(
                address = "http://example.org",
                timestamp = System.currentTimeMillis(),
                lastUpdated = null,
                weight = 1,
                enabled = true,
                name = "My first repository",
            ),
            Repository(
                address = "http://example.com",
                timestamp = System.currentTimeMillis(),
                lastUpdated = null,
                weight = 2,
                enabled = true,
                name = "My second repository",
            ),
        )
        RepositoriesScaffold(repos, repos[0], {}) { }
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
