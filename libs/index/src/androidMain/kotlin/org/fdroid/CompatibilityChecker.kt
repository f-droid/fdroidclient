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
        @Suppress("UNNECESSARY_SAFE_CALL")
        packageManager.systemAvailableFeatures?.forEach { featureInfo ->
            put(featureInfo.name, if (SDK_INT >= 24) featureInfo.version else 0)
        }
    }

    public override fun isCompatible(manifest: PackageManifest): Boolean {
        if (sdkInt < (manifest.minSdkVersion ?: 0)) return false
        if (sdkInt > (manifest.maxSdkVersion ?: Int.MAX_VALUE)) return false
        if ((manifest.targetSdkVersion ?: 1) <
            CompatibilityCheckerUtils.minInstallableTargetSdk(sdkInt)) return false
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

/**
 * Contains helper methods for checking compatibility of an APK
 */
public object CompatibilityCheckerUtils {
    // Mirrored from AOSP due to lack of public APIs
    // frameworks/base/services/core/java/com/android/server/pm/PackageManagerService.java
    // search for MIN_INSTALLABLE_TARGET_SDK
    // https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-16.0.0_r1/services/core/java/com/android/server/pm/PackageManagerService.java
    // TODO: Keep this in sync with AOSP to avoid INSTALL_FAILED_DEPRECATED_SDK_VERSION errors
    @JvmOverloads
    public fun minInstallableTargetSdk(sdkInt: Int = SDK_INT): Int {
        return when (sdkInt) {
            34 -> 23 // Android 6.0, M
            35 -> 24 // Android 7.0, N
            36 -> 24 // Android 7.0, N (didn't change)
            else -> 1 // Android 1.0, BASE
        }
    }
}
