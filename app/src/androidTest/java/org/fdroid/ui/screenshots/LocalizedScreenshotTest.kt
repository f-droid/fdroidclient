package org.fdroid.ui.screenshots

import android.app.LocaleConfig
import android.os.Build.VERSION.SDK_INT
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.DarkMode
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.Locales
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.then
import androidx.compose.ui.text.intl.LocaleList
import androidx.core.os.LocaleListCompat
import androidx.navigation3.runtime.NavEntry
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

@OptIn(DelicateCoilApi::class)
abstract class LocalizedScreenshotTest(val localeName: String) {
  @get:Rule val composeRule = createComposeRule()

  companion object {
    val locales: List<String> by lazy {
      if (!enabled) return@lazy emptyList()
      val fallback = listOf("en-US", "de-DE", "ar-SA", "he", "zh-CN")
      if (SDK_INT >= 33) {
        val localeConfig = LocaleConfig(InstrumentationRegistry.getInstrumentation().targetContext)
        mutableListOf<String>().apply {
          localeConfig.supportedLocales?.let { localeList ->
            for (i in 0 until localeList.size()) {
              val locale = localeList.get(i)
              add(locale.toLanguageTag())
            }
          } ?: addAll(fallback)
        }
      } else {
        fallback
      }
    }
    val enabled: Boolean
      get() = InstrumentationRegistry.getArguments().getString("fdroid_screenshots") == "true"
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
    assumeTrue(enabled)
  }

  protected fun screenshotTest(
    screenName: String,
    showBottomBar: Boolean = true,
    currentNavKey: NavKey = NavigationKey.Discover,
    numUpdates: Int = 3,
    hasAppIssues: Boolean = true,
    dark: Boolean = false,
    content: @Composable (LocaleListCompat) -> Unit,
  ) {
    val localeList = LocaleList(localeName)
    composeRule.setContent {
      DeviceConfigurationOverride(
        override =
          DeviceConfigurationOverride.Locales(locales = localeList) then
            DeviceConfigurationOverride.DarkMode(dark)
      ) {
        MainContent(
          model =
            MainModel(dynamicColors = false, numUpdates = numUpdates, hasAppIssues = hasAppIssues),
          navEntries =
            listOf(
              NavEntry(currentNavKey) { content(LocaleListCompat.forLanguageTags(localeName)) }
            ),
          directive = PaneScaffoldDirective.Default,
          isBigScreen = false,
          showBottomBar = showBottomBar,
          currentNavKey = currentNavKey,
          onNav = {},
          onBack = {},
        )
      }
    }
    composeRule.waitForIdle()

    val dir = context.getExternalFilesDir("screenshots") ?: fail("Could not create screenshots dir")
    assertTrue(dir.isDirectory)
    val subDir =
      File(dir, "${FLAVOR_variant}/fastlane/metadata/android/$localeName/images/phoneScreenshots")
    subDir.mkdirs()
    assertTrue(subDir.isDirectory)

    val file = File(subDir, "${screenName}.png")
    println("Saving screenshot to ${file.absolutePath}")
    uiDevice.takeScreenshot(file)
  }
}
