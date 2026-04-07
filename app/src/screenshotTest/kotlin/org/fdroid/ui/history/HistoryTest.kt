package org.fdroid.ui.history

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import java.util.concurrent.TimeUnit.DAYS
import org.fdroid.R
import org.fdroid.history.InstallEvent
import org.fdroid.history.UninstallEvent
import org.fdroid.ui.ScreenshotTest

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun HistoryLoadingTest() =
  ScreenshotTest(showBottomBar = false) { History(items = null, enabled = true, {}, {}, {}) }

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun HistoryEmptyTest() =
  ScreenshotTest(showBottomBar = false) {
    History(
      items = emptyList(),
      enabled = true,
      onEnabled = {},
      onDeleteAll = {},
      onBackClicked = {},
    )
  }

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun HistoryEmptyDisabledTest() =
  ScreenshotTest(showBottomBar = false) {
    History(
      items = emptyList(),
      enabled = false,
      onEnabled = {},
      onDeleteAll = {},
      onBackClicked = {},
    )
  }

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun HistoryPopulatedTest() =
  ScreenshotTest(showBottomBar = false) {
    History(
      items = getHistoryItems(),
      enabled = true,
      onEnabled = {},
      onDeleteAll = {},
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
fun HistoryNightTest() =
  ScreenshotTest(showBottomBar = false) {
    History(
      items = getHistoryItems(),
      enabled = false,
      onEnabled = {},
      onDeleteAll = {},
      onBackClicked = {},
    )
  }

private fun getHistoryItems(now: Long = System.currentTimeMillis()): List<HistoryItem> =
  listOf(
    HistoryItem(
      event =
        InstallEvent(
          time = now - DAYS.toMillis(1),
          packageName = "org.example.app",
          name = "Example App",
          versionName = "2.0.0",
          oldVersionName = "1.9.0",
        ),
      iconModel = R.drawable.ic_repo_app_default,
    ),
    HistoryItem(
      event =
        UninstallEvent(
          time = now - DAYS.toMillis(3),
          packageName = "com.example.oldapp",
          name = "Old App",
        ),
      iconModel = R.drawable.ic_repo_app_default,
    ),
  )
