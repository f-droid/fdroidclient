package org.fdroid.fdroid.data;

import android.content.pm.PackageInfo;

import mock.MockContextSwappableComponents;
import mock.MockInstallablePackageManager;

public class InstalledAppTestUtils {

    /**
     * Will tell {@code pm} that we are installing {@code packageName}, and then update the
     * "installed apps" table in the database.
     */
    public static void install(MockContextSwappableComponents context,
                               MockInstallablePackageManager pm, String packageName,
                               int versionCode, String versionName) {

        context.setPackageManager(pm);
        pm.install(packageName, versionCode, versionName);
        PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);
        InstalledAppProviderService.insertAppIntoDb(context, packageName, packageInfo);
    }

}
