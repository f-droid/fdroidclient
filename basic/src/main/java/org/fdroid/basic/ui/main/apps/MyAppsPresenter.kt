package org.fdroid.basic.ui.main.apps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow
import org.fdroid.basic.manager.AppUpdateItem
import org.fdroid.database.AppListSortOrder
import java.text.Collator
import java.util.Locale

@Composable
fun MyAppsPresenter(
    appUpdatesFlow: StateFlow<List<AppUpdateItem>?>,
    installedAppsFlow: StateFlow<List<InstalledAppItem>?>,
    sortOrderFlow: StateFlow<AppListSortOrder>,
): MyAppsModel {
    val appUpdates = appUpdatesFlow.collectAsState().value
    val installedApps = installedAppsFlow.collectAsState().value
    val sortOrder = sortOrderFlow.collectAsState().value
    val packageNames = appUpdates?.map { it.packageName } ?: emptyList()
    val collator = Collator.getInstance(Locale.getDefault())
    return MyAppsModel(
        appUpdates = when (sortOrder) {
            AppListSortOrder.NAME -> appUpdates?.sortedWith { a1, a2 -> collator.compare(a1.name, a2.name) }
            AppListSortOrder.LAST_UPDATED -> appUpdates?.sortedByDescending { it.update.added }
        },
        installedApps = installedApps?.filter {
            // filter out apps already in updates
            it.packageName !in packageNames
        }?.let { apps ->
            when (sortOrder) {
                AppListSortOrder.NAME -> apps.sortedWith { a1, a2 -> collator.compare(a1.name, a2.name) }
                AppListSortOrder.LAST_UPDATED -> apps.sortedByDescending { it.lastUpdated }
            }
        },
        sortOrder = sortOrder,
    )
}

data class MyAppsModel(
    val appUpdates: List<AppUpdateItem>? = null,
    val installedApps: List<InstalledAppItem>? = null,
    val sortOrder: AppListSortOrder = AppListSortOrder.NAME,
)
