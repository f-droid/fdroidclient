package org.fdroid

import android.content.pm.PackageManager
import android.os.Build.SUPPORTED_ABIS
import android.os.Build.VERSION.SDK_INT
import org.fdroid.index.v2.ManifestV2

public fun interface CompatibilityChecker {
    public fun isCompatible(manifest: ManifestV2): Boolean
}

/**
 * This class checks if an APK is compatible with the user's device.
 */
public class CompatibilityCheckerImpl(
    packageManager: PackageManager,
    private val forceTouchApps: Boolean = false,
) : CompatibilityChecker {

    private val features = HashMap<String, Int>().apply {
        // the docs still say that this can be null, so better be on the safe side
        @Suppress("SAFE_CALL_WILL_CHANGE_NULLABILITY", "UNNECESSARY_SAFE_CALL")
        packageManager.systemAvailableFeatures?.forEach { featureInfo ->
            put(featureInfo.name, if (SDK_INT >= 24) featureInfo.version else 0)
        }
    }

    public override fun isCompatible(manifest: ManifestV2): Boolean {
        if (SDK_INT < manifest.usesSdk?.minSdkVersion ?: 0) return false
        if (SDK_INT > manifest.maxSdkVersion ?: Int.MAX_VALUE) return false
        if (!isNativeCodeCompatible(manifest)) return false
        manifest.features.iterator().forEach { feature ->
            if (forceTouchApps && feature.name == "android.hardware.touchscreen") return@forEach
            if (!features.containsKey(feature.name)) return false
        }
        return true
    }

    private fun isNativeCodeCompatible(manifest: ManifestV2): Boolean {
        if (manifest.nativecode.isNullOrEmpty()) return true
        SUPPORTED_ABIS.forEach { supportedAbi ->
            if (manifest.nativecode.contains(supportedAbi)) return true
        }
        return false
    }
}
