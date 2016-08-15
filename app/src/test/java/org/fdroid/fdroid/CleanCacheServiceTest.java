package org.fdroid.fdroid;

import org.apache.commons.io.FileUtils;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.CleanCacheService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

// TODO: Use sdk=24 when Robolectric supports this
@Config(constants = BuildConfig.class, sdk = 23)
@RunWith(RobolectricGradleTestRunner.class)
public class CleanCacheServiceTest {

    @Test
    public void testClearOldFiles() throws IOException, InterruptedException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        assertTrue(tempDir.isDirectory());
        assertTrue(tempDir.canWrite());

        File dir = new File(tempDir, "F-Droid-test.clearOldFiles");
        FileUtils.deleteQuietly(dir);
        assertTrue(dir.mkdirs());
        assertTrue(dir.isDirectory());

        File first = new File(dir, "first");
        first.deleteOnExit();

        File second = new File(dir, "second");
        second.deleteOnExit();

        assertFalse(first.exists());
        assertFalse(second.exists());

        assertTrue(first.createNewFile());
        assertTrue(first.exists());

        Thread.sleep(7000);
        assertTrue(second.createNewFile());
        assertTrue(second.exists());

        CleanCacheService.clearOldFiles(dir, 3);
        assertFalse(first.exists());
        assertTrue(second.exists());

        Thread.sleep(7000);
        CleanCacheService.clearOldFiles(dir, 3);
        assertFalse(first.exists());
        assertFalse(second.exists());
    }
}
