package org.fdroid.database

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_SIGNATURES
import androidx.core.content.pm.PackageInfoCompat
import org.fdroid.CompatibilityChecker
import org.fdroid.CompatibilityCheckerImpl
import org.fdroid.PackagePreference
import org.fdroid.UpdateChecker

public class DbUpdateChecker @JvmOverloads constructor(
    db: FDroidDatabase,
    private val packageManager: PackageManager,
    compatibilityChecker: CompatibilityChecker = CompatibilityCheckerImpl(packageManager),
) {

    private val appDao = db.getAppDao() as AppDaoInt
    private val versionDao = db.getVersionDao() as VersionDaoInt
    private val appPrefsDao = db.getAppPrefsDao() as AppPrefsDaoInt
    private val updateChecker = UpdateChecker(compatibilityChecker)

    /**
     * Returns a list of apps that can be updated.
     * @param releaseChannels optional list of release channels to consider on top of stable.
     * If this is null or empty, only versions without channel (stable) will be considered.
     */
    @JvmOverloads
    public fun getUpdatableApps(
        releaseChannels: List<String>? = null,
        includeKnownVulnerabilities: Boolean = false,
    ): List<UpdatableApp> {
        val updatableApps = ArrayList<UpdatableApp>()

        @Suppress("DEPRECATION") // we'll use this as long as it works, new one was broken
        val installedPackages = packageManager.getInstalledPackages(GET_SIGNATURES)
        val packageNames = installedPackages.map { it.packageName }
        val versionsByPackage = HashMap<String, ArrayList<Version>>(packageNames.size)
        versionDao.getVersions(packageNames).forEach { version ->
            val list = versionsByPackage.getOrPut(version.packageName) { ArrayList() }
            list.add(version)
        }
        installedPackages.iterator().forEach { packageInfo ->
            val packageName = packageInfo.packageName
            val versions = versionsByPackage[packageName] ?: return@forEach // continue
            val version = getVersion(
                versions = versions,
                packageName = packageName,
                packageInfo = packageInfo,
                preferredSigner = null,
                releaseChannels = releaseChannels,
                includeKnownVulnerabilities = includeKnownVulnerabilities,
            )
            if (version != null) {
                val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
                val app = getUpdatableApp(version, versionCode)
                if (app != null) updatableApps.add(app)
            }
        }
        return updatableApps
    }

    /**
     * Returns an [AppVersion] for the given [packageName] that is an update or new install
     * or null if there is none.
     * @param releaseChannels optional list of release channels to consider on top of stable.
     * If this is null or empty, only versions without channel (stable) will be considered.
     */
    @SuppressLint("PackageManagerGetSignatures")
    public fun getSuggestedVersion(
        packageName: String,
        preferredSigner: String? = null,
        releaseChannels: List<String>? = null,
    ): AppVersion? {
        val versions = versionDao.getVersions(listOf(packageName))
        if (versions.isEmpty()) return null
        val packageInfo = try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, GET_SIGNATURES)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        val version = getVersion(
            versions = versions,
            packageName = packageName,
            packageInfo = packageInfo,
            preferredSigner = preferredSigner,
            releaseChannels = releaseChannels,
        ) ?: return null
        val versionedStrings = versionDao.getVersionedStrings(
            repoId = version.repoId,
            packageName = version.packageName,
            versionId = version.versionId,
        )
        return version.toAppVersion(versionedStrings)
    }

    private fun getVersion(
        versions: List<Version>,
        packageName: String,
        packageInfo: PackageInfo?,
        preferredSigner: String?,
        releaseChannels: List<String>?,
        includeKnownVulnerabilities: Boolean = false,
    ): Version? {
        val preferencesGetter: (() -> PackagePreference?) = {
            appPrefsDao.getAppPrefsOrNull(packageName)
        }
        return if (packageInfo == null) {
            updateChecker.getSuggestedVersion(
                versions = versions,
                preferredSigner = preferredSigner,
                releaseChannels = releaseChannels,
                preferencesGetter = preferencesGetter,
            )
        } else {
            updateChecker.getUpdate(
                versions = versions,
                packageInfo = packageInfo,
                releaseChannels = releaseChannels,
                includeKnownVulnerabilities = includeKnownVulnerabilities,
                preferencesGetter = preferencesGetter,
            )
        }
    }

    private fun getUpdatableApp(version: Version, installedVersionCode: Long): UpdatableApp? {
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
            update = version.toAppVersion(versionedStrings),
            hasKnownVulnerability = version.hasKnownVulnerability,
            name = appOverviewItem.name,
            summary = appOverviewItem.summary,
            localizedIcon = appOverviewItem.localizedIcon,
        )
    }
}
