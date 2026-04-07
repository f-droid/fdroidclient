package org.fdroid.ui.repositories

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import org.fdroid.download.NetworkState
import org.fdroid.ui.ScreenshotTest
import org.fdroid.ui.utils.getRepositoriesInfo
import org.fdroid.ui.utils.repoItems

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun RepositoriesListTest() =
  ScreenshotTest(showBottomBar = false) {
    val model =
      RepositoryModel(
        repositories = repoItems,
        showOnboarding = false,
        lastCheckForUpdate = "42min. ago",
        networkState = NetworkState(isOnline = false, isMetered = false),
      )
    val info = getRepositoriesInfo(model, repoItems[0].repoId)
    Repositories(info, true) {}
  }

@Composable
@PreviewTest
@Preview(
  showBackground = true,
  showSystemUi = true,
  uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
fun RepositoriesListNightTest() =
  ScreenshotTest(showBottomBar = false) {
    val model =
      RepositoryModel(
        repositories = repoItems.map { if (it.name == "F-Droid") it else it.copy(enabled = false) },
        showOnboarding = false,
        lastCheckForUpdate = "23hrs. ago",
        networkState = NetworkState(isOnline = true, isMetered = false),
      )
    val info = getRepositoriesInfo(model, null)
    Repositories(info, false) {}
  }
