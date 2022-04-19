package org.fdroid.database

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_SIGNATURES
import android.os.Build
import org.fdroid.CompatibilityCheckerImpl
import org.fdroid.index.IndexUtils

public class UpdateChecker(
    db: FDroidDatabase,
    private val packageManager: PackageManager,
) {

    private val appDao = db.getAppDao() as AppDaoInt
    private val versionDao = db.getVersionDao() as VersionDaoInt
    private val compatibilityChecker = CompatibilityCheckerImpl(packageManager)

    /**
     * Returns a list of apps that can be updated.
     * @param releaseChannels optional list of release channels to consider on top of stable.
     * If this is null or empty, only versions without channel (stable) will be considered.
     */
    public fun getUpdatableApps(releaseChannels: List<String>? = null): List<UpdatableApp> {
        val updatableApps = ArrayList<UpdatableApp>()

        @Suppress("DEPRECATION") // we'll use this as long as it works, new one was broken
        val installedPackages = packageManager.getInstalledPackages(GET_SIGNATURES)
        val packageNames = installedPackages.map { it.packageName }
        val versionsByPackage = HashMap<String, ArrayList<Version>>(packageNames.size)
        versionDao.getVersions(packageNames).forEach { version ->
            val list = versionsByPackage.getOrPut(version.packageId) { ArrayList() }
            list.add(version)
        }
        installedPackages.iterator().forEach { packageInfo ->
            val versions = versionsByPackage[packageInfo.packageName] ?: return@forEach // continue
            val version = getVersion(versions, packageInfo, releaseChannels)
            if (version != null) {
                val versionCode = packageInfo.getVersionCode()
                val app = getUpdatableApp(version, versionCode)
                if (app != null) updatableApps.add(app)
            }
        }
        return updatableApps
    }

    /**
     * Returns an [AppVersion] for the given [packageName] that is an update
     * or null if there is none.
     * @param releaseChannels optional list of release channels to consider on top of stable.
     * If this is null or empty, only versions without channel (stable) will be considered.
     */
    @SuppressLint("PackageManagerGetSignatures")
    public fun getUpdate(packageName: String, releaseChannels: List<String>? = null): AppVersion? {
        val versions = versionDao.getVersions(listOf(packageName))
        if (versions.isEmpty()) return null
        val packageInfo = try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, GET_SIGNATURES)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        val version = getVersion(versions, packageInfo, releaseChannels) ?: return null
        val versionedStrings = versionDao.getVersionedStrings(
            repoId = version.repoId,
            packageId = version.packageId,
            versionId = version.versionId,
        )
        return version.toAppVersion(versionedStrings)
    }

    private fun getVersion(
        versions: List<Version>,
        packageInfo: PackageInfo?,
        releaseChannels: List<String>?,
    ): Version? {
        val versionCode = packageInfo?.getVersionCode() ?: 0
        // the below is rather expensive, so we only do that when there's update candidates
        // TODO handle signingInfo.signingCertificateHistory as well
        @Suppress("DEPRECATION")
        val signatures by lazy {
            packageInfo?.signatures?.map {
                IndexUtils.getPackageSignature(it.toByteArray())
            }?.toSet()
        }
        versions.iterator().forEach versions@{ version ->
            // if the installed version has a known vulnerability, we return it as well
            if (version.manifest.versionCode == versionCode && version.hasKnownVulnerability) {
                return version
            }
            // if version code is not higher than installed skip package as list is sorted
            if (version.manifest.versionCode <= versionCode) return null
            // check release channels if they are not empty
            if (!version.releaseChannels.isNullOrEmpty()) {
                // if release channels are not empty (stable) don't consider this version
                if (releaseChannels == null) return@versions
                // don't consider version with non-matching release channel
                if (releaseChannels.intersect(version.releaseChannels).isEmpty()) return@versions
            }
            // skip incompatible versions
            if (!compatibilityChecker.isCompatible(version.manifest)) return@versions
            val canInstall = if (packageInfo == null) {
                true // take first one with highest version code and repo weight
            } else {
                // TODO also support AppPrefs with ignoring updates
                val versionSignatures = version.manifest.signer?.sha256?.toSet()
                signatures == versionSignatures
            }
            // no need to see other versions, we got the highest version code per sorting
            if (canInstall) return version
        }
        return null
    }

    private fun getUpdatableApp(version: Version, installedVersionCode: Long): UpdatableApp? {
        val versionedStrings = versionDao.getVersionedStrings(
            repoId = version.repoId,
            packageId = version.packageId,
            versionId = version.versionId,
        )
        val appOverviewItem =
            appDao.getAppOverviewItem(version.repoId, version.packageId) ?: return null
        return UpdatableApp(
            packageId = version.packageId,
            installedVersionCode = installedVersionCode,
            upgrade = version.toAppVersion(versionedStrings),
            hasKnownVulnerability = version.hasKnownVulnerability,
            name = appOverviewItem.name,
            summary = appOverviewItem.summary,
            localizedIcon = appOverviewItem.localizedIcon,
        )
    }
}

internal fun PackageInfo.getVersionCode(): Long {
    return if (Build.VERSION.SDK_INT >= 28) {
        longVersionCode
    } else {
        @Suppress("DEPRECATION") // we use the new one above, if available
        versionCode.toLong()
    }
}
