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
import android.util.Log;

import androidx.annotation.Nullable;

import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;

import java.util.Arrays;
import java.util.HashSet;

/**
 * This ApkVerifier verifies that the downloaded apk corresponds to the Apk information
 * displayed to the user. This is especially important in case an unattended installer
 * has been used which displays permissions before download.
 */
class ApkVerifier {

    private static final String TAG = "ApkVerifier";

    private final Uri localApkUri;
    private final Apk expectedApk;
    private final PackageManager pm;

    /**
     * IMPORTANT: localApkUri must be available as a File on the file system with an absolute path
     * to be readable by Android's internal PackageParser.
     */
    ApkVerifier(Context context, Uri localApkUri, Apk expectedApk) {
        this.localApkUri = localApkUri;
        this.expectedApk = expectedApk;
        this.pm = context.getPackageManager();
    }

    void verifyApk() throws ApkVerificationException, ApkPermissionUnequalException {
        Utils.debugLog(TAG, "localApkUri.getPath: " + localApkUri.getPath());

        // parse downloaded apk file locally
        PackageInfo localApkInfo = pm.getPackageArchiveInfo(
                localApkUri.getPath(), PackageManager.GET_PERMISSIONS);
        if (localApkInfo == null) {
            // Unfortunately, more specific errors are not forwarded to us
            // but the internal PackageParser sometimes shows warnings in logcat such as
            // "Requires newer sdk version #14 (current version is #11)"
            throw new ApkVerificationException("Parsing apk file failed! " +
                    "Maybe minSdk of apk is lower than current Sdk? " +
                    "Look into logcat for more specific warnings of Android's PackageParser");
        }

        // check if the apk has the expected packageName
        if (!TextUtils.equals(localApkInfo.packageName, expectedApk.packageName)) {
            throw new ApkVerificationException("Apk file has unexpected packageName! " +
                    localApkInfo.packageName);
        }

        if (localApkInfo.versionCode < 0) {
            throw new ApkVerificationException("Apk file has no valid versionCode!");
        }

        // verify permissions, important for unattended installer
        if (!requestedPermissionsEqual(expectedApk.requestedPermissions, localApkInfo.requestedPermissions)) {
            throw new ApkPermissionUnequalException("Permissions in APK and index do not match!");
        }

        int localTargetSdkVersion = localApkInfo.applicationInfo.targetSdkVersion;
        int expectedTargetSdkVersion = expectedApk.targetSdkVersion;
        Utils.debugLog(TAG, "localTargetSdkVersion: " + localTargetSdkVersion);
        Utils.debugLog(TAG, "expectedTargetSdkVersion: " + expectedTargetSdkVersion);
        if (expectedTargetSdkVersion == Apk.SDK_VERSION_MIN_VALUE) {
            // NOTE: In old fdroidserver versions, targetSdkVersion was not stored inside the repo!
            Log.w(TAG, "Skipping check for targetSdkVersion, not available in this app or repo!");
        } else if (localTargetSdkVersion != expectedTargetSdkVersion) {
            throw new ApkVerificationException(
                    String.format("TargetSdkVersion of apk file (%d) is not the expected targetSdkVersion (%d)!",
                            localTargetSdkVersion, expectedTargetSdkVersion));
        }
    }

    /**
     * Compares to sets of APK permissions to see if they are an exact match.  The
     * data format is {@link String} arrays but they are in effect sets. This is the
     * same data format as {@link android.content.pm.PackageInfo#requestedPermissions}
     */
    static boolean requestedPermissionsEqual(@Nullable String[] expected, @Nullable String[] actual) {
        Utils.debugLog(TAG, "Checking permissions");
        Utils.debugLog(TAG, "Expected:\n  " + (expected == null ? "None" : TextUtils.join("\n  ", expected)));
        Utils.debugLog(TAG, "Actual:\n  " + (actual == null ? "None" : TextUtils.join("\n  ", actual)));

        if (expected == null && actual == null) {
            return true;
        }
        if (expected == null || actual == null) {
            return false;
        }
        if (expected.length != actual.length) {
            return false;
        }
        HashSet<String> expectedSet = new HashSet<>(Arrays.asList(expected));
        HashSet<String> actualSet = new HashSet<>(Arrays.asList(actual));
        return expectedSet.equals(actualSet);
    }

    static class ApkVerificationException extends Exception {

        ApkVerificationException(String message) {
            super(message);
        }
    }

    static class ApkPermissionUnequalException extends Exception {

        ApkPermissionUnequalException(String message) {
            super(message);
        }
    }

}
