package org.fdroid.fdroid.data;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.support.annotation.Nullable;

public class InstalledAppTestUtils {

    /**
     * Will tell {@code pm} that we are installing {@code packageName}, and then update the
     * "installed apps" table in the database.
     */
    public static void install(Context context,
                               String packageName,
                               int versionCode, String versionName) {
        install(context, packageName, versionCode, versionName, null);
    }

    public static void install(Context context,
                               String packageName,
                               int versionCode, String versionName,
                               @Nullable String signingCert) {
        install(context, packageName, versionCode, versionName, signingCert, null);
    }

    public static void install(Context context,
                               String packageName,
                               int versionCode, String versionName,
                               @Nullable String signingCert,
                               @Nullable String hash) {
        PackageInfo info = new PackageInfo();
        info.packageName = packageName;
        info.versionCode = versionCode;
        info.versionName = versionName;
        info.applicationInfo = new ApplicationInfo();
        info.applicationInfo.publicSourceDir = "/tmp/mock-location";
        if (signingCert != null) {
            info.signatures = new Signature[]{new Signature(signingCert)};
        }

        String hashType = "sha256";
        if (hash == null) {
            hash = "00112233445566778899aabbccddeeff";
        }

        InstalledAppProviderService.insertAppIntoDb(context, info, hashType, hash);
    }

}
