package org.fdroid.fdroid.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.ContextWrapper;
import android.os.Environment;
import android.webkit.MimeTypeMap;

import androidx.test.core.app.ApplicationProvider;

import org.apache.commons.io.FileUtils;
import org.fdroid.fdroid.installer.ApkCache;
import org.fdroid.fdroid.nearby.PublicSourceDirProvider;
import org.fdroid.index.v2.FileV1;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowMimeTypeMap;

import java.io.File;
import java.io.IOException;
import java.net.URL;

@RunWith(RobolectricTestRunner.class)
public class ApkTest {

    private final ContextWrapper context = ApplicationProvider.getApplicationContext();

    @Before
    public final void setUp() {
        ShadowMimeTypeMap mimeTypeMap = Shadows.shadowOf(MimeTypeMap.getSingleton());
        mimeTypeMap.addExtensionMimeTypMapping("apk", "application/vnd.android.package-archive");
        mimeTypeMap.addExtensionMimeTypMapping("obf", "application/octet-stream");
        mimeTypeMap.addExtensionMimeTypMapping("zip", PublicSourceDirProvider.SHARE_APK_MIME_TYPE);
        ShadowLog.stream = System.out;
    }

    @Test(expected = IllegalStateException.class)
    public void testGetMediaInstallPathWithApk() {
        Apk apk = new Apk();
        apk.apkFile = new FileV1("test.apk", "hash", null, null);
        apk.repoAddress = "https://example.com/fdroid/repo";
        apk.canonicalRepoAddress = "https://example.com/fdroid/repo";
        assertTrue(apk.isApk());
        apk.getMediaInstallPath(context);
    }

    @Test
    public void testGetMediaInstallPathWithOta() throws IOException {
        Apk apk = new Apk();
        apk.apkFile = new FileV1("org.fdroid.fdroid.privileged.ota_2110.zip", "hash", null, null);
        apk.repoAddress = "https://example.com/fdroid/repo";
        apk.canonicalRepoAddress = "https://example.com/fdroid/repo";
        assertFalse(apk.isApk());
        copyResourceFileToCache(apk);
        File path = apk.getMediaInstallPath(context);
        assertEquals(new File(context.getApplicationInfo().dataDir + "/ota"), path);
    }

    @Test
    public void testGetMediaInstallPathWithObf() {
        Apk apk = new Apk();
        apk.apkFile = new FileV1("Norway_bouvet_europe_2.obf", "hash", null, null);
        apk.repoAddress = "https://example.com/fdroid/repo";
        apk.canonicalRepoAddress = "https://example.com/fdroid/repo";
        assertFalse(apk.isApk());
        File path = apk.getMediaInstallPath(context);
        assertEquals(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), path);
    }

    @Test
    public void testGetMediaInstallPathWithObfZip() throws IOException {
        Apk apk = new Apk();
        apk.apkFile = new FileV1("Norway_bouvet_europe_2.obf.zip", "hash", null, null);
        apk.repoAddress = "https://example.com/fdroid/repo";
        apk.canonicalRepoAddress = "https://example.com/fdroid/repo";
        assertFalse(apk.isApk());
        copyResourceFileToCache(apk);
        File path = apk.getMediaInstallPath(context);
        assertEquals(context.getCacheDir(), path);
    }

    private void copyResourceFileToCache(Apk apk) throws IOException {
        URL res = getClass().getClassLoader().getResource(apk.apkFile.getName());
        FileUtils.copyInputStreamToFile(res.openStream(),
                ApkCache.getApkDownloadPath(context, apk.getCanonicalUrl()));
    }
}
