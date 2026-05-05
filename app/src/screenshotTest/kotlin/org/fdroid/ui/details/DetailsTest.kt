package org.fdroid.ui.details

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import org.fdroid.download.NetworkState
import org.fdroid.ui.ScreenshotTest
import org.fdroid.ui.utils.testApp

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true, heightDp = 3000)
fun DetailsTest() =
  ScreenshotTest(showBottomBar = false) {
    AppDetails(
      item =
        testApp.copy(
          versions = emptyList(),
          categories = testApp.categories?.subList(0, 2),
          phoneScreenshots =
            listOf(
              "https://f-droid.org/repo/org.fdroid.fdroid.2024-05-31-17-00-00.png",
              "https://f-droid.org/repo/org.fdroid.fdroid.2024-05-31-17-00-00.png",
              "https://f-droid.org/repo/org.fdroid.fdroid.2024-05-31-17-00-00.png",
              "https://f-droid.org/repo/org.fdroid.fdroid.2024-05-31-17-00-00.png",
              "https://f-droid.org/repo/org.fdroid.fdroid.2024-05-31-17-00-00.png",
            ),
          networkState = NetworkState(isOnline = true, isMetered = false),
        ),
      onNav = {},
      onBackNav = {},
    )
  }

@Preview
@Composable
@PreviewTest
fun AppDetailsNotFoundTest() =
  ScreenshotTest(showBottomBar = false) { AppDetails(NotFoundAppDetailsItem, {}, {}) }

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true, heightDp = 3000)
fun DetailsInstallTest() =
  ScreenshotTest(showBottomBar = false) {
    // reduces information, so we can see the bottom of the expanded page
    // shows install button instead of open button
    AppDetails(
      item =
        testApp.copy(
          versions = listOf(testApp.versions!!.first()),
          whatsNew = null,
          antiFeatures = null,
          installedVersion = null,
          installedVersionCode = null,
          installedVersionName = null,
          suggestedVersion = testApp.versions.first().version,
          categories = testApp.categories?.subList(0, 2),
          actions = testApp.actions.copy(launchIntent = null),
          networkState = NetworkState(isOnline = true, isMetered = false),
        ),
      onNav = {},
      onBackNav = {},
    )
  }

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun DetailsUpdateTest() =
  ScreenshotTest(showBottomBar = false) {
    // show update and open button, hide donation options
    AppDetails(
      item =
        testApp.copy(
          whatsNew = null,
          antiFeatures = null,
          app =
            testApp.app.copy(
              donate = null,
              bitcoin = null,
              litecoin = null,
              liberapay = null,
              liberapayID = null,
              openCollective = null,
            ),
          suggestedVersion = testApp.versions?.first()?.version,
          categories = testApp.categories?.subList(0, 2),
          networkState = NetworkState(isOnline = true, isMetered = false),
        ),
      onNav = {},
      onBackNav = {},
    )
  }

@Composable
@PreviewTest
@Preview(
  showBackground = true,
  showSystemUi = true,
  uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
  heightDp = 3000,
)
fun DetailsNightTest() =
  ScreenshotTest(showBottomBar = false) {
    AppDetails(
      item = testApp.copy(categories = testApp.categories?.subList(0, 2), whatsNew = "foo bar"),
      onNav = {},
      onBackNav = {},
    )
  }
