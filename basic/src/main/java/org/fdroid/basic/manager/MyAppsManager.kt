package org.fdroid.basic.manager

import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.fdroid.basic.download.getDownloadRequest
import org.fdroid.basic.repo.RepositoryManager
import org.fdroid.basic.utils.IoDispatcher
import org.fdroid.database.DbUpdateChecker
import org.fdroid.download.DownloadRequest
import org.fdroid.index.v2.PackageVersion
import javax.inject.Inject
import javax.inject.Singleton

data class AppUpdateItem(
    val packageName: String,
    val name: String,
    val installedVersionName: String,
    val update: PackageVersion,
    val whatsNew: String?,
    val iconDownloadRequest: DownloadRequest? = null,
)

@Singleton
class MyAppsManager @Inject constructor(
    private val dbUpdateChecker: DbUpdateChecker,
    @IoDispatcher private val coroutineScope: CoroutineScope,
    private val repositoryManager: RepositoryManager,
) {
    private val _updates = MutableStateFlow<List<AppUpdateItem>?>(null)
    val updates = _updates.asStateFlow()
    private val _numUpdates = MutableStateFlow(0)
    val numUpdates = _numUpdates.asStateFlow()

    init {
        loadUpdates()
    }

    fun loadUpdates() = coroutineScope.launch {
        // TODO (includeKnownVulnerabilities = true) and show in AppDetails
        val localeList = LocaleListCompat.getDefault()
        val updates = dbUpdateChecker.getUpdatableApps(onlyFromPreferredRepo = true).map { update ->
            AppUpdateItem(
                packageName = update.packageName,
                name = update.name ?: "Unknown app",
                installedVersionName = update.installedVersionName,
                update = update.update,
                whatsNew = update.update.getWhatsNew(localeList),
                iconDownloadRequest = repositoryManager.getRepository(update.repoId)?.let { repo ->
                    update.getIcon(localeList)?.getDownloadRequest(repo)
                },
            )
        }
        _updates.value = updates
        _numUpdates.value = updates.size
    }
}
