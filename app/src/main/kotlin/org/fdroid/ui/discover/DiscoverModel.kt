package org.fdroid.ui.discover

import androidx.compose.foundation.text.input.TextFieldState
import org.fdroid.download.NetworkState
import org.fdroid.repo.RepoUpdateState
import org.fdroid.ui.categories.CategoryGroup
import org.fdroid.ui.categories.CategoryItem

sealed class DiscoverModel

data class FirstStartDiscoverModel(
  val networkState: NetworkState,
  val repoUpdateState: RepoUpdateState?,
) : DiscoverModel()

data object LoadingDiscoverModel : DiscoverModel()

data object NoEnabledReposDiscoverModel : DiscoverModel()

data class LoadedDiscoverModel(
  val newApps: List<AppDiscoverItem>,
  val recentlyUpdatedApps: List<AppDiscoverItem>,
  val mostDownloadedApps: List<AppDiscoverItem>?,
  val categories: Map<CategoryGroup, List<CategoryItem>>?,
  val searchTextFieldState: TextFieldState,
  val hasRepoIssues: Boolean,
) : DiscoverModel()
