@file:Suppress("ktlint:standard:filename")

package org.fdroid.ui.apps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow
import org.fdroid.database.AppListSortOrder
import org.fdroid.ui.utils.normalize
import java.text.Collator
import java.util.Locale

@Composable
fun MyAppsPresenter(
    appUpdatesFlow: StateFlow<List<AppUpdateItem>?>,
    installedAppsFlow: StateFlow<List<InstalledAppItem>?>,
    searchQueryFlow: StateFlow<String>,
    sortOrderFlow: StateFlow<AppListSortOrder>,
): MyAppsModel {
    val appUpdates = appUpdatesFlow.collectAsState().value
    val installedApps = installedAppsFlow.collectAsState().value
    val searchQuery = searchQueryFlow.collectAsState().value.normalize()
    val sortOrder = sortOrderFlow.collectAsState().value
    val packageNames = appUpdates?.map { it.packageName } ?: emptyList()
    val collator = Collator.getInstance(Locale.getDefault())

    val updates = if (searchQuery.isBlank()) appUpdates else appUpdates?.filter {
        it.name.normalize().contains(searchQuery, ignoreCase = true)
    }
    val installed = if (searchQuery.isBlank()) installedApps else installedApps?.filter {
        it.name.normalize().contains(searchQuery, ignoreCase = true)
    }
    return MyAppsModel(
        appUpdates = when (sortOrder) {
            AppListSortOrder.NAME -> updates?.sortedWith { a1, a2 ->
                // storing collator.getCollationKey() and using that could be an optimization
                collator.compare(a1.name, a2.name)
            }
            AppListSortOrder.LAST_UPDATED -> updates?.sortedByDescending { it.update.added }
        },
        installedApps = installed?.filter {
            // filter out apps already in updates
            it.packageName !in packageNames
        }?.let { apps ->
            when (sortOrder) {
                AppListSortOrder.NAME -> apps.sortedWith { a1, a2 ->
                    // storing collator.getCollationKey() and using that could be an optimization
                    collator.compare(a1.name, a2.name)
                }
                AppListSortOrder.LAST_UPDATED -> apps.sortedByDescending { it.lastUpdated }
            }
        },
        sortOrder = sortOrder,
    )
}
