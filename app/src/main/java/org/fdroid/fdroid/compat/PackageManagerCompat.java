package org.fdroid.fdroid.compat;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import org.fdroid.fdroid.installer.PrivilegedInstaller;
import org.fdroid.fdroid.Utils;

public class PackageManagerCompat {

    private static final String TAG = "PackageManagerCompat";

    @TargetApi(11)
    public static void setInstaller(Context context, PackageManager mPm, String packageName) {
        if (Build.VERSION.SDK_INT < 11) return;
        try {
            /*
             * Starting with 7.0 (API 24), we're using PackageInstaller APIs
             * to install and uninstall apps via the privileged extension.
             * That enforces the uninstaller being the same as the installer,
             * so set the package name to that.
             */
            if (Build.VERSION.SDK_INT >= 24 && PrivilegedInstaller.isDefault(context)) {
                mPm.setInstallerPackageName(packageName, "org.fdroid.fdroid.privileged");
            } else {
                mPm.setInstallerPackageName(packageName, "org.fdroid.fdroid");
            }
            Utils.debugLog(TAG, "Installer package name for " + packageName + " set successfully");
        } catch (SecurityException e) {
            throw new SecurityException(e);
        } catch (Exception e) {
            // Many problems can occur:
            //  * App wasn't installed due to incompatibility
            //  * User canceled install
            //  * Another app interfered in the process
            //  * Another app already set the target's installer package
            //  * ...
            Log.e(TAG, "Could not set installer package name for " +
                    packageName, e);
        }
    }

}
