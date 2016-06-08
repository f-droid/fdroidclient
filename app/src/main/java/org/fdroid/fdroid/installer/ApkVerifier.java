/*
 * Copyright (C) 2016 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.fdroid.fdroid.installer;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;

import org.apache.commons.io.FileUtils;
import org.fdroid.fdroid.Hasher;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.SanitizedFile;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;

/**
 * This ApkVerifier verifies that the downloaded apk corresponds to the Apk information
 * displayed to the user. This is especially important in case an unattended installer
 * has been used which displays permissions before download.
 */
public class ApkVerifier {

    private static final String TAG = "ApkVerifier";

    private final Context context;
    private final Uri localApkUri;
    private final Apk expectedApk;
    private final PackageManager pm;

    ApkVerifier(Context context, Uri localApkUri, Apk expectedApk) {
        this.context = context;
        this.localApkUri = localApkUri;
        this.expectedApk = expectedApk;
        this.pm = context.getPackageManager();
    }

    public void verifyApk() throws ApkVerificationException {
        // parse downloaded apk file locally
        PackageInfo localApkInfo = pm.getPackageArchiveInfo(
                localApkUri.getPath(), PackageManager.GET_PERMISSIONS);
        if (localApkInfo == null) {
            throw new ApkVerificationException("parsing apk file failed!");
        }

        // check if the apk has the expected packageName
        if (!TextUtils.equals(localApkInfo.packageName, expectedApk.packageName)) {
            throw new ApkVerificationException("apk has unexpected packageName!");
        }

        if (localApkInfo.versionCode < 0) {
            throw new ApkVerificationException("apk has no valid versionCode!");
        }

        // verify permissions, important for unattended installer
        HashSet<String> localPermissions = getLocalPermissionsSet(localApkInfo);
        HashSet<String> expectedPermissions = expectedApk.getFullPermissionsSet();
        Utils.debugLog(TAG, "localPermissions: " + localPermissions);
        Utils.debugLog(TAG, "expectedPermissions: " + expectedPermissions);
        if (!localPermissions.equals(expectedPermissions)) {
            throw new ApkVerificationException("permissions of apk not equals expected permissions!");
        }

        int localTargetSdkVersion = localApkInfo.applicationInfo.targetSdkVersion;
        Utils.debugLog(TAG, "localTargetSdkVersion: " + localTargetSdkVersion);
        // TODO: check target sdk

    }

    private HashSet<String> getLocalPermissionsSet(PackageInfo localApkInfo) {
        String[] localPermissions = localApkInfo.requestedPermissions;
        if (localPermissions == null) {
            return new HashSet<>();
        }

        return new HashSet<>(Arrays.asList(localApkInfo.requestedPermissions));
    }

    public Uri getSafeUri() throws ApkVerificationException {
        File apkFile = new File(localApkUri.getPath());

        SanitizedFile sanitizedApkFile = null;
        try {

            /* Always copy the APK to the safe location inside of the protected area
             * of the app to prevent attacks based on other apps swapping the file
             * out during the install process. Most likely, apkFile was just downloaded,
             * so it should still be in the RAM disk cache */
            sanitizedApkFile = SanitizedFile.knownSanitized(File.createTempFile("install-", ".apk",
                    context.getFilesDir()));
            FileUtils.copyFile(apkFile, sanitizedApkFile);
            if (!verifyApkFile(sanitizedApkFile, expectedApk.hash, expectedApk.hashType)) {
                FileUtils.deleteQuietly(apkFile);
                throw new ApkVerificationException(apkFile + " failed to verify!");
            }
            apkFile = null; // ensure this is not used now that its copied to apkToInstall

            // Need the apk to be world readable, so that the installer is able to read it.
            // Note that saving it into external storage for the purpose of letting the installer
            // have access is insecure, because apps with permission to write to the external
            // storage can overwrite the app between F-Droid asking for it to be installed and
            // the installer actually installing it.
            sanitizedApkFile.setReadable(true, false);

        } catch (IOException | NoSuchAlgorithmException e) {
            throw new ApkVerificationException(e);
        } finally {
            // 20 minutes the start of the install process, delete the file
            final File apkToDelete = sanitizedApkFile;
            new Thread() {
                @Override
                public void run() {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_LOWEST);
                    try {
                        Thread.sleep(1200000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        FileUtils.deleteQuietly(apkToDelete);
                    }
                }
            }.start();
        }

        return Uri.fromFile(sanitizedApkFile);
    }

    /**
     * Checks the APK file against the provided hash, returning whether it is a match.
     */
    static boolean verifyApkFile(File apkFile, String hash, String hashType)
            throws NoSuchAlgorithmException {
        if (!apkFile.exists()) {
            return false;
        }
        Hasher hasher = new Hasher(hashType, apkFile);
        return hasher.match(hash);
    }

    public static class ApkVerificationException extends Exception {

        public ApkVerificationException(String message) {
            super(message);
        }

        public ApkVerificationException(Throwable cause) {
            super(cause);
        }
    }

}
