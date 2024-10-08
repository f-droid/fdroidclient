package org.fdroid.index.v1

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_PERMISSIONS
import android.content.pm.PackageManager.GET_SIGNATURES
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.encodeToStream
import org.fdroid.index.IndexCreator
import org.fdroid.index.IndexParser
import org.fdroid.index.IndexUtils.getPackageSigner
import org.fdroid.index.IndexUtils.getsig
import java.io.File
import java.io.IOException

/**
 * Creates a deprecated V1 index from the given [packageNames]
 * with information obtained from the [PackageManager].
 *
 * Attention: While [createRepo] creates `index-v1.json`,
 * it does **not** create a signed `index-v1.jar`.
 * The caller needs to handle this last signing step themselves.
 */
public class IndexV1Creator(
    packageManager: PackageManager,
    repoDir: File,
    packageNames: Set<String>,
    private val repo: RepoV1,
) : IndexCreator<IndexV1>(packageManager, repoDir, packageNames) {

    @Throws(IOException::class)
    @OptIn(ExperimentalSerializationApi::class)
    public override fun createRepo(): IndexV1 {
        prepareIconFolders()
        val index = createIndex()
        val indexJsonFile = File(repoDir, DATA_FILE_NAME)
        indexJsonFile.outputStream().use { outputStream ->
            IndexParser.json.encodeToStream(index, outputStream)
        }
        return index
    }

    private fun createIndex(): IndexV1 {
        val apps = ArrayList<AppV1>(packageNames.size)
        val packages = HashMap<String, List<PackageV1>>(packageNames.size)
        for (packageName in packageNames) {
            addApp(packageName, apps, packages)
        }
        return IndexV1(
            repo = repo,
            apps = apps,
            packages = packages,
        )
    }

    private fun addApp(
        packageName: String,
        apps: ArrayList<AppV1>,
        packages: HashMap<String, List<PackageV1>>,
    ) {
        @Suppress("DEPRECATION")
        val flags = GET_SIGNATURES or GET_PERMISSIONS

        try {
            @Suppress("PackageManagerGetSignatures")
            val packageInfo = packageManager.getPackageInfo(packageName, flags)
            apps.add(getApp(packageInfo))
            val p = getPackage(packageInfo)
            if (p == null) {
                Log.w("IndexV1Creator", "Got no package for $packageName")
                return
            }
            packages[packageName] = listOf(p)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.i("IndexV1Creator", "app disappeared during addApp: ", e)
        }
    }

    private fun getApp(packageInfo: PackageInfo): AppV1 {
        val icon = copyIconToRepo(packageInfo)
        return AppV1(
            packageName = packageInfo.packageName,
            name = packageInfo.applicationInfo?.loadLabel(packageManager).toString(),
            license = "Unknown",
            icon = icon,
        )
    }

    private fun getPackage(packageInfo: PackageInfo): PackageV1? {
        val apk = copyApkToRepo(packageInfo) ?: return null
        val appInfo = packageInfo.applicationInfo ?: return null
        val signatures = packageInfo.signatures ?: return null
        val hash = hashFile(apk)
        val apkName = apk.name
        val sig = getsig(signatures[0].toByteArray())
        val signer = getPackageSigner(signatures[0].toByteArray())
        return PackageV1(
            packageName = packageInfo.packageName,
            versionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
            versionName = packageInfo.versionName ?: PackageInfoCompat.getLongVersionCode(
                packageInfo
            ).toString(),
            apkName = apkName,
            hash = hash,
            hashType = "sha256",
            sig = sig,
            signer = signer,
            size = File(appInfo.publicSourceDir).length(),
            minSdkVersion = if (SDK_INT >= 24) appInfo.minSdkVersion else null,
            targetSdkVersion = appInfo.targetSdkVersion,
            usesPermission = packageInfo.requestedPermissions?.map {
                PermissionV1(it)
            } ?: emptyList(),
            usesPermission23 = emptyList(),
            nativeCode = parseNativeCode(packageInfo),
            features = packageInfo.reqFeatures?.map { it.name } ?: emptyList(),
        )
    }
}
