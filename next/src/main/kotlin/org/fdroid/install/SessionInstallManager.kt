package org.fdroid.install

import android.os.Build.VERSION.SDK_INT

object SessionInstallManager {

    /**
     * If this returns true, we can use
     * [android.content.pm.PackageInstaller.SessionParams.setRequireUserAction] with false,
     * thus updating the app with the given targetSdk without user action.
     */
    fun isTargetSdkSupported(targetSdk: Int): Boolean {
        if (SDK_INT < 31) return false // not supported below Android 12

        if (SDK_INT == 31 && targetSdk >= 29) return true
        if (SDK_INT == 32 && targetSdk >= 29) return true
        if (SDK_INT == 33 && targetSdk >= 30) return true
        if (SDK_INT == 34 && targetSdk >= 31) return true
        // This needs to be adjusted as new Android versions are released
        // https://developer.android.com/reference/android/content/pm/PackageInstaller.SessionParams#setRequireUserAction(int)
        // https://cs.android.com/android/platform/superproject/+/android-16.0.0_r2:frameworks/base/services/core/java/com/android/server/pm/PackageInstallerSession.java;l=329;drc=73caa0299d9196ddeefe4f659f557fb880f6536d
        // current code requires targetSdk 33 on SDK 35+
        return SDK_INT >= 35 && targetSdk >= 33
    }
}
