package org.fdroid.ui.lists

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.fdroid.database.AppListSortOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests the search filtering that [AppListViewModel.onSearch] drives. Rather than constructing the
 * full ViewModel (which requires Hilt + Molecule's AndroidUiDispatcher), we drive
 * [AppListPresenter] directly via [moleculeFlow], which is the exact code path that
 * [AppListViewModel.onSearch] triggers.
 *
 * This is the in-memory [String.contains]-based counterpart of AppSearchItemsTest, which covers the
 * DB FTS4 path.
 *
 * [AppListPresenter] uses [org.fdroid.search.SearchHelper.fixQuery] on the incoming query before
 * matching, which:
 * - strips diacritics via NFKD normalization (enables diacritic-insensitive search), and
 * - inserts zero-width spaces (U+200B) after each ideographic character (enables CJK matching
 *   against names/summaries that were stored with those same zero-width spaces by App.zero()).
 */
@RunWith(RobolectricTestRunner::class)
internal class AppListSearchTest {

  private val query = MutableStateFlow("")
  private val presenterFlow: Flow<AppListModel> =
    moleculeFlow(RecompositionMode.Immediate) {
      AppListPresenter(
        type = AppListType.All("All"),
        appsFlow = MutableStateFlow(apps),
        sortByFlow = MutableStateFlow(AppListSortOrder.LAST_UPDATED),
        filterIncompatibleFlow = MutableStateFlow(false),
        categoriesFlow = flowOf(emptyList()),
        antiFeaturesFlow = flowOf(emptyList()),
        filteredCategoryIdsFlow = MutableStateFlow(emptySet()),
        notSelectedAntiFeatureIdsFlow = MutableStateFlow(emptySet()),
        repositoriesFlow = flowOf(emptyList()),
        filteredRepositoryIdsFlow = MutableStateFlow(emptySet()),
        searchQueryFlow = query,
      )
    }

  private val apps =
    listOf(
      // English
      appItem(
        packageName = "org.fdroid.fdroid",
        name = "F-Droid",
        summary = "The app store that respects freedom and privacy",
      ),
      appItem(
        packageName = "com.aurora.store",
        name = "Aurora Store",
        summary = "An unofficial FOSS client to Google Play with an elegant design and privacy",
      ),
      appItem(
        "com.github.libretube",
        name = "LibreTube",
        summary = "Alternative frontend for YouTube with focus on privacy",
      ),
      // German needs diacritic normalization on both query and item
      appItem(
        "com.duckduckgo.mobile.android",
        name = "DuckDuckGo Privacy Browser",
        summary = "Privatsphäre vereinfacht",
      ),
      // Portuguese needs diacritic normalization (ã, ç, etc.)
      appItem(
        "at.bitfire.davdroid",
        name = "DAVx5",
        summary = "Sincronização e cliente de CalDAV/CardDAV",
      ),
      // Japanese CJK kanji are ideographic, so zero-width spaces are needed
      appItem(
        packageName = "com.junkfood.seal",
        name = "Seal",
        summary = "Material You デザインの動画・音声ダウンローダー".zero(),
      ),
      // Korean Hangul is NOT ideographic, no zero-width spaces needed
      appItem(
        packageName = "com.nextcloud.android.beta",
        name = "Nextcloud Dev",
        summary = "동기화 클라이언트",
      ),
      // Simplified Chinese, all CJK characters get zero-width spaces
      appItem(
        packageName = "app.organicmaps",
        name = "Organic Maps・离线地图与导航 & GPS".zero(),
        summary = "获取可靠的离线地图及 GPS 导航，用于徒步、骑行和旅行。".zero(),
      ),
      // Traditional Chinese
      appItem(
        packageName = "com.capyreader.app",
        name = "Capy Reader",
        summary = "小型 RSS 閱讀器".zero(),
      ),
    )

  @Test
  fun findsByName() = runTest {
    presenterFlow.test {
      awaitInitialItems()

      query.value = "F-Droid"
      assertContains(awaitItem(), "org.fdroid.fdroid")

      query.value = "LibreTube"
      assertContains(awaitItem(), "com.github.libretube")

      query.value = "Aurora Store"
      assertContains(awaitItem(), "com.aurora.store")

      ensureAllEventsConsumed()
    }
  }

  @Test
  fun findsBySummary() = runTest {
    presenterFlow.test {
      awaitInitialItems()

      query.value = "Alternative frontend"
      assertContains(awaitItem(), "com.github.libretube")

      query.value = "freedom and privacy"
      assertContains(awaitItem(), "org.fdroid.fdroid")

      ensureAllEventsConsumed()
    }
  }

  @Test
  fun findsByPackageName() = runTest {
    presenterFlow.test {
      awaitInitialItems()

      query.value = "org.fdroid.fdroid"
      assertContains(awaitItem(), "org.fdroid.fdroid")

      query.value = "com.aurora"
      assertContains(awaitItem(), "com.aurora.store")

      query.value = "capyreader"
      assertContains(awaitItem(), "com.capyreader.app")

      ensureAllEventsConsumed()
    }
  }

  @Test
  fun findsGermanText() = runTest {
    presenterFlow.test {
      awaitInitialItems()

      // query diacritics normalized: "Privatsphäre" -> "Privatsphare", matches normalized summary
      query.value = "Privatsphäre"
      assertContains(awaitItem(), "com.duckduckgo.mobile.android")

      // query without diacritics also matches because summary is equally normalized
      query.value = "Privatsphare vereinfacht"
      assertContains(awaitItem(), "com.duckduckgo.mobile.android")

      ensureAllEventsConsumed()
    }
  }

  @Test
  fun findsPortugueseText() = runTest {
    presenterFlow.test {
      awaitInitialItems()

      // query without diacritics matches normalized summary
      query.value = "Sincronizacao"
      assertContains(awaitItem(), "at.bitfire.davdroid")

      // query with diacritics is also normalized before matching
      query.value = "Sincronização"
      assertContains(awaitItem(), "at.bitfire.davdroid")

      ensureAllEventsConsumed()
    }
  }

  @Test
  fun findsSimplifiedChineseText() = runTest {
    presenterFlow.test {
      awaitInitialItems()

      query.value = "地图"
      assertContains(awaitItem(), "app.organicmaps")

      query.value = "导航"
      assertContains(awaitItem(), "app.organicmaps")

      ensureAllEventsConsumed()
    }
  }

  @Test
  fun findsTraditionalChineseText() = runTest {
    presenterFlow.test {
      awaitInitialItems()

      query.value = "閱讀"
      assertContains(awaitItem(), "com.capyreader.app")

      ensureAllEventsConsumed()
    }
  }

  @Test
  fun findsJapaneseText() = runTest {
    presenterFlow.test {
      awaitInitialItems()

      query.value = "動画"
      assertContains(awaitItem(), "com.junkfood.seal")

      ensureAllEventsConsumed()
    }
  }

  @Test
  fun findsKoreanText() = runTest {
    presenterFlow.test {
      awaitInitialItems()

      query.value = "동기화"
      assertContains(awaitItem(), "com.nextcloud.android.beta")

      ensureAllEventsConsumed()
    }
  }

  @Test
  fun noMatchReturnsEmptyList() = runTest {
    presenterFlow.test {
      awaitInitialItems()

      query.value = "zzznomatch"
      val result = awaitItem()
      assertNotNull(result.apps)
      assertTrue(result.apps.isEmpty(), "No-match query should return empty list")

      ensureAllEventsConsumed()
    }
  }

  // Helpers

  private suspend fun ReceiveTurbine<AppListModel>.awaitInitialItems() {
    // first the apps are null, because that's the default value of the StateFlow
    // before the presenter emits the first model with the actual list
    assertNull(awaitItem().apps)
    // then we get the first model with the entire unfiltered apps list
    val unfilteredModel = awaitItem()
    assertEquals(
      expected = apps.map { it.packageName }.toSet(),
      actual = unfilteredModel.apps?.map { it.packageName }?.toSet(),
    )
  }

  private fun assertContains(model: AppListModel, packageName: String) {
    val apps = model.apps
    assertNotNull(apps, "apps list must not be null")
    assertTrue(
      apps.any { it.packageName == packageName },
      "Expected '$packageName' in results but got: ${apps.map { it.packageName }}",
    )
  }

  private fun appItem(packageName: String, name: String, summary: String = "") =
    AppListItem(
      repoId = 1L,
      packageName = packageName,
      name = name,
      summary = summary,
      lastUpdated = 23L,
      isInstalled = false,
      isCompatible = true,
    )

  /**
   * Mirrors the internal `App.zero()` DB helper: inserts a zero-width space (U+200B) after every
   * ideographic character that is not the last character in the string. This is applied to CJK
   * name/summary strings in the test data to match what the DB stores in `localizedName` (populated
   * from the zero()-processed metadata name/summary at index time).
   */
  private fun String.zero(): String = buildString {
    this@zero.forEachIndexed { i, char ->
      if (Character.isIdeographic(char.code) && i + 1 < this@zero.length) {
        append(char)
        append('\u200B')
      } else {
        append(char)
      }
    }
  }
}
