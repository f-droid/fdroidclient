package org.fdroid.fdroid

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import mu.KotlinLogging
import org.acra.util.versionCodeLong
import org.fdroid.database.DbUpdateChecker
import org.fdroid.database.Repository
import org.fdroid.database.UpdatableApp
import org.fdroid.download.DownloaderFactory
import org.fdroid.fdroid.AppUpdateStatusManager.Status.Downloading
import org.fdroid.fdroid.data.Apk
import org.fdroid.fdroid.data.App
import org.fdroid.fdroid.installer.ApkCache
import org.fdroid.fdroid.installer.InstallManagerService
import org.fdroid.fdroid.installer.InstallerFactory
import org.fdroid.index.RepoManager

class AppUpdateManager @JvmOverloads constructor(
    private val context: Context,
    private val repoManager: RepoManager,
    private val updateChecker: DbUpdateChecker,
    private val downloaderFactory: DownloaderFactory =
        org.fdroid.fdroid.net.DownloaderFactory.INSTANCE,
    private val statusManager: AppUpdateStatusManager = AppUpdateStatusManager.getInstance(context),
) {

    private val log = KotlinLogging.logger { }

    /**
     * Returns true if all apps were updates successfully.
     */
    fun updateApps(): Boolean {
        // get apps with updates pending
        val releaseChannels = Preferences.get().backendReleaseChannels
        val updatableApps = updateChecker.getUpdatableApps(releaseChannels, true)
            .sortedWith { app1, app2 ->
                // our own app will be last to update
                if (app1.packageName == context.packageName) return@sortedWith 1
                if (app2.packageName == context.packageName) return@sortedWith -1
                // other apps are sorted by name
                (app1.name ?: "").compareTo(app2.name ?: "", ignoreCase = true)
            }
        // inform the status manager of the available updates
        statusManager.addUpdatableApps(updatableApps, false)
        // update each individual app
        var success = true
        updatableApps.forEach { app ->
            val repo = repoManager.getRepository(app.repoId)
                ?: return@forEach // repo removed in the meantime?
            val listener = object : AppInstallListener {
                private val installManagerService = InstallManagerService.getInstance(context)
                private val legacyApp = App(app)
                private val legacyApk = Apk(app.update, repo)
                private val uri = legacyApk.canonicalUrl.toUri()
                private var lastProgress = 0L

                override fun onInstallProcessStarted() {
                    statusManager.addApk(legacyApp, legacyApk, Downloading, null)
                }

                override fun onDownloadProgress(downloadedBytes: Long, totalBytes: Long) {
                    val now = System.currentTimeMillis()
                    if (now - lastProgress > 1000) {
                        installManagerService.onDownloadProgress(
                            uri, legacyApp, legacyApk, downloadedBytes, totalBytes
                        )
                        lastProgress = now
                    }
                }

                override fun onDownloadFailed(e: Exception) {
                    installManagerService.onDownloadFailed(uri, e.message)
                }

                override fun onReadyToInstall() {
                    installManagerService.onDownloadComplete(uri)
                }
            }
            success = success && updateApp(app, repo, listener)
        }
        return success
    }

    private fun updateApp(
        app: UpdatableApp,
        repo: Repository,
        listener: AppInstallListener?,
    ): Boolean {
        listener?.onInstallProcessStarted()
        // legacy cruft
        val legacyApp = App(app)
        val legacyApk = Apk(app.update, repo)
        val uri = legacyApk.canonicalUrl.toUri()
        // check if app was already installed in the meantime
        try {
            val packageInfo = context.packageManager.getPackageInfo(app.packageName, 0)
            // bail out if app update was already installed
            if (packageInfo.versionCodeLong >= app.update.manifest.versionCode) return true
        } catch (e: Exception) {
            log.error(e) { "Error getting package info for ${app.packageName}" }
        }
        // download file
        val file = ApkCache.getApkDownloadPath(context, uri)
        val downloader = downloaderFactory.create(repo, uri, app.update.file, file)
        downloader.setListener { bytesRead, totalBytes ->
            log.info { "${app.name} (${app.packageName}) $bytesRead/$totalBytes" }
            listener?.onDownloadProgress(bytesRead, totalBytes)
        }
        try {
            downloader.download()
        } catch (e: Exception) {
            log.error(e) { "Error downloading $uri" }
            listener?.onDownloadFailed(e)
            return false
        }
        listener?.onReadyToInstall()
        // install file
        log.info { "Download of ${app.name} (${app.packageName}) complete, installing..." }
        val installer = InstallerFactory.create(context, legacyApp, legacyApk)
        installer.installPackage(Uri.fromFile(file), uri)
        return true
    }

}

interface AppInstallListener {
    fun onInstallProcessStarted()
    fun onDownloadProgress(downloadedBytes: Long, totalBytes: Long)
    fun onDownloadFailed(e: Exception)
    fun onReadyToInstall()
}
