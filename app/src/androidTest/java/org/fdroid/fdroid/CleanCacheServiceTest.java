package org.fdroid.fdroid;

import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.apache.commons.io.FileUtils;
import org.fdroid.fdroid.compat.FileCompatTest;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class CleanCacheServiceTest {

    public static final String TAG = "CleanCacheServiceTest";

    @Test
    public void testClearOldFiles() throws IOException, InterruptedException {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        File tempDir = FileCompatTest.getWriteableDir(instrumentation);
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

        CleanCacheService.clearOldFiles(dir, 3000);  // check all in dir
        assertFalse(first.exists());
        assertTrue(second.exists());

        Thread.sleep(7000);
        CleanCacheService.clearOldFiles(second, 3000);  // check just second file
        assertFalse(first.exists());
        assertFalse(second.exists());

        // make sure it doesn't freak out on a non-existant file
        File nonexistant = new File(tempDir, "nonexistant");
        CleanCacheService.clearOldFiles(nonexistant, 1);
        CleanCacheService.clearOldFiles(null, 1);
    }
}
