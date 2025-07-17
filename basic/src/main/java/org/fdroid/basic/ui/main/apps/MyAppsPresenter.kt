package org.fdroid.basic.ui.main.apps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow
import org.fdroid.basic.manager.AppUpdateItem
import org.fdroid.basic.ui.main.lists.Sort
import java.text.Collator
import java.util.Locale

@Composable
fun MyAppsPresenter(
    appUpdatesFlow: StateFlow<List<AppUpdateItem>?>,
    installedAppsFlow: StateFlow<List<InstalledAppItem>?>,
    sortOrderFlow: StateFlow<Sort>,
): MyAppsModel {
    val appUpdates = appUpdatesFlow.collectAsState().value
    val installedApps = installedAppsFlow.collectAsState().value
    val sortOrder = sortOrderFlow.collectAsState().value
    val packageNames = appUpdates?.map { it.packageName } ?: emptyList()
    val collator = Collator.getInstance(Locale.getDefault())
    return MyAppsModel(
        appUpdates = when (sortOrder) {
            Sort.NAME -> appUpdates?.sortedWith { a1, a2 -> collator.compare(a1.name, a2.name) }
            Sort.LATEST -> appUpdates?.sortedByDescending { it.update.added }
        },
        installedApps = installedApps?.filter {
            // filter out apps already in updates
            it.packageName !in packageNames
        }?.let { apps ->
            when (sortOrder) {
                Sort.NAME -> apps.sortedWith { a1, a2 -> collator.compare(a1.name, a2.name) }
                Sort.LATEST -> apps.sortedByDescending { it.lastUpdated }
            }
        },
        sortOrder = sortOrder,
    )
}

data class MyAppsModel(
    val appUpdates: List<AppUpdateItem>? = null,
    val installedApps: List<InstalledAppItem>? = null,
    val sortOrder: Sort = Sort.NAME,
)
