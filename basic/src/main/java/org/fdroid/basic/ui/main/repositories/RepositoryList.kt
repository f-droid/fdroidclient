package org.fdroid.basic.ui.main.repositories

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.UI_MODE_TYPE_NORMAL
import android.os.Parcelable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import kotlinx.parcelize.Parcelize
import org.fdroid.basic.R
import org.fdroid.fdroid.ui.theme.FDroidContent

@Composable
@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
fun RepositoryList(
    repositories: List<Repository>?,
    currentRepository: Repository?,
    onRepositorySelected: (Repository) -> Unit,
    onAddRepo: () -> Unit,
    onBackClicked: () -> Unit,
) {
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddRepo,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add repo")
            }
        }
    ) { paddingValues ->
        if (repositories == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                LoadingIndicator(modifier = Modifier.padding(paddingValues))
            }
        } else {
            RepositoriesList(
                repositories = repositories,
                currentRepository = currentRepository,
                onRepositorySelected = {
                    onRepositorySelected(it)
                },
                modifier = Modifier
                    .padding(paddingValues),
            )
        }
    }
}

@Preview(
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL,
)
@Composable
fun RepositoriesScaffoldLoadingPreview() {
    FDroidContent {
        RepositoryList(null, null, {}, {}) { }
    }
}

@Preview(
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL,
)
@Composable
fun RepositoriesScaffoldPreview() {
    FDroidContent {
        val repos = listOf(
            Repository(
                repoId = 1,
                address = "http://example.org",
                timestamp = System.currentTimeMillis(),
                lastUpdated = null,
                weight = 1,
                enabled = true,
                name = "My first repository",
            ),
            Repository(
                repoId = 2,
                address = "http://example.com",
                timestamp = System.currentTimeMillis(),
                lastUpdated = null,
                weight = 2,
                enabled = true,
                name = "My second repository",
            ),
        )
        RepositoryList(repos, repos[0], {}, {}) { }
    }
}

@Parcelize
data class Repository(
    val repoId: Long,
    val address: String,
    val timestamp: Long,
    val lastUpdated: Long?,
    val weight: Int,
    val enabled: Boolean,
    private val name: String,
) : Parcelable {
    fun getName(localeList: LocaleListCompat): String? = name
}
