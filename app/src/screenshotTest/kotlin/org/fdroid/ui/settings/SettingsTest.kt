package org.fdroid.ui.settings

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import java.lang.System.currentTimeMillis
import java.util.concurrent.TimeUnit.HOURS
import kotlinx.coroutines.flow.MutableStateFlow
import me.zhanghai.compose.preference.MapPreferences
import org.fdroid.ui.ScreenshotTest

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true, heightDp = 1400)
fun SettingsTest() =
  ScreenshotTest(showBottomBar = false) {
    Settings(model = getSettingsModel(), isBigScreen = false, onSaveLogcat = {}, onBackClicked = {})
  }

@Composable
@PreviewTest
@Preview(
  showBackground = true,
  showSystemUi = true,
  uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
  heightDp = 1400,
)
fun SettingsNightTest() =
  ScreenshotTest(showBottomBar = false) {
    Settings(model = getSettingsModel(), isBigScreen = false, onSaveLogcat = {}, onBackClicked = {})
  }

private fun getSettingsModel(
  nextRepoUpdate: Long = currentTimeMillis() - HOURS.toMillis(12),
  nextAppUpdate: Long = Long.MAX_VALUE,
) =
  SettingsModel(
    prefsFlow = MutableStateFlow(MapPreferences(emptyMap())),
    nextRepoUpdateFlow = MutableStateFlow(nextRepoUpdate),
    nextAppUpdateFlow = MutableStateFlow(nextAppUpdate),
  )
