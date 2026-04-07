package org.fdroid.ui.repositories

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import org.fdroid.download.NetworkState
import org.fdroid.ui.ScreenshotTest
import org.fdroid.ui.repositories.details.ArchiveState
import org.fdroid.ui.repositories.details.RepoDetails
import org.fdroid.ui.utils.getRepoDetailsInfo
import org.fdroid.ui.utils.getRepository

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true, heightDp = 1300)
fun RepositoryDetailsTest() =
  ScreenshotTest(showBottomBar = false) { RepoDetails(getRepoDetailsInfo(), { _, _ -> }, {}) }

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun RepositoryDetailsMinTest() =
  ScreenshotTest(showBottomBar = false) {
    val info = getRepoDetailsInfo()
    RepoDetails(
      getRepoDetailsInfo(
        info.model.copy(
          repo = getRepository(username = null, password = null, lastError = null),
          officialMirrors = listOf(info.model.officialMirrors.first()),
          userMirrors = emptyList(),
          updateState = null,
          archiveState = ArchiveState.ENABLED,
          networkState = NetworkState(isOnline = true, isMetered = true),
        )
      ),
      { _, _ -> },
      {},
    )
  }

@Composable
@PreviewTest
@Preview(
  showBackground = true,
  showSystemUi = true,
  uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
  heightDp = 1300,
)
fun RepositoryDetailsNightTest() =
  ScreenshotTest(showBottomBar = false) {
    val info = getRepoDetailsInfo()
    RepoDetails(
      getRepoDetailsInfo(
        info.model.copy(
          updateState = null,
          archiveState = ArchiveState.UNKNOWN,
          networkState = NetworkState(isOnline = false, isMetered = true),
        )
      ),
      { _, _ -> },
      {},
    )
  }
