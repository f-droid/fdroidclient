@file:Suppress("ktlint:standard:filename")

package org.fdroid.ui.apps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow
import org.fdroid.database.AppListSortOrder
import org.fdroid.install.InstallState
import org.fdroid.install.InstallStateWithInfo
import org.fdroid.ui.utils.normalize
import java.text.Collator
import java.util.Locale

@Composable
fun MyAppsPresenter(
    appUpdatesFlow: StateFlow<List<AppUpdateItem>?>,
    appInstallStatesFlow: StateFlow<Map<String, InstallState>>,
    installedAppsFlow: StateFlow<List<InstalledAppItem>?>,
    searchQueryFlow: StateFlow<String>,
    sortOrderFlow: StateFlow<AppListSortOrder>,
): MyAppsModel {
    val appUpdates = appUpdatesFlow.collectAsState().value
    val appInstallStates = appInstallStatesFlow.collectAsState().value
    val installedApps = installedAppsFlow.collectAsState().value
    val searchQuery = searchQueryFlow.collectAsState().value.normalize()
    val sortOrder = sortOrderFlow.collectAsState().value
    val processedPackageNames = mutableSetOf<String>()

    val updates = appUpdates?.filter {
        val keep = searchQuery.isBlank() ||
            it.name.normalize().contains(searchQuery, ignoreCase = true)
        if (keep) processedPackageNames.add(it.packageName)
        keep
    }
    val installingApps = appInstallStates.mapNotNull { (packageName, state) ->
        if (state is InstallStateWithInfo) {
            val keep = if (searchQuery.isBlank()) {
                packageName !in processedPackageNames
            } else {
                packageName !in processedPackageNames &&
                    state.name.normalize().contains(searchQuery, ignoreCase = true)
            }
            processedPackageNames.add(packageName)
            if (keep) InstallingAppItem(packageName, state) else null
        } else {
            null
        }
    }
    val installed = installedApps?.filter {
        if (searchQuery.isBlank()) {
            it.packageName !in processedPackageNames
        } else {
            it.packageName !in processedPackageNames &&
                it.name.normalize().contains(searchQuery, ignoreCase = true)
        }
    }
    return MyAppsModel(
        installingApps = installingApps.sort(sortOrder),
        appUpdates = updates?.sort(sortOrder),
        installedApps = installed?.sort(sortOrder),
        sortOrder = sortOrder,
    )
}

private fun <T : MyAppItem> List<T>.sort(sortOrder: AppListSortOrder): List<T> {
    val collator = Collator.getInstance(Locale.getDefault())
    return when (sortOrder) {
        AppListSortOrder.NAME -> sortedWith { a1, a2 ->
            // storing collator.getCollationKey() and using that could be an optimization
            collator.compare(a1.name, a2.name)
        }
        AppListSortOrder.LAST_UPDATED -> sortedByDescending { it.lastUpdated }
    }
}
