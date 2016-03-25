package org.fdroid.fdroid;

import android.app.Instrumentation;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.fdroid.fdroid.compat.FileCompatForTest;
import org.fdroid.fdroid.data.SanitizedFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * This test needs to run on the emulator, even though it technically could
 * run as a plain JUnit test, because it is testing the specifics of
 * Android's symlink handling.
 */
@RunWith(AndroidJUnit4.class)
public class FileCompatTest {

    private static final String TAG = "FileCompatTest";

    private File dir;
    private SanitizedFile sourceFile;
    private SanitizedFile destFile;

    @Before
    public void setUp() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        dir = TestUtils.getWriteableDir(instrumentation);
        sourceFile = SanitizedFile.knownSanitized(TestUtils.copyAssetToDir(instrumentation.getContext(), "simpleIndex.jar", dir));
        destFile = new SanitizedFile(dir, "dest-" + UUID.randomUUID() + ".testproduct");
        assertFalse(destFile.exists());
        assertTrue(sourceFile.getAbsolutePath() + " should exist.", sourceFile.exists());
    }

    @After
    public void tearDown() {
        if (!sourceFile.delete()) {
            System.out.println("Can't delete " + sourceFile.getAbsolutePath() + ".");
        }

        if (!destFile.delete()) {
            System.out.println("Can't delete " + destFile.getAbsolutePath() + ".");
        }
    }

    @Test
    public void testSymlinkRuntime() {
        FileCompatForTest.symlinkRuntimeTest(sourceFile, destFile);
        assertTrue(destFile.getAbsolutePath() + " should exist after symlinking", destFile.exists());
    }

    @Test
    public void testSymlinkLibcore() {

        if (Build.VERSION.SDK_INT >= 19) {
            FileCompatForTest.symlinkLibcoreTest(sourceFile, destFile);
            assertTrue(destFile.getAbsolutePath() + " should exist after symlinking", destFile.exists());
        } else {
            Log.w(TAG, "Cannot test symlink-libcore on this device. Requires android-19, but this has android-" + Build.VERSION.SDK_INT);
        }
    }

    @Test
    public void testSymlinkOs() {

        if (Build.VERSION.SDK_INT >= 21) {
            FileCompatForTest.symlinkOsTest(sourceFile, destFile);
            assertTrue(destFile.getAbsolutePath() + " should exist after symlinking", destFile.exists());
        } else {
            Log.w(TAG, "Cannot test symlink-os on this device. Requires android-21, but only has android-" + Build.VERSION.SDK_INT);
        }
    }

}
