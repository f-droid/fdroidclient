package org.fdroid.updates

import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import mu.KotlinLogging
import org.fdroid.database.AppVersion
import org.fdroid.database.DbUpdateChecker
import org.fdroid.database.FDroidDatabase
import org.fdroid.download.getImageModel
import org.fdroid.index.RepoManager
import org.fdroid.install.AppInstallManager
import org.fdroid.ui.apps.AppUpdateItem
import org.fdroid.utils.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class UpdatesManager @Inject constructor(
    private val db: FDroidDatabase,
    private val dbUpdateChecker: DbUpdateChecker,
    private val repoManager: RepoManager,
    private val appInstallManager: AppInstallManager,
    @param:IoDispatcher private val coroutineScope: CoroutineScope,
) {
    private val log = KotlinLogging.logger { }

    private val _updates = MutableStateFlow<List<AppUpdateItem>?>(null)
    val updates = _updates.asStateFlow()
    private val _numUpdates = MutableStateFlow(0)
    val numUpdates = _numUpdates.asStateFlow()

    val notificationStates: UpdateNotificationState
        get() = UpdateNotificationState(
            updates = updates.value?.map { update ->
                AppUpdate(
                    packageName = update.packageName,
                    name = update.name,
                    currentVersionName = update.installedVersionName,
                    updateVersionName = update.update.versionName,
                )
            } ?: emptyList()
        )

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
                    repoId = update.repoId,
                    packageName = update.packageName,
                    name = update.name ?: "Unknown app",
                    installedVersionName = update.installedVersionName,
                    update = update.update,
                    whatsNew = update.update.getWhatsNew(localeList),
                    iconModel = repoManager.getRepository(update.repoId)?.let { repo ->
                        update.getIcon(localeList)?.getImageModel(repo)
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

    suspend fun updateAll(): List<Job> {
        val appsToUpdate = updates.value ?: updates.first() ?: return emptyList()
        val concurrencyLimit = min(Runtime.getRuntime().availableProcessors(), 8)
        val semaphore = Semaphore(concurrencyLimit)
        return appsToUpdate.map { update ->
            // launch a new co-routine for each app to update
            coroutineScope.launch {
                // suspend here until we get a permit from the semaphore (there's free workers)
                semaphore.withPermit {
                    updateApp(update)
                }
            }
        }
    }

    private suspend fun updateApp(update: AppUpdateItem) {
        val app = db.getAppDao().getApp(update.repoId, update.packageName) ?: return
        appInstallManager.install(
            appMetadata = app.metadata,
            // we know this is true, because we set this above in loadUpdates()
            version = update.update as AppVersion,
            currentVersionName = update.installedVersionName,
            repo = repoManager.getRepository(update.repoId) ?: return,
            iconModel = update.iconModel
        )
    }
}
