package org.fdroid.ui.lists

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.fdroid.database.AppListSortOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AppListSortTest {

  private val apps =
    listOf(
      // added first, but updated last
      appItem(packageName = "a", added = 1L, lastUpdated = 30L),
      appItem(packageName = "b", added = 3L, lastUpdated = 10L),
      appItem(packageName = "c", added = 2L, lastUpdated = 20L),
    )

  @Test
  fun testNewAppsSortedByAdded() = runTest {
    val model =
      getModel(AppListType.New("New"), AppListSortOrder.LAST_UPDATED).first { it.apps != null }
    assertEquals(listOf("b", "c", "a"), model.apps?.map { it.packageName })
  }

  @Test
  fun testOtherListsSortedByLastUpdated() = runTest {
    val model =
      getModel(AppListType.All("All"), AppListSortOrder.LAST_UPDATED).first { it.apps != null }
    assertEquals(listOf("a", "c", "b"), model.apps?.map { it.packageName })
  }

  @Test
  fun testNewAppsSortedByName() = runTest {
    val model = getModel(AppListType.New("New"), AppListSortOrder.NAME).first { it.apps != null }
    assertEquals(listOf("a", "b", "c"), model.apps?.map { it.packageName })
  }

  private fun getModel(type: AppListType, sortBy: AppListSortOrder): Flow<AppListModel> =
    moleculeFlow(RecompositionMode.Immediate) {
      AppListPresenter(
        type = type,
        appsFlow = MutableStateFlow(apps),
        sortByFlow = MutableStateFlow(sortBy),
        filterIncompatibleFlow = MutableStateFlow(false),
        categoriesFlow = flowOf(emptyList()),
        antiFeaturesFlow = flowOf(emptyList()),
        filteredCategoryIdsFlow = MutableStateFlow(emptySet()),
        notSelectedAntiFeatureIdsFlow = MutableStateFlow(emptySet()),
        repositoriesFlow = flowOf(emptyList()),
        filteredRepositoryIdsFlow = MutableStateFlow(emptySet()),
        searchQueryFlow = MutableStateFlow(""),
      )
    }

  private fun appItem(packageName: String, added: Long, lastUpdated: Long) =
    AppListItem(
      repoId = 1L,
      packageName = packageName,
      name = packageName,
      summary = "",
      lastUpdated = lastUpdated,
      isInstalled = false,
      isCompatible = true,
      added = added,
    )
}
