package org.fdroid.ui.screenshots

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.Locales
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.intl.LocaleList
import androidx.navigation3.runtime.NavKey
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.fail
import org.fdroid.BuildConfig.FLAVOR_variant
import org.fdroid.ui.MainContent
import org.fdroid.ui.MainModel
import org.fdroid.ui.navigation.NavigationKey
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule

private const val ENABLED = false

@OptIn(DelicateCoilApi::class)
abstract class LocalizedScreenshotTest(val localeName: String) {
  @get:Rule val composeRule = createComposeRule()

  companion object {
    val locales = listOf("en-US", "de-DE", "ar-SA", "he", "zh-CN")
  }

  private val context = InstrumentationRegistry.getInstrumentation().targetContext
  private val idlingEventListener = IdlingEventListener()
  private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

  init {
    val imageLoader =
      SingletonImageLoader.get(context).newBuilder().eventListener(idlingEventListener).build()
    SingletonImageLoader.setUnsafe(imageLoader)
    composeRule.registerIdlingResource(idlingEventListener)
  }

  @Before
  fun before() {
    assumeTrue(ENABLED)
  }

  protected fun screenshotTest(
    screenName: String,
    showBottomBar: Boolean = true,
    currentNavKey: NavKey = NavigationKey.Discover,
    numUpdates: Int = 3,
    hasAppIssues: Boolean = true,
    content: @Composable (Modifier) -> Unit,
  ) {
    val localeList = LocaleList(localeName)
    composeRule.setContent {
      DeviceConfigurationOverride(
        override = DeviceConfigurationOverride.Locales(locales = localeList)
      ) {
        MainContent(
          model =
            MainModel(
              dynamicColors = false,
              smallBottomBar = false,
              numUpdates = numUpdates,
              hasAppIssues = hasAppIssues,
            ),
          isBigScreen = false,
          showBottomBar = showBottomBar,
          currentNavKey = currentNavKey,
          onNav = {},
          content = content,
        )
      }
    }
    composeRule.waitForIdle()

    val dir = context.getExternalFilesDir("screenshots") ?: fail("Could not create screenshots dir")
    assertTrue(dir.isDirectory)
    val subDir =
      File(dir, "${FLAVOR_variant}/fastlane/metadata/$localeName/images/phoneScreenshots")
    subDir.mkdirs()
    assertTrue(subDir.isDirectory)

    val file = File(subDir, "${screenName}.png")
    uiDevice.takeScreenshot(file)
  }
}
