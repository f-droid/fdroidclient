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
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.fdroid.fdroid.AssetUtils;
import org.fdroid.fdroid.data.Apk;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This test checks the ApkVerifier by parsing a repo from permissionsRepo.xml
 * and checking the listed permissions against the ones specified in apks' AndroidManifest,
 * which have been specifically generated for this test.
 * - the apk file name must match the package name in the xml
 * - the versionName of listed apks inside the repo have either a good or bad outcome.
 * this must be defined in GOOD_VERSION_NAMES and BAD_VERSION_NAMES.
 * <p/>
 * NOTE: This androidTest cannot run as a Robolectric test because the
 * required methods from PackageManger are not included in Robolectric's Android API.
 * java.lang.NoClassDefFoundError: java/util/jar/StrictJarFile
 * at android.content.pm.PackageManager.getPackageArchiveInfo(PackageManager.java:3545)
 */
@RunWith(AndroidJUnit4.class)
public class ApkVerifierTest {

    Instrumentation instrumentation;

    File sdk14Apk;
    File minMaxApk;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        File dir = null;
        try {
            dir = tempFolder.newFolder("apks");
        } catch (IOException e) {
            fail(e.getMessage());
        }
        sdk14Apk = AssetUtils.copyAssetToDir(instrumentation.getContext(),
                "org.fdroid.permissions.sdk14.apk",
                dir
        );
        minMaxApk = AssetUtils.copyAssetToDir(instrumentation.getContext(),
                "org.fdroid.permissions.minmax.apk",
                dir
        );
        assertTrue(sdk14Apk.exists());
        assertTrue(minMaxApk.exists());
    }

    @Test
    public void testWithoutPrefix() {
        Apk apk = new Apk();
        apk.packageName = "org.fdroid.permissions.sdk14";
        apk.targetSdkVersion = 14;
        apk.permissions = new String[]{
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

        Uri uri = Uri.fromFile(sdk14Apk);

        ApkVerifier apkVerifier = new ApkVerifier(instrumentation.getContext(), uri, apk);

        try {
            apkVerifier.verifyApk();
        } catch (ApkVerifier.ApkVerificationException | ApkVerifier.ApkPermissionUnequalException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testWithPrefix() {
        Apk apk = new Apk();
        apk.packageName = "org.fdroid.permissions.sdk14";
        apk.targetSdkVersion = 14;
        apk.permissions = new String[]{
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
    @Test
    public void testAdditionalPermission() {
        Apk apk = new Apk();
        apk.packageName = "org.fdroid.permissions.sdk14";
        apk.targetSdkVersion = 14;
        apk.permissions = new String[]{
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
                "NEW_PERMISSION",
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
     * Missing permissions are not okay!
     * The user is then not warned about a permission that the apk uses!
     */
    @Test
    public void testMissingPermission() {
        Apk apk = new Apk();
        apk.packageName = "org.fdroid.permissions.sdk14";
        apk.targetSdkVersion = 14;
        apk.permissions = new String[]{
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

}
