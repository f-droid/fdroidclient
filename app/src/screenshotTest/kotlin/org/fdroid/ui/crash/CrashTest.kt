package org.fdroid.ui.crash

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import org.fdroid.ui.ScreenshotTest

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun CrashTest() =
  ScreenshotTest(showBottomBar = false) {
    Crash(isOldCrash = false, onCancel = {}, onSend = { _, _ -> }, onSave = { _, _ -> true })
  }

@Composable
@PreviewTest
@Preview(
  showBackground = true,
  showSystemUi = true,
  uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
fun CrashNightTest() =
  ScreenshotTest(showBottomBar = false) {
    Crash(isOldCrash = true, onCancel = {}, onSend = { _, _ -> }, onSave = { _, _ -> true })
  }
