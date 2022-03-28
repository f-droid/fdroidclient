package org.fdroid.database

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_SIGNATURES
import android.os.Build
import org.fdroid.index.IndexUtils

public class UpdateChecker(
    db: FDroidDatabase,
    private val packageManager: PackageManager,
) {

    private val appDao = db.getAppDao() as AppDaoInt
    private val versionDao = db.getVersionDao() as VersionDaoInt

    fun getUpdatableApps(): List<UpdatableApp> {
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
            val version = getVersion(versions, packageInfo)
            if (version != null) {
                val versionCode = packageInfo.getVersionCode()
                val app = getUpdatableApp(version, versionCode)
                if (app != null) updatableApps.add(app)
            }
        }
        return updatableApps
    }

    @SuppressLint("PackageManagerGetSignatures")
    fun getUpdate(packageName: String): AppVersion? {
        val versions = versionDao.getVersions(listOf(packageName))
        if (versions.isEmpty()) return null
        val packageInfo = try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, GET_SIGNATURES)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        val version = getVersion(versions, packageInfo) ?: return null
        val versionedStrings = versionDao.getVersionedStrings(
            repoId = version.repoId,
            packageId = version.packageId,
            versionId = version.versionId,
        )
        return version.toAppVersion(versionedStrings)
    }

    private fun getVersion(versions: List<Version>, packageInfo: PackageInfo?): Version? {
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
            // if version code is not higher than installed skip package as list is sorted
            if (version.manifest.versionCode <= versionCode) return null
            // not considering beta versions for now
            if (!version.releaseChannels.isNullOrEmpty()) return@versions
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
