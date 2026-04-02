package org.fdroid.ui.details

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import org.fdroid.download.NetworkState
import org.fdroid.install.InstallState
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.testApp

@Preview
@Composable
@PreviewTest
fun DetailsHeaderOpenTest() {
  FDroidContent { Column { AppDetailsHeader(testApp, PaddingValues(top = 8.dp)) } }
}

@Preview
@Composable
@PreviewTest
private fun DetailsHeaderInstallTest() {
  FDroidContent {
    Column {
      AppDetailsHeader(
        testApp.copy(
          installedVersion = null,
          installedVersionCode = null,
          installedVersionName = null,
          suggestedVersion = testApp.versions?.first()?.version,
          networkState = NetworkState(true, isMetered = true),
        ),
        PaddingValues(top = 8.dp),
      )
    }
  }
}

@Preview
@Composable
@PreviewTest
private fun DetailsHeaderUpdateTest() {
  FDroidContent {
    Column {
      AppDetailsHeader(
        testApp.copy(
          suggestedVersion = testApp.versions?.first()?.version,
          networkState = NetworkState(true, isMetered = true),
        ),
        PaddingValues(top = 8.dp),
      )
    }
  }
}

@Preview
@Composable
@PreviewTest
private fun DetailsHeaderLoadingTest() {
  FDroidContent {
    Column {
      val app = testApp.copy(versions = null)
      AppDetailsHeader(app, PaddingValues(top = 8.dp))
    }
  }
}

@Preview
@Composable
@PreviewTest
private fun DetailsHeaderDownloadingTest() {
  FDroidContent {
    Column {
      val now = System.currentTimeMillis()
      val app =
        testApp.copy(
          installState =
            InstallState.Downloading(
              name = "",
              versionName = "",
              currentVersionName = "",
              lastUpdated = 23L,
              iconModel = null,
              downloadedBytes = 1024 * 1024 * 3,
              totalBytes = 1024 * 1024 * 8,
              startMillis = now - 2000,
            ),
          networkState = NetworkState(true, isMetered = true),
        )
      AppDetailsHeader(app, PaddingValues(top = 8.dp), now)
    }
  }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
@PreviewTest
private fun DetailsHeaderDownloadingNightTest() {
  FDroidContent {
    Column {
      val now = System.currentTimeMillis()
      val app =
        testApp.copy(
          installState =
            InstallState.Downloading(
              name = "",
              versionName = "",
              currentVersionName = "",
              lastUpdated = 23L,
              iconModel = null,
              downloadedBytes = 1024 * 1024 * 3,
              totalBytes = 1024 * 1024 * 8,
              startMillis = now - 2000,
            ),
          networkState = NetworkState(true, isMetered = true),
        )
      AppDetailsHeader(app, PaddingValues(top = 8.dp), now)
    }
  }
}

@Preview
@Composable
@PreviewTest
private fun DetailsHeaderStartingTest() {
  FDroidContent {
    Column {
      val app =
        testApp.copy(
          installState = InstallState.Starting("", "", "", 23),
          networkState = NetworkState(true, isMetered = true),
        )
      AppDetailsHeader(app, PaddingValues(top = 16.dp))
    }
  }
}
