package org.fdroid.fdroid.compat;

import java.lang.Exception;

import android.content.pm.PackageManager;
import android.util.Log;

public class PackageManagerCompat extends Compatibility {

    public static void setInstaller(PackageManager mPm, String app_id) {
        if (!hasApi(11)) return;
        try {
            mPm.setInstallerPackageName(app_id, "org.fdroid.fdroid");
            Log.d("FDroid", "Installer package name for " +
                    app_id + " set successfully");
        } catch (Exception e) {
            // Many problems can occur:
            //  * App wasn't installed due to incompatibility
            //  * User canceled install
            //  * Another app interfered in the process
            //  * Another app already set the target's installer package
            //  * ...
            Log.d("FDroid", "Could not set installer package name for " +
                    app_id + ": " + e.getMessage());
        }
    }

}
