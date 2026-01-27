package org.fdroid.database

import android.content.Context
import android.content.pm.ApplicationInfo.FLAG_SYSTEM
import android.content.pm.PackageInfo
import android.os.Build.VERSION.SDK_INT
import androidx.core.content.pm.PackageInfoCompat.getLongVersionCode
import org.fdroid.CompatibilityChecker
import org.fdroid.CompatibilityCheckerImpl
import org.fdroid.UpdateChecker
import org.fdroid.index.IndexUtils.getPackageSigner

public class DbAppChecker(
    db: FDroidDatabase,
    private val context: Context,
    compatibilityChecker: CompatibilityChecker = CompatibilityCheckerImpl(context.packageManager),
    private val updateChecker: UpdateChecker = UpdateChecker(compatibilityChecker),
) {
    private val packageManager = context.packageManager
    private val appDao = db.getAppDao() as AppDaoInt
    private val versionDao = db.getVersionDao() as VersionDaoInt
    private val appPrefsDao = db.getAppPrefsDao() as AppPrefsDaoInt

    /**
     * Gets all apps that somehow have a special status that warrants the user's attention.
     * These can include apps that:
     * * have updates available ([UpdatableApp])
     * * can not be updated, because we don't have those apps anymore ([NotAvailable])
     * * can not be updated, because all versions have incompatible signer ([NoCompatibleSigner])
     * * could get updated from another repo ([UpdateInOtherRepo])
     * * have known vulnerabilities ([KnownVulnerability])
     */
    public fun getApps(packageInfoMap: Map<String, PackageInfo>): AppCheckResult {
        val updatableApps = ArrayList<UpdatableApp>()
        val appsWithIssue = ArrayList<AppWithIssue>()

        // get all versions for all packages (irrespective of preferred repo)
        // and make them accessible per packageName
        val packageNames = packageInfoMap.keys.toList()
        val versionsByPackage = HashMap<String, ArrayList<Version>>(packageNames.size)
        // TODO add test for an app ignoring all updates, this won't return versions here
        versionDao.getVersions(packageNames).forEach { version ->
            val versions = versionsByPackage.getOrPut(version.packageName) { ArrayList() }
            versions.add(version)
        }
        // go through all apps (packages) and check for updates
        val preferredRepos = appPrefsDao.getPreferredRepos(packageNames)
        packageInfoMap.forEach packages@{ (packageName, packageInfo) ->
            // get versions for this app and try to find an update in them
            val versions = versionsByPackage[packageName]
            val flags = packageInfo.applicationInfo?.flags ?: 0
            if (versions.isNullOrEmpty() && flags and FLAG_SYSTEM == 0) {
                // we have no versions and no system app,
                // so check if we maybe had installed this app in the past
                getUnavailableApp(packageInfo, preferredRepos)?.let { unavailableApp ->
                    appsWithIssue.add(unavailableApp)
                }
                return@packages // continue
            }
            // we ignore system apps without version
            if (versions == null) return@packages // continue
            // get all updates from the versions we found
            // these can be from other repos, have incompatible signers or just are KnownVuln
            val updates = updateChecker.getUpdates(
                versions = versions,
                allowedSignersGetter = null, // all signers are allowed
                installedVersionCode = getLongVersionCode(packageInfo),
                allowedReleaseChannels = null,
                includeKnownVulnerabilities = true,
                preferencesGetter = { appPrefsDao.getAppPrefsOrNull(packageName) },
            ).toList()
            // if there are no updates available, there's nothing left to do for us
            if (updates.isEmpty()) return@packages
            // we have updates, so now get some data for us to judge those updates

            // get preferred repo for the current app
            val preferredRepoId = preferredRepos[packageName]
                ?: error("No preferred repo for $packageName")

            // get allowed signers for current app
            // always gives us the oldest signer, even if they rotated certs by now
            @Suppress("DEPRECATION")
            val allowedSigners = packageInfo.signatures?.map {
                getPackageSigner(it.toByteArray())
            }?.toSet() ?: error("Got no signatures for $packageName")

            // happy path is a preferred and compatible update, so we look for those first
            // for simplicity and safety, we tell the user to make those updates first
            updates.forEach { update ->
                if (update.isOk(preferredRepoId, allowedSigners)) {
                    getUpdatableApp(
                        version = update,
                        installedVersionCode = getLongVersionCode(packageInfo),
                        installedVersionName = packageInfo.versionName ?: "???",
                    )?.let { app -> updatableApps.add(app) }
                    return@packages
                }
            }

            // we do have update(s), but there's an issue with them, find out what
            // for simplicity, we only consider the issue of the most recent version
            val update = updates[0]
            val updateSigners = update.signer?.sha256?.toSet()
            val hasCompatibleSigner =
                updateSigners == null || updateSigners.intersect(allowedSigners).isNotEmpty()
            val app = appDao.getAppOverviewItem(preferredRepoId, packageName) ?: return@packages

            // find out the specific issue
            val appWithIssue = if (update.hasKnownVulnerability) {
                AvailableAppWithIssue(
                    app = app,
                    installVersionName = packageInfo.versionName ?: "???",
                    installVersionCode = getLongVersionCode(packageInfo),
                    issue = KnownVulnerability(preferredRepoId == update.repoId),
                )
            } else if (hasCompatibleSigner) {
                // the signer is compatible, so the update must come from a non-preferred repo
                AvailableAppWithIssue(
                    app = app,
                    installVersionName = packageInfo.versionName ?: "???",
                    installVersionCode = getLongVersionCode(packageInfo),
                    issue = UpdateInOtherRepo(update.repoId),
                )
            } else {
                // no update with compatible signer available
                getNoCompatibleSignerApp(
                    // check if there's a compatible signer available in a non-preferred repo
                    repoIdWithCompatibleSigner = updates.find {
                        val signers = it.signer?.sha256?.toSet()
                        signers == null || signers.intersect(allowedSigners).isNotEmpty()
                    }?.repoId,
                    app = app,
                    versions = versions,
                    packageInfo = packageInfo,
                    preferredRepoId = preferredRepoId,
                    allowedSigners = allowedSigners
                )
            }
            appWithIssue?.let { appsWithIssue.add(it) }
        }
        return AppCheckResult(
            updates = updatableApps,
            issues = appsWithIssue,
        )
    }

    /**
     * Returns a [UnavailableAppWithIssue], in case the app provided with [packageInfo]
     * was installed by us in the past.
     */
    private fun getUnavailableApp(
        packageInfo: PackageInfo,
        preferredRepos: Map<String, Long>,
    ): UnavailableAppWithIssue? {
        // check if we installed the app or are the current update owner of this app
        val weInstalledApp = if (SDK_INT >= 30) {
            val installInfo = packageManager.getInstallSourceInfo(packageInfo.packageName)
            context.packageName == installInfo.initiatingPackageName ||
                context.packageName == installInfo.installingPackageName ||
                (SDK_INT >= 34 && context.packageName == installInfo.updateOwnerPackageName)
        } else {
            @Suppress("DEPRECATION") // no other choice to use this for old API versions
            val installer = packageManager.getInstallerPackageName(packageInfo.packageName)
            context.packageName == installer
        }
        if (weInstalledApp) {
            // we had installed this app, check if we maybe just got no versions
            val app = preferredRepos[packageInfo.packageName]?.let { repoId ->
                appDao.getAppOverviewItem(repoId, packageInfo.packageName)
            }
            // we still have the app, so we just didn't get versions for it,
            // like when the user was ignoring all updates for the app
            if (app != null) return null
            // warn the user that this app isn't available anymore
            val notAvailable = UnavailableAppWithIssue(
                packageName = packageInfo.packageName,
                name = packageInfo.applicationInfo?.loadLabel(packageManager),
                installVersionName = packageInfo.versionName ?: "???",
                installVersionCode = getLongVersionCode(packageInfo),
            )
            return notAvailable
        }
        return null
    }

    /**
     * Returns [AvailableAppWithIssue] with [NoCompatibleSigner],
     * if the app has an update in a repo, but all versions have an incompatible signer.
     *
     * @param repoIdWithCompatibleSigner the ID of the [Repository]
     * that does have a compatible signer.
     * Null if no repository has a compatible signer.
     */
    private fun getNoCompatibleSignerApp(
        repoIdWithCompatibleSigner: Long?,
        app: AppOverviewItem,
        versions: ArrayList<Version>,
        packageInfo: PackageInfo,
        preferredRepoId: Long,
        allowedSigners: Set<String>,
    ): AvailableAppWithIssue? {
        return if (repoIdWithCompatibleSigner == null) {
            // all updates are not compatible, we only warn about this,
            // if all versions in the preferred repo aren't compatible
            val allIncompatible = versions.all { version ->
                version.repoId != preferredRepoId ||
                    !version.isOk(preferredRepoId, allowedSigners)
            }
            if (allIncompatible) {
                // possibly the wrong repo was preferred, try to find the right one
                val repoId = versions.find {
                    // treat the current repo as preferred, so we only look at signers
                    it.isOk(it.repoId, allowedSigners)
                }?.repoId
                if (repoId == null) {
                    // the app may have been installed with another installer
                    val installerPackageName = if (SDK_INT >= 30) {
                        packageManager.getInstallSourceInfo(packageInfo.packageName)
                            .installingPackageName
                    } else {
                        @Suppress("DEPRECATION") // no other choice to use this for old API versions
                        packageManager.getInstallerPackageName(packageInfo.packageName)
                    }
                    // if there is another installer, we don't warn, but leave things to them
                    if (installerPackageName != null &&
                        installerPackageName != context.packageName
                    ) return null
                }
                return AvailableAppWithIssue(
                    app = app,
                    installVersionName = packageInfo.versionName ?: "???",
                    installVersionCode = getLongVersionCode(packageInfo),
                    issue = NoCompatibleSigner(repoId),
                )
            } else {
                // there was at least one compatible version in a repo,
                // so there's hope the update will arrive there
                null
            }
        } else {
            AvailableAppWithIssue(
                app = app,
                installVersionName = packageInfo.versionName ?: "???",
                installVersionCode = getLongVersionCode(packageInfo),
                issue = NoCompatibleSigner(repoIdWithCompatibleSigner),
            )
        }
    }

    /**
     * @return true if this version is an update from the preferred repo with a compatible signer
     * and not a known vulnerable version.
     */
    private fun Version.isOk(preferredRepoId: Long, signers: Set<String>): Boolean {
        val ourSigners = signer?.sha256?.toSet()
        return preferredRepoId == repoId &&
            !hasKnownVulnerability &&
            (ourSigners == null || ourSigners.intersect(signers).isNotEmpty())
    }

    private fun getUpdatableApp(
        version: Version,
        installedVersionCode: Long,
        installedVersionName: String,
    ): UpdatableApp? {
        val versionedStrings = versionDao.getVersionedStrings(
            repoId = version.repoId,
            packageName = version.packageName,
            versionId = version.versionId,
        )
        val appOverviewItem =
            appDao.getAppOverviewItem(version.repoId, version.packageName) ?: return null
        return UpdatableApp(
            repoId = version.repoId,
            packageName = version.packageName,
            installedVersionCode = installedVersionCode,
            installedVersionName = installedVersionName,
            update = version.toAppVersion(versionedStrings),
            isFromPreferredRepo = true,
            hasKnownVulnerability = version.hasKnownVulnerability,
            name = appOverviewItem.name,
            summary = appOverviewItem.summary,
            localizedIcon = appOverviewItem.localizedIcon,
        )
    }
}
