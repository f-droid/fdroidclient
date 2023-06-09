package org.fdroid.fdroid.work;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.nearby.LocalRepoManager;
import org.fdroid.fdroid.shadows.ShadowLog;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Test non-time-based cache deletion methods.  Robolectric is lacking full
 * support for getting time from files, so methods that rely on time must be
 * tested in {@code CleanCacheWorkerTest} in {@code androidTest}.
 */
@RunWith(RobolectricTestRunner.class)
public class CleanCacheWorkerTest {
    public static final String TAG = "CleanCacheWorkerTest";

    private static final Context CONTEXT = ApplicationProvider.getApplicationContext();
    private static final File FILES_DIR = CONTEXT.getFilesDir();

    @Before
    public void setUp() {
        ShadowLog.stream = System.out;
        Preferences.setupForTests(CONTEXT);
    }

    @Test
    public void testDeleteOldInstallerFiles() throws IOException {
        Assume.assumeTrue(BuildConfig.FLAVOR.startsWith("full"));
        ArrayList<File> webRootAssetFiles = new ArrayList<>();
        File indexHtml = new File(FILES_DIR, "index.html");
        assertTrue(indexHtml.createNewFile());
        webRootAssetFiles.add(indexHtml);
        for (String name : LocalRepoManager.WEB_ROOT_ASSET_FILES) {
            File f = new File(FILES_DIR, name);
            assertTrue(f.createNewFile());
            webRootAssetFiles.add(f);
        }
        File apk = new File(FILES_DIR, "fake.apk");
        assertTrue(apk.createNewFile());
        File giantblob = new File(FILES_DIR, "giantblob");
        assertTrue(giantblob.createNewFile());
        File obf = new File(FILES_DIR, "fake.obf");
        assertTrue(obf.createNewFile());
        File zip = new File(FILES_DIR, "fake.zip");
        assertTrue(zip.createNewFile());
        CleanCacheWorker.deleteOldInstallerFiles(CONTEXT);
        assertFalse(apk.exists());
        assertFalse(giantblob.exists());
        assertFalse(obf.exists());
        assertFalse(zip.exists());
        for (File f : webRootAssetFiles) {
            assertTrue(f.exists());
        }
    }

    /**
     * Pure smoke check, Robolectric does not support file times fully.
     */
    @Test
    public void testDeleteExpiredApksFromCache() {
        CleanCacheWorker.deleteExpiredApksFromCache(CONTEXT);
    }

    /**
     * Pure smoke check, Robolectric does not support file times fully.
     */
    @Test
    public void testDeleteStrayIndexFiles() {
        CleanCacheWorker.deleteStrayIndexFiles(CONTEXT);
    }

    /**
     * Pure smoke check, Robolectric does not support file times fully.
     */
    @Test
    public void testDeleteOldIcons() {
        CleanCacheWorker.deleteOldIcons(CONTEXT);
    }
}
