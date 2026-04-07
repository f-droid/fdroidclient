package org.fdroid.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true, heightDp = 1200)
fun AboutTest() = ScreenshotTest(showBottomBar = false) { About("2.0.0-beta1") {} }

@Composable
@PreviewTest
@Preview(
  showBackground = true,
  showSystemUi = true,
  uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
  heightDp = 1200,
)
fun AboutNightTest() = ScreenshotTest(showBottomBar = false) { About("2.0.0-beta1") {} }
