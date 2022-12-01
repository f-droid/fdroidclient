package org.fdroid

import android.content.pm.PackageManager
import android.os.Build.SUPPORTED_ABIS
import android.os.Build.VERSION.SDK_INT
import org.fdroid.index.v2.PackageManifest

public fun interface CompatibilityChecker {
    public fun isCompatible(manifest: PackageManifest): Boolean
}

/**
 * This class checks if an APK is compatible with the user's device.
 */
public class CompatibilityCheckerImpl @JvmOverloads constructor(
    packageManager: PackageManager,
    private val forceTouchApps: Boolean = false,
    private val sdkInt: Int = SDK_INT,
    private val supportedAbis: Array<String> = SUPPORTED_ABIS,
) : CompatibilityChecker {

    private val features = HashMap<String, Int>().apply {
        // the docs still say that this can be null, so better be on the safe side
        @Suppress("SAFE_CALL_WILL_CHANGE_NULLABILITY", "UNNECESSARY_SAFE_CALL")
        packageManager.systemAvailableFeatures?.forEach { featureInfo ->
            put(featureInfo.name, if (SDK_INT >= 24) featureInfo.version else 0)
        }
    }

    public override fun isCompatible(manifest: PackageManifest): Boolean {
        if (sdkInt < manifest.minSdkVersion ?: 0) return false
        if (sdkInt > manifest.maxSdkVersion ?: Int.MAX_VALUE) return false
        if (!isNativeCodeCompatible(manifest)) return false
        manifest.featureNames?.iterator()?.forEach { feature ->
            if (forceTouchApps && feature == "android.hardware.touchscreen") return@forEach
            if (!features.containsKey(feature)) return false
        }
        return true
    }

    private fun isNativeCodeCompatible(manifest: PackageManifest): Boolean {
        val nativeCode = manifest.nativecode
        if (nativeCode.isNullOrEmpty()) return true
        supportedAbis.forEach { supportedAbi ->
            if (nativeCode.contains(supportedAbi)) return true
        }
        return false
    }
}
