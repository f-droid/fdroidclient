package org.fdroid.ui.discover

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import org.fdroid.download.NetworkState
import org.fdroid.repo.RepoUpdateProgress
import org.fdroid.ui.ScreenshotTest
import org.fdroid.ui.categories.CategoryGroups
import org.fdroid.ui.categories.CategoryItem

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun DiscoverFirstStartTest() = ScreenshotTest {
  Discover(
    discoverModel =
      FirstStartDiscoverModel(
        networkState = NetworkState(isOnline = true, isMetered = false),
        repoUpdateState = RepoUpdateProgress(1, true, 0.25f),
      ),
    onListTap = {},
    onAppTap = {},
    onNav = {},
  )
}

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
private fun DiscoverNoEnabledReposTest() = ScreenshotTest {
  Discover(discoverModel = NoEnabledReposDiscoverModel, onListTap = {}, onAppTap = {}, onNav = {})
}

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
private fun DiscoverTest() {
  val app1 =
    AppDiscoverItem(
      packageName = "foo bar",
      name = "New App!",
      isInstalled = false,
      imageModel = null,
      lastUpdated = 10,
    )
  val app2 =
    AppDiscoverItem(
      packageName = "bar foo",
      name = "Nice App!",
      isInstalled = false,
      imageModel = null,
      lastUpdated = 9,
    )
  val app3 =
    AppDiscoverItem(
      packageName = "org.example",
      name = "Downloaded App!",
      isInstalled = false,
      imageModel = null,
      lastUpdated = 8,
    )
  ScreenshotTest {
    val model =
      LoadedDiscoverModel(
        newApps = listOf(app1),
        recentlyUpdatedApps = listOf(app2),
        mostDownloadedApps = listOf(app3),
        categories =
          mapOf(CategoryGroups.productivity to listOf(CategoryItem("Calculator", "Calculator"))),
        searchTextFieldState = rememberTextFieldState(),
        hasRepoIssues = false,
      )
    Discover(discoverModel = model, onListTap = {}, onAppTap = {}, onNav = {})
  }
}
