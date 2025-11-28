package org.fdroid.updates

import android.content.Context
import android.content.pm.PackageInfo
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import mu.KotlinLogging
import org.fdroid.NotificationManager
import org.fdroid.database.AppVersion
import org.fdroid.database.AvailableAppWithIssue
import org.fdroid.database.DbAppChecker
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.UnavailableAppWithIssue
import org.fdroid.download.getImageModel
import org.fdroid.index.RepoManager
import org.fdroid.install.AppInstallManager
import org.fdroid.install.InstalledAppsCache
import org.fdroid.repo.RepoUpdateWorker
import org.fdroid.settings.SettingsManager
import org.fdroid.ui.apps.AppUpdateItem
import org.fdroid.ui.apps.AppWithIssueItem
import org.fdroid.utils.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class UpdatesManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val db: FDroidDatabase,
    private val dbAppChecker: DbAppChecker,
    private val settingsManager: SettingsManager,
    private val repoManager: RepoManager,
    private val appInstallManager: AppInstallManager,
    private val installedAppsCache: InstalledAppsCache,
    private val notificationManager: NotificationManager,
    @param:IoDispatcher private val coroutineScope: CoroutineScope,
) {
    private val log = KotlinLogging.logger { }

    private val _updates = MutableStateFlow<List<AppUpdateItem>?>(null)
    val updates = _updates.asStateFlow()
    private val _appsWithIssues = MutableStateFlow<List<AppWithIssueItem>?>(null)
    val appsWithIssues = _appsWithIssues.asStateFlow()
    private val _numUpdates = MutableStateFlow(0)
    val numUpdates = _numUpdates.asStateFlow()

    /**
     * The time in milliseconds of the (earliest!) next repository update run.
     * This is [Long.MAX_VALUE], if no time is known.
     */
    val nextRepoUpdateFlow = RepoUpdateWorker.getAutoUpdateWorkInfo(context).map { workInfo ->
        workInfo?.nextScheduleTimeMillis ?: Long.MAX_VALUE
    }

    /**
     * The time in milliseconds of the (earliest!) next automatic app update run.
     * This is [Long.MAX_VALUE], if no time is known.
     */
    val nextAppUpdateFlow = AppUpdateWorker.getAutoUpdateWorkInfo(context).map { workInfo ->
        workInfo?.nextScheduleTimeMillis ?: Long.MAX_VALUE
    }

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
        coroutineScope.launch {
            // refresh updates whenever installed apps change
            installedAppsCache.installedApps.collect {
                loadUpdates(it)
            }
        }
    }

    fun loadUpdates(
        packageInfoMap: Map<String, PackageInfo> = installedAppsCache.installedApps.value,
    ) = coroutineScope.launch {
        if (packageInfoMap.isEmpty()) return@launch
        val localeList = LocaleListCompat.getDefault()
        try {
            log.info { "Checking for updates (${packageInfoMap.size} apps)..." }
            val proxyConfig = settingsManager.proxyConfig
            val apps = dbAppChecker.getApps(packageInfoMap = packageInfoMap)
            val updates = apps.updates.map { update ->
                AppUpdateItem(
                    repoId = update.repoId,
                    packageName = update.packageName,
                    name = update.name ?: "Unknown app",
                    installedVersionName = update.installedVersionName,
                    update = update.update,
                    whatsNew = update.update.getWhatsNew(localeList),
                    iconModel = repoManager.getRepository(update.repoId)?.let { repo ->
                        update.getIcon(localeList)?.getImageModel(repo, proxyConfig)
                    },
                )
            }
            _updates.value = updates
            _numUpdates.value = updates.size
            // update 'update available' notification, if it is currently showing
            if (notificationManager.isAppUpdatesAvailableNotificationShowing) {
                if (updates.isEmpty()) notificationManager.cancelAppUpdatesAvailableNotification()
                else notificationManager.showAppUpdatesAvailableNotification(notificationStates)
            }

            val issueItems = apps.issues.map { app ->
                when (app) {
                    is AvailableAppWithIssue -> AppWithIssueItem(
                        packageName = app.app.packageName,
                        name = app.app.getName(localeList) ?: "Unknown app",
                        installedVersionName = app.installVersionName,
                        issue = app.issue,
                        lastUpdated = app.app.lastUpdated,
                        iconModel = PackageName(
                            packageName = app.app.packageName,
                            iconDownloadRequest = repoManager.getRepository(app.app.repoId)?.let {
                                app.app.getIcon(localeList)?.getImageModel(it, proxyConfig)
                            } as? DownloadRequest),
                    )
                    is UnavailableAppWithIssue -> AppWithIssueItem(
                        packageName = app.packageName,
                        name = app.name.toString(),
                        installedVersionName = app.installVersionName,
                        issue = app.issue,
                        lastUpdated = -1,
                        iconModel = PackageName(app.packageName, null),
                    )
                }
            }
            _appsWithIssues.value = issueItems
        } catch (e: Exception) {
            log.error(e) { "Error loading updates: " }
            return@launch
        }
    }

    suspend fun updateAll(): List<Job> {
        val appsToUpdate = updates.value ?: updates.first() ?: return emptyList()
        // we could do more in-depth checks regarding pre-approval, but this also works
        val canAskPreApprovalNow = appsToUpdate.size == 1
        val concurrencyLimit = min(Runtime.getRuntime().availableProcessors(), 8)
        val semaphore = Semaphore(concurrencyLimit)
        return appsToUpdate.map { update ->
            currentCoroutineContext().ensureActive()
            // launch a new co-routine for each app to update
            coroutineScope.launch {
                // suspend here until we get a permit from the semaphore (there's free workers)
                semaphore.withPermit {
                    currentCoroutineContext().ensureActive()
                    updateApp(update, canAskPreApprovalNow)
                }
            }
        }
    }

    private suspend fun updateApp(update: AppUpdateItem, canAskPreApprovalNow: Boolean) {
        val app = db.getAppDao().getApp(update.repoId, update.packageName) ?: return
        appInstallManager.install(
            appMetadata = app.metadata,
            // we know this is true, because we set this above in loadUpdates()
            version = update.update as AppVersion,
            currentVersionName = update.installedVersionName,
            repo = repoManager.getRepository(update.repoId) ?: return,
            iconModel = update.iconModel,
            canAskPreApprovalNow = canAskPreApprovalNow,
        )
    }
}
