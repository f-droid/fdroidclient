package org.fdroid.updates

import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.fdroid.database.DbUpdateChecker
import org.fdroid.download.getDownloadRequest
import org.fdroid.index.RepoManager
import org.fdroid.ui.apps.AppUpdateItem
import org.fdroid.utils.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdatesManager @Inject constructor(
    private val dbUpdateChecker: DbUpdateChecker,
    @IoDispatcher private val coroutineScope: CoroutineScope,
    private val repoManager: RepoManager,
) {
    private val log = KotlinLogging.logger { }

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
        val updates = try {
            log.info { "Checking for updates..." }
            dbUpdateChecker.getUpdatableApps(onlyFromPreferredRepo = true).map { update ->
                AppUpdateItem(
                    packageName = update.packageName,
                    name = update.name ?: "Unknown app",
                    installedVersionName = update.installedVersionName,
                    update = update.update,
                    whatsNew = update.update.getWhatsNew(localeList),
                    iconDownloadRequest = repoManager.getRepository(update.repoId)?.let { repo ->
                        update.getIcon(localeList)?.getDownloadRequest(repo)
                    },
                )
            }
        } catch (e: Exception) {
            log.error(e) { "Error loading updates: " }
            return@launch
        }
        _updates.value = updates
        _numUpdates.value = updates.size
    }
}
