package org.fdroid.ui.repositories

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.os.LocaleListCompat
import com.android.tools.screenshot.PreviewTest
import kotlinx.coroutines.flow.MutableStateFlow
import org.fdroid.database.MinimalApp
import org.fdroid.download.NetworkState
import org.fdroid.index.v2.FileV2
import org.fdroid.repo.AddRepoError
import org.fdroid.repo.AddRepoError.ErrorType.IO_ERROR
import org.fdroid.repo.Adding
import org.fdroid.repo.FetchResult.IsNewRepository
import org.fdroid.repo.Fetching
import org.fdroid.repo.None
import org.fdroid.ui.ScreenshotTest
import org.fdroid.ui.repositories.add.AddRepo
import org.fdroid.ui.utils.getRepository

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun AddRepoIntroTest() =
  ScreenshotTest(showBottomBar = false) {
    AddRepo(
      state = None,
      networkStateFlow = networkStateFlow,
      proxyConfig = null,
      onFetchRepo = {},
      onAddRepo = {},
      onExistingRepo = {},
      onRepoAdded = { _, _ -> },
      onBackClicked = {},
    )
  }

@Composable
@PreviewTest
@Preview(
  showBackground = true,
  showSystemUi = true,
  uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
fun AddRepoIntroNightTest() =
  ScreenshotTest(showBottomBar = false) {
    AddRepo(
      state = None,
      networkStateFlow = MutableStateFlow(NetworkState(isOnline = false, isMetered = false)),
      proxyConfig = null,
      onFetchRepo = {},
      onAddRepo = {},
      onExistingRepo = {},
      onRepoAdded = { _, _ -> },
      onBackClicked = {},
    )
  }

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun AddRepoFetchingProgressTest() =
  ScreenshotTest(showBottomBar = false) {
    AddRepo(
      state =
        Fetching(
          fetchUrl = "https://example.org/fdroid/repo",
          receivedRepo = null,
          apps = emptyList(),
          fetchResult = null,
        ),
      networkStateFlow = networkStateFlow,
      proxyConfig = null,
      onFetchRepo = {},
      onAddRepo = {},
      onExistingRepo = {},
      onRepoAdded = { _, _ -> },
      onBackClicked = {},
    )
  }

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun AddRepoFetchingPreviewTest() =
  ScreenshotTest(showBottomBar = false) {
    val repo = getRepository(address = "https://example.org/fdroid/repo")
    AddRepo(
      state =
        Fetching(
          fetchUrl = "https://example.org/fdroid/repo",
          receivedRepo = repo,
          apps = previewApps,
          fetchResult = IsNewRepository,
        ),
      networkStateFlow = networkStateFlow,
      proxyConfig = null,
      onFetchRepo = {},
      onAddRepo = {},
      onExistingRepo = {},
      onRepoAdded = { _, _ -> },
      onBackClicked = {},
    )
  }

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun AddRepoAddingTest() =
  ScreenshotTest(showBottomBar = false) {
    AddRepo(
      state = Adding,
      networkStateFlow = networkStateFlow,
      proxyConfig = null,
      onFetchRepo = {},
      onAddRepo = {},
      onExistingRepo = {},
      onRepoAdded = { _, _ -> },
      onBackClicked = {},
    )
  }

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun AddRepoErrorTest() =
  ScreenshotTest(showBottomBar = false) {
    AddRepo(
      state = AddRepoError(IO_ERROR),
      networkStateFlow = networkStateFlow,
      proxyConfig = null,
      onFetchRepo = {},
      onAddRepo = {},
      onExistingRepo = {},
      onRepoAdded = { _, _ -> },
      onBackClicked = {},
    )
  }

private val networkStateFlow = MutableStateFlow(NetworkState(isOnline = true, isMetered = false))

private val previewApps =
  listOf(
    object : MinimalApp {
      override val repoId = 0L
      override val packageName = "org.example.app"
      override val name = "Example App"
      override val summary = "A short summary of the example app"

      override fun getIcon(localeList: LocaleListCompat): FileV2? = null
    },
    object : MinimalApp {
      override val repoId = 0L
      override val packageName = "com.example.another"
      override val name = "Another App with a Longer Name"
      override val summary = "Summary of another app that is also somewhat long"

      override fun getIcon(localeList: LocaleListCompat): FileV2? = null
    },
  )
