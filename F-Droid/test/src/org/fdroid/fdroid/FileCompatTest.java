package org.fdroid.fdroid;

import android.os.Build;
import android.test.InstrumentationTestCase;
import android.util.Log;
import org.fdroid.fdroid.compat.FileCompatForTest;
import org.fdroid.fdroid.data.SanitizedFile;

import java.io.File;

public class FileCompatTest extends InstrumentationTestCase {

    private static final String TAG = "org.fdroid.fdroid.FileCompatTest";

    private File dir;
    private SanitizedFile sourceFile;
    private SanitizedFile destFile;

    public void setUp() {
        dir = TestUtils.getWriteableDir(getInstrumentation());
        sourceFile = SanitizedFile.knownSanitized(TestUtils.copyAssetToDir(getInstrumentation().getContext(), "simpleIndex.jar", dir));
        destFile = new SanitizedFile(dir, "dest.txt");
        assertTrue(!destFile.exists());
        assertTrue(sourceFile.getAbsolutePath() + " should exist.", sourceFile.exists());
    }

    public void tearDown() {
        if (sourceFile.exists()) {
            assertTrue("Can't delete " + sourceFile.getAbsolutePath() + ".", sourceFile.delete());
        }

        if (destFile.exists()) {
            assertTrue("Can't delete " + destFile.getAbsolutePath() + ".", destFile.delete());
        }
    }

    public void testSymlinkRuntime() {
        SanitizedFile destFile = new SanitizedFile(dir, "dest.txt");
        assertFalse(destFile.exists());

        FileCompatForTest.symlinkRuntimeTest(sourceFile, destFile);
        assertTrue(destFile.getAbsolutePath() + " should exist after symlinking", destFile.exists());
    }

    public void testSymlinkLibcore() {

        if (Build.VERSION.SDK_INT >= 19) {
            SanitizedFile destFile = new SanitizedFile(dir, "dest.txt");
            assertFalse(destFile.exists());

            FileCompatForTest.symlinkLibcoreTest(sourceFile, destFile);
            assertTrue(destFile.getAbsolutePath() + " should exist after symlinking", destFile.exists());
        } else {
            Log.w(TAG, "Cannot test symlink-libcore on this device. Requires android-19, but this has android-" + Build.VERSION.SDK_INT);
        }
    }

    public void testSymlinkOs() {

        if (Build.VERSION.SDK_INT >= 21 ) {
            SanitizedFile destFile = new SanitizedFile(dir, "dest.txt");
            assertFalse(destFile.exists());

            FileCompatForTest.symlinkOsTest(sourceFile, destFile);
            assertTrue(destFile.getAbsolutePath() + " should exist after symlinking", destFile.exists());
        } else {
            Log.w(TAG, "Cannot test symlink-os on this device. Requires android-21, but only has android-" + Build.VERSION.SDK_INT);
        }
    }

}
