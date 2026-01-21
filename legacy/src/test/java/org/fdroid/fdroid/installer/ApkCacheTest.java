package org.fdroid.fdroid.installer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.ContextWrapper;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import java.io.File;

@RunWith(RobolectricTestRunner.class)
public class ApkCacheTest {
    private static final String TAG = "ApkCacheTest";

    private ContextWrapper context;
    private File cacheDir;

    @Before
    public final void setUp() {
        context = ApplicationProvider.getApplicationContext();
        cacheDir = ApkCache.getApkCacheDir(context);
        ShadowLog.stream = System.out;
    }

    @Test
    public void testGetApkCacheDir() {
        Log.i(TAG, "path: " + cacheDir);
        assertTrue("Must be full path", cacheDir.isAbsolute());
        assertTrue("Must be a directory", cacheDir.isDirectory());
        assertTrue("Must be writable", cacheDir.canWrite());
    }

    @Test
    public void testGetApkDownloadPath() {
        assertEquals("Should be in folder based on repo hostname",
                new File(cacheDir, "f-droid.org--1/org.fdroid.fdroid_1008000.apk"),
                ApkCache.getApkDownloadPath(context,
                        "https://f-droid.org/repo/org.fdroid.fdroid_1008000.apk"));
        assertEquals("Should be in folder based on repo hostname with port number",
                new File(cacheDir, "192.168.234.12-8888/sun.bob.leela_2.apk"),
                ApkCache.getApkDownloadPath(context,
                        "http://192.168.234.12:8888/fdroid/repo/sun.bob.leela_2.apk"));
        assertEquals("Should work for OTA files also",
                new File(cacheDir, "f-droid.org--1/org.fdroid.fdroid.privileged.ota_2110.zip"),
                ApkCache.getApkDownloadPath(context,
                        "http://f-droid.org/fdroid/repo/org.fdroid.fdroid.privileged.ota_2110.zip"));
        assertEquals("Should work for ZIP files also",
                new File(cacheDir, "example.com--1/Norway_bouvet_europe_2.obf.zip"),
                ApkCache.getApkDownloadPath(context,
                        "https://example.com/fdroid/repo/Norway_bouvet_europe_2.obf.zip"));
        assertEquals("Should work for OBF files also",
                new File(cacheDir, "example.com--1/Norway_bouvet_europe_2.obf"),
                ApkCache.getApkDownloadPath(context,
                        "https://example.com/fdroid/repo/Norway_bouvet_europe_2.obf"));
    }
}
