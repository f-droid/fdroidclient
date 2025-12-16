@file:Suppress("ktlint:standard:filename")

package org.fdroid.ui.apps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.fdroid.database.AppListSortOrder
import org.fdroid.download.NetworkState
import org.fdroid.install.InstallState
import org.fdroid.install.InstallStateWithInfo
import org.fdroid.ui.utils.normalize
import java.text.Collator
import java.util.Locale

// TODO add tests for this, similar to DetailsPresenter
@Composable
fun MyAppsPresenter(
    appUpdatesFlow: StateFlow<List<AppUpdateItem>?>,
    appInstallStatesFlow: StateFlow<Map<String, InstallState>>,
    appsWithIssuesFlow: StateFlow<List<AppWithIssueItem>?>,
    installedAppsFlow: Flow<List<InstalledAppItem>>,
    searchQueryFlow: StateFlow<String>,
    sortOrderFlow: StateFlow<AppListSortOrder>,
    networkStateFlow: StateFlow<NetworkState>,
): MyAppsModel {
    val appUpdates = appUpdatesFlow.collectAsState().value
    val appInstallStates = appInstallStatesFlow.collectAsState().value
    val appsWithIssues = appsWithIssuesFlow.collectAsState().value
    val installedApps = installedAppsFlow.collectAsState(null).value
    val searchQuery = searchQueryFlow.collectAsState().value.normalize()
    val sortOrder = sortOrderFlow.collectAsState().value
    val processedPackageNames = mutableSetOf<String>()

    // we want to show apps currently installing/updating even if they have updates available,
    // so we need to handle those first
    val installingApps = appInstallStates.mapNotNull { (packageName, state) ->
        if (state is InstallStateWithInfo) {
            val keep = searchQuery.isBlank() ||
                state.name.normalize().contains(searchQuery, ignoreCase = true)
            if (keep) {
                processedPackageNames.add(packageName)
                InstallingAppItem(packageName, state)
            } else null
        } else {
            null
        }
    }
    val updates = appUpdates?.filter {
        val keep = if (searchQuery.isBlank()) {
            it.packageName !in processedPackageNames
        } else {
            it.packageName !in processedPackageNames &&
                it.name.normalize().contains(searchQuery, ignoreCase = true)
        }
        if (keep) processedPackageNames.add(it.packageName)
        keep
    }
    val withIssues = appsWithIssues?.filter {
        val keep = if (searchQuery.isBlank()) {
            it.packageName !in processedPackageNames
        } else {
            it.packageName !in processedPackageNames &&
                it.name.normalize().contains(searchQuery, ignoreCase = true)
        }
        if (keep) processedPackageNames.add(it.packageName)
        keep
    }
    val installed = installedApps?.filter {
        if (searchQuery.isBlank()) {
            it.packageName !in processedPackageNames
        } else {
            it.packageName !in processedPackageNames &&
                it.name.normalize().contains(searchQuery, ignoreCase = true)
        }
    }
    var updateBytes: Long? = 0L
    updates?.forEach {
        val size = it.update.size
        if (size == null) {
            // when we don't know the size of one update, we can't provide a total, so say null
            updateBytes = null
            return@forEach
        } else {
            updateBytes = updateBytes?.plus(size)
        }
    } ?: run { updateBytes = null }
    return MyAppsModel(
        installingApps = installingApps.sort(sortOrder),
        appUpdates = updates?.sort(sortOrder),
        appsWithIssue = withIssues?.sort(sortOrder),
        installedApps = installed?.sort(sortOrder),
        sortOrder = sortOrder,
        networkState = networkStateFlow.collectAsState().value,
        appUpdatesBytes = updateBytes,
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
