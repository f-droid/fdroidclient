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

import android.app.Instrumentation;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.fdroid.fdroid.AssetUtils;
import org.fdroid.fdroid.RepoXMLHandler;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.compat.FileCompatTest;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.mock.RepoDetails;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This test checks the ApkVerifier by parsing a repo from permissionsRepo.xml
 * and checking the listed permissions against the ones specified in apks' AndroidManifest,
 * which have been specifically generated for this test.
 * <p>
 * NOTE: This androidTest cannot run as a Robolectric test because the
 * required methods from PackageManger are not included in Robolectric's Android API.
 * java.lang.NoClassDefFoundError: java/util/jar/StrictJarFile
 * at android.content.pm.PackageManager.getPackageArchiveInfo(PackageManager.java:3545)
 */
@RunWith(AndroidJUnit4.class)
public class ApkVerifierTest {
    public static final String TAG = "ApkVerifierTest";

    Instrumentation instrumentation;

    File sdk14Apk;
    File minMaxApk;
    private File extendedPermissionsApk;
    private File extendedPermsXml;

    @Before
    public void setUp() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        File dir = FileCompatTest.getWriteableDir(instrumentation);
        assertTrue(dir.isDirectory());
        assertTrue(dir.canWrite());
        sdk14Apk = AssetUtils.copyAssetToDir(instrumentation.getContext(),
                "org.fdroid.permissions.sdk14.apk",
                dir
        );
        minMaxApk = AssetUtils.copyAssetToDir(instrumentation.getContext(),
                "org.fdroid.permissions.minmax.apk",
                dir
        );
        extendedPermissionsApk = AssetUtils.copyAssetToDir(instrumentation.getContext(),
                "org.fdroid.extendedpermissionstest.apk",
                dir
        );
        extendedPermsXml = AssetUtils.copyAssetToDir(instrumentation.getContext(),
                "extendedPerms.xml",
                dir
        );
        assertTrue(sdk14Apk.exists());
        assertTrue(minMaxApk.exists());
        assertTrue(extendedPermissionsApk.exists());
        assertTrue(extendedPermsXml.exists());
    }

    @Test
    public void testNulls() {
        assertTrue(ApkVerifier.requestedPermissionsEqual(null, null));

        String[] perms = new String[] {"Blah"};
        assertFalse(ApkVerifier.requestedPermissionsEqual(perms, null));
        assertFalse(ApkVerifier.requestedPermissionsEqual(null, perms));
    }

    @Test
    public void testWithoutPrefix() {
        Apk apk = new Apk();
        apk.packageName = "org.fdroid.permissions.sdk14";
        apk.targetSdkVersion = 14;
        String[] noPrefixPermissions = new String[]{
                "AUTHENTICATE_ACCOUNTS",
                "MANAGE_ACCOUNTS",
                "READ_PROFILE",
                "WRITE_PROFILE",
                "GET_ACCOUNTS",
                "READ_CONTACTS",
                "WRITE_CONTACTS",
                "WRITE_EXTERNAL_STORAGE",
                "READ_EXTERNAL_STORAGE",
                "INTERNET",
                "ACCESS_NETWORK_STATE",
                "NFC",
                "READ_SYNC_SETTINGS",
                "WRITE_SYNC_SETTINGS",
                "WRITE_CALL_LOG", // implied-permission!
                "READ_CALL_LOG", // implied-permission!
        };
        for (int i = 0; i < noPrefixPermissions.length; i++) {
            noPrefixPermissions[i] = RepoXMLHandler.fdroidToAndroidPermission(noPrefixPermissions[i]);
        }
        apk.requestedPermissions = noPrefixPermissions;

        Uri uri = Uri.fromFile(sdk14Apk);
        ApkVerifier apkVerifier = new ApkVerifier(instrumentation.getContext(), uri, apk);

        try {
            apkVerifier.verifyApk();
        } catch (ApkVerifier.ApkVerificationException | ApkVerifier.ApkPermissionUnequalException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test(expected = ApkVerifier.ApkPermissionUnequalException.class)
    public void testWithMinMax()
            throws ApkVerifier.ApkPermissionUnequalException, ApkVerifier.ApkVerificationException {
        Apk apk = new Apk();
        apk.packageName = "org.fdroid.permissions.minmax";
        apk.targetSdkVersion = 24;
        ArrayList<String> permissionsList = new ArrayList<>();
        permissionsList.add("android.permission.READ_CALENDAR");
        if (Build.VERSION.SDK_INT <= 18) {
            permissionsList.add("android.permission.WRITE_EXTERNAL_STORAGE");
        }
        if (Build.VERSION.SDK_INT >= 23) {
            permissionsList.add("android.permission.ACCESS_FINE_LOCATION");
        }
        apk.requestedPermissions = permissionsList.toArray(new String[permissionsList.size()]);

        Uri uri = Uri.fromFile(minMaxApk);
        ApkVerifier apkVerifier = new ApkVerifier(instrumentation.getContext(), uri, apk);
        apkVerifier.verifyApk();

        permissionsList.add("ADDITIONAL_PERMISSION");
        apk.requestedPermissions = permissionsList.toArray(new String[permissionsList.size()]);
        apkVerifier.verifyApk();
    }

    @Test
    public void testWithPrefix() {
        Apk apk = new Apk();
        apk.packageName = "org.fdroid.permissions.sdk14";
        apk.targetSdkVersion = 14;
        apk.requestedPermissions = new String[]{
                "android.permission.AUTHENTICATE_ACCOUNTS",
                "android.permission.MANAGE_ACCOUNTS",
                "android.permission.READ_PROFILE",
                "android.permission.WRITE_PROFILE",
                "android.permission.GET_ACCOUNTS",
                "android.permission.READ_CONTACTS",
                "android.permission.WRITE_CONTACTS",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.INTERNET",
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.NFC",
                "android.permission.READ_SYNC_SETTINGS",
                "android.permission.WRITE_SYNC_SETTINGS",
                "android.permission.WRITE_CALL_LOG", // implied-permission!
                "android.permission.READ_CALL_LOG", // implied-permission!
        };

        Uri uri = Uri.fromFile(sdk14Apk);

        ApkVerifier apkVerifier = new ApkVerifier(instrumentation.getContext(), uri, apk);

        try {
            apkVerifier.verifyApk();
        } catch (ApkVerifier.ApkVerificationException | ApkVerifier.ApkPermissionUnequalException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Additional permissions are okay. The user is simply
     * warned about a permission that is not used inside the apk
     */
    @Test(expected = ApkVerifier.ApkPermissionUnequalException.class)
    public void testAdditionalPermission()
            throws ApkVerifier.ApkPermissionUnequalException, ApkVerifier.ApkVerificationException {
        Apk apk = new Apk();
        apk.packageName = "org.fdroid.permissions.sdk14";
        apk.targetSdkVersion = 14;
        apk.requestedPermissions = new String[]{
                "android.permission.AUTHENTICATE_ACCOUNTS",
                "android.permission.MANAGE_ACCOUNTS",
                "android.permission.READ_PROFILE",
                "android.permission.WRITE_PROFILE",
                "android.permission.GET_ACCOUNTS",
                "android.permission.READ_CONTACTS",
                "android.permission.WRITE_CONTACTS",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.INTERNET",
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.NFC",
                "android.permission.READ_SYNC_SETTINGS",
                "android.permission.WRITE_SYNC_SETTINGS",
                "android.permission.WRITE_CALL_LOG", // implied-permission!
                "android.permission.READ_CALL_LOG", // implied-permission!
                "android.permission.FAKE_NEW_PERMISSION",
        };

        Uri uri = Uri.fromFile(sdk14Apk);
        ApkVerifier apkVerifier = new ApkVerifier(instrumentation.getContext(), uri, apk);
        apkVerifier.verifyApk();
    }

    /**
     * Missing permissions are not okay!
     * The user is then not warned about a permission that the apk uses!
     */
    @Test
    public void testMissingPermission() {
        Apk apk = new Apk();
        apk.packageName = "org.fdroid.permissions.sdk14";
        apk.targetSdkVersion = 14;
        apk.requestedPermissions = new String[]{
                //"android.permission.AUTHENTICATE_ACCOUNTS",
                "android.permission.MANAGE_ACCOUNTS",
                "android.permission.READ_PROFILE",
                "android.permission.WRITE_PROFILE",
                "android.permission.GET_ACCOUNTS",
                "android.permission.READ_CONTACTS",
                "android.permission.WRITE_CONTACTS",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.INTERNET",
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.NFC",
                "android.permission.READ_SYNC_SETTINGS",
                "android.permission.WRITE_SYNC_SETTINGS",
                "android.permission.WRITE_CALL_LOG", // implied-permission!
                "android.permission.READ_CALL_LOG", // implied-permission!
        };

        Uri uri = Uri.fromFile(sdk14Apk);

        ApkVerifier apkVerifier = new ApkVerifier(instrumentation.getContext(), uri, apk);

        try {
            apkVerifier.verifyApk();
            fail();
        } catch (ApkVerifier.ApkVerificationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } catch (ApkVerifier.ApkPermissionUnequalException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testExtendedPerms() throws IOException,
            ApkVerifier.ApkPermissionUnequalException, ApkVerifier.ApkVerificationException {
        RepoDetails actualDetails = getFromFile(extendedPermsXml);
        HashSet<String> expectedSet = new HashSet<>(Arrays.asList(new String[]{
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.ACCESS_WIFI_STATE",
                "android.permission.INTERNET",
                "android.permission.READ_SYNC_STATS",
                "android.permission.READ_SYNC_SETTINGS",
                "android.permission.WRITE_SYNC_SETTINGS",
                "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
                "android.permission.READ_CONTACTS",
                "android.permission.WRITE_CONTACTS",
                "android.permission.READ_CALENDAR",
                "android.permission.WRITE_CALENDAR",
        }));
        if (Build.VERSION.SDK_INT <= 18) {
            expectedSet.add("android.permission.READ_EXTERNAL_STORAGE");
            expectedSet.add("android.permission.WRITE_EXTERNAL_STORAGE");
        }
        if (Build.VERSION.SDK_INT <= 22) {
            expectedSet.add("android.permission.GET_ACCOUNTS");
            expectedSet.add("android.permission.AUTHENTICATE_ACCOUNTS");
            expectedSet.add("android.permission.MANAGE_ACCOUNTS");
        }
        if (Build.VERSION.SDK_INT >= 23) {
            expectedSet.add("android.permission.CAMERA");
            if (Build.VERSION.SDK_INT <= 23) {
                expectedSet.add("android.permission.CALL_PHONE");
            }
        }
        Apk apk = actualDetails.apks.get(0);
        HashSet<String> actualSet = new HashSet<>(Arrays.asList(apk.requestedPermissions));
        for (String permission : expectedSet) {
            if (!actualSet.contains(permission)) {
                Log.i(TAG, permission + " in expected but not actual! (android-"
                        + Build.VERSION.SDK_INT + ")");
            }
        }
        for (String permission : actualSet) {
            if (!expectedSet.contains(permission)) {
                Log.i(TAG, permission + " in actual but not expected! (android-"
                        + Build.VERSION.SDK_INT + ")");
            }
        }
        String[] expectedPermissions = expectedSet.toArray(new String[expectedSet.size()]);
        assertTrue(ApkVerifier.requestedPermissionsEqual(expectedPermissions, apk.requestedPermissions));

        String[] badPermissions = Arrays.copyOf(expectedPermissions, expectedPermissions.length + 1);
        assertFalse(ApkVerifier.requestedPermissionsEqual(badPermissions, apk.requestedPermissions));
        badPermissions[badPermissions.length - 1] = "notarealpermission";
        assertFalse(ApkVerifier.requestedPermissionsEqual(badPermissions, apk.requestedPermissions));

        Uri uri = Uri.fromFile(extendedPermissionsApk);
        ApkVerifier apkVerifier = new ApkVerifier(instrumentation.getContext(), uri, apk);
        apkVerifier.verifyApk();
    }

    @NonNull
    private RepoDetails getFromFile(File indexFile) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(indexFile);
            return RepoDetails.getFromFile(inputStream, Repo.PUSH_REQUEST_IGNORE);
        } finally {
            Utils.closeQuietly(inputStream);
        }
    }
}
