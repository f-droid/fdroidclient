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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.Instrumentation;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.fdroid.fdroid.AssetUtils;
import org.fdroid.fdroid.compat.FileCompatTest;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.index.v2.PermissionV2;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.TreeSet;

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

    private Instrumentation instrumentation;

    private File sdk14Apk;
    private File minMaxApk;
    private File extendedPermissionsApk;

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
        assertTrue(sdk14Apk.exists());
        assertTrue(minMaxApk.exists());
        assertTrue(extendedPermissionsApk.exists());
    }

    @Test
    public void testNulls() {
        assertTrue(ApkVerifier.requestedPermissionsEqual(null, null));

        String[] perms = new String[]{"Blah"};
        assertFalse(ApkVerifier.requestedPermissionsEqual(perms, null));
        assertFalse(ApkVerifier.requestedPermissionsEqual(null, perms));
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
        apk.requestedPermissions = permissionsList.toArray(new String[0]);

        Uri uri = Uri.fromFile(minMaxApk);
        ApkVerifier apkVerifier = new ApkVerifier(instrumentation.getContext(), uri, apk);
        apkVerifier.verifyApk();

        permissionsList.add("ADDITIONAL_PERMISSION");
        apk.requestedPermissions = permissionsList.toArray(new String[0]);
        apkVerifier.verifyApk();
    }

    @Test
    public void testWithPrefix() {
        Apk apk = new Apk();
        apk.packageName = "org.fdroid.permissions.sdk14";
        apk.targetSdkVersion = 14;
        TreeSet<String> expectedSet = new TreeSet<>(Arrays.asList(
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
                "android.permission.READ_CALL_LOG"// implied-permission!
        ));
        if (Build.VERSION.SDK_INT >= 29) {
            expectedSet.add("android.permission.ACCESS_MEDIA_LOCATION");
        }
        apk.requestedPermissions = expectedSet.toArray(new String[0]);

        Uri uri = Uri.fromFile(sdk14Apk);

        ApkVerifier apkVerifier = new ApkVerifier(instrumentation.getContext(), uri, apk);

        try {
            apkVerifier.verifyApk();
        } catch (ApkVerifier.ApkVerificationException |
                 ApkVerifier.ApkPermissionUnequalException e) {
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
    public void testExtendedPerms()
            throws ApkVerifier.ApkPermissionUnequalException, ApkVerifier.ApkVerificationException {
        HashSet<String> expectedSet = new HashSet<>(Arrays.asList(
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
                "android.permission.WRITE_CALENDAR"
        ));
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
        Apk apk = new Apk();
        apk.packageName = "urzip.at.or.at.urzip";
        ArrayList<PermissionV2> perms = new ArrayList<>();
        perms.add(new PermissionV2("android.permission.READ_EXTERNAL_STORAGE", 18));
        perms.add(new PermissionV2("android.permission.WRITE_SYNC_SETTINGS", null));
        perms.add(new PermissionV2("android.permission.ACCESS_NETWORK_STATE", null));
        perms.add(new PermissionV2("android.permission.WRITE_EXTERNAL_STORAGE", 18));
        perms.add(new PermissionV2("android.permission.WRITE_CONTACTS", null));
        perms.add(new PermissionV2("android.permission.ACCESS_WIFI_STATE", null));
        perms.add(new PermissionV2("android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS", null));
        perms.add(new PermissionV2("android.permission.WRITE_CALENDAR", null));
        perms.add(new PermissionV2("android.permission.READ_CONTACTS", null));
        perms.add(new PermissionV2("android.permission.READ_SYNC_SETTINGS", null));
        perms.add(new PermissionV2("android.permission.MANAGE_ACCOUNTS", 22));
        perms.add(new PermissionV2("android.permission.INTERNET", null));
        perms.add(new PermissionV2("android.permission.AUTHENTICATE_ACCOUNTS", 22));
        perms.add(new PermissionV2("android.permission.GET_ACCOUNTS", 22));
        perms.add(new PermissionV2("android.permission.READ_CALENDAR", null));
        perms.add(new PermissionV2("android.permission.READ_SYNC_STATS", null));
        apk.setRequestedPermissions(perms, 0);
        ArrayList<PermissionV2> perms23 = new ArrayList<>();
        perms23.add(new PermissionV2("android.permission.CAMERA", null));
        perms23.add(new PermissionV2("android.permission.CALL_PHONE", 23));
        apk.setRequestedPermissions(perms23, 23);
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
        String[] expectedPermissions = expectedSet.toArray(new String[0]);
        assertTrue(ApkVerifier.requestedPermissionsEqual(expectedPermissions, apk.requestedPermissions));

        String[] badPermissions = Arrays.copyOf(expectedPermissions, expectedPermissions.length + 1);
        assertFalse(ApkVerifier.requestedPermissionsEqual(badPermissions, apk.requestedPermissions));
        badPermissions[badPermissions.length - 1] = "notarealpermission";
        assertFalse(ApkVerifier.requestedPermissionsEqual(badPermissions, apk.requestedPermissions));

        Uri uri = Uri.fromFile(extendedPermissionsApk);
        ApkVerifier apkVerifier = new ApkVerifier(instrumentation.getContext(), uri, apk);
        apkVerifier.verifyApk();
    }

    @Test
    public void testImpliedPerms() {
        TreeSet<String> expectedSet = new TreeSet<>(Arrays.asList(
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.ACCESS_WIFI_STATE",
                "android.permission.INTERNET",
                "android.permission.READ_CALENDAR",
                "android.permission.READ_CONTACTS",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.READ_SYNC_SETTINGS",
                "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
                "android.permission.WRITE_CALENDAR",
                "android.permission.WRITE_CONTACTS",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.WRITE_SYNC_SETTINGS",
                "org.dmfs.permission.READ_TASKS",
                "org.dmfs.permission.WRITE_TASKS"
        ));
        if (Build.VERSION.SDK_INT <= 22) { // maxSdkVersion="22"
            expectedSet.addAll(Arrays.asList(
                    "android.permission.AUTHENTICATE_ACCOUNTS",
                    "android.permission.GET_ACCOUNTS",
                    "android.permission.MANAGE_ACCOUNTS"
            ));
        }
        if (Build.VERSION.SDK_INT >= 29) {
            expectedSet.add("android.permission.ACCESS_MEDIA_LOCATION");
        }
        Apk apk = new Apk();
        apk.packageName = "urzip.at.or.at.urzip";
        apk.targetSdkVersion = 24;
        ArrayList<PermissionV2> perms = new ArrayList<>();
        perms.add(new PermissionV2("android.permission.READ_EXTERNAL_STORAGE", 18));
        perms.add(new PermissionV2("android.permission.WRITE_SYNC_SETTINGS", null));
        perms.add(new PermissionV2("android.permission.ACCESS_NETWORK_STATE", null));
        perms.add(new PermissionV2("android.permission.WRITE_EXTERNAL_STORAGE", null));
        perms.add(new PermissionV2("android.permission.WRITE_CONTACTS", null));
        perms.add(new PermissionV2("android.permission.ACCESS_WIFI_STATE", null));
        perms.add(new PermissionV2("android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS", null));
        perms.add(new PermissionV2("android.permission.WRITE_CALENDAR", null));
        perms.add(new PermissionV2("android.permission.READ_CONTACTS", null));
        perms.add(new PermissionV2("android.permission.READ_SYNC_SETTINGS", null));
        perms.add(new PermissionV2("android.permission.MANAGE_ACCOUNTS", 22));
        perms.add(new PermissionV2("android.permission.INTERNET", null));
        perms.add(new PermissionV2("android.permission.AUTHENTICATE_ACCOUNTS", 22));
        perms.add(new PermissionV2("android.permission.GET_ACCOUNTS", 22));
        perms.add(new PermissionV2("android.permission.READ_CALENDAR", null));
        perms.add(new PermissionV2("org.dmfs.permission.READ_TASKS", null));
        perms.add(new PermissionV2("org.dmfs.permission.WRITE_TASKS", null));
        apk.setRequestedPermissions(perms, 0);
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
        String[] expectedPermissions = expectedSet.toArray(new String[0]);
        assertTrue(ApkVerifier.requestedPermissionsEqual(expectedPermissions, apk.requestedPermissions));

        expectedSet = new TreeSet<>(Arrays.asList(
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.ACCESS_WIFI_STATE",
                "android.permission.AUTHENTICATE_ACCOUNTS",
                "android.permission.GET_ACCOUNTS",
                "android.permission.INTERNET",
                "android.permission.MANAGE_ACCOUNTS",
                "android.permission.READ_CALENDAR",
                "android.permission.READ_CONTACTS",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.READ_SYNC_SETTINGS",
                "android.permission.WRITE_CALENDAR",
                "android.permission.WRITE_CONTACTS",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.WRITE_SYNC_SETTINGS",
                "org.dmfs.permission.READ_TASKS",
                "org.dmfs.permission.WRITE_TASKS"
        ));
        if (Build.VERSION.SDK_INT >= 29) {
            expectedSet.add("android.permission.ACCESS_MEDIA_LOCATION");
        }
        expectedPermissions = expectedSet.toArray(new String[expectedSet.size()]);
        apk = new Apk();
        apk.packageName = "urzip.at.or.at.urzip";
        apk.targetSdkVersion = 23;
        perms = new ArrayList<>();
        perms.add(new PermissionV2("android.permission.WRITE_SYNC_SETTINGS", null));
        perms.add(new PermissionV2("android.permission.ACCESS_NETWORK_STATE", null));
        perms.add(new PermissionV2("android.permission.WRITE_EXTERNAL_STORAGE", null));
        perms.add(new PermissionV2("android.permission.WRITE_CONTACTS", null));
        perms.add(new PermissionV2("android.permission.ACCESS_WIFI_STATE", null));
        perms.add(new PermissionV2("android.permission.WRITE_CALENDAR", null));
        perms.add(new PermissionV2("android.permission.READ_CONTACTS", null));
        perms.add(new PermissionV2("android.permission.READ_SYNC_SETTINGS", null));
        perms.add(new PermissionV2("android.permission.MANAGE_ACCOUNTS", null));
        perms.add(new PermissionV2("android.permission.INTERNET", null));
        perms.add(new PermissionV2("android.permission.AUTHENTICATE_ACCOUNTS", null));
        perms.add(new PermissionV2("android.permission.GET_ACCOUNTS", null));
        perms.add(new PermissionV2("android.permission.READ_CALENDAR", null));
        perms.add(new PermissionV2("org.dmfs.permission.READ_TASKS", null));
        perms.add(new PermissionV2("org.dmfs.permission.WRITE_TASKS", null));
        apk.setRequestedPermissions(perms, 0);
        actualSet = new HashSet<>(Arrays.asList(apk.requestedPermissions));
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
        assertTrue(ApkVerifier.requestedPermissionsEqual(expectedPermissions, apk.requestedPermissions));
    }

}
