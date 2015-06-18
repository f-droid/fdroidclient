package org.fdroid.fdroid;

import android.os.Build;
import android.test.InstrumentationTestCase;
import android.util.Log;

import org.fdroid.fdroid.compat.FileCompatForTest;
import org.fdroid.fdroid.data.SanitizedFile;

import java.io.File;
import java.util.UUID;

public class FileCompatTest extends InstrumentationTestCase {

    private static final String TAG = "FileCompatTest";

    private File dir;
    private SanitizedFile sourceFile;
    private SanitizedFile destFile;

    public void setUp() {
        dir = TestUtils.getWriteableDir(getInstrumentation());
        sourceFile = SanitizedFile.knownSanitized(TestUtils.copyAssetToDir(getInstrumentation().getContext(), "simpleIndex.jar", dir));
        destFile = new SanitizedFile(dir, "dest-" + UUID.randomUUID() + ".testproduct");
        assertFalse(destFile.exists());
        assertTrue(sourceFile.getAbsolutePath() + " should exist.", sourceFile.exists());
    }

    public void tearDown() {
        if (!sourceFile.delete()) {
            System.out.println("Can't delete " + sourceFile.getAbsolutePath() + ".");
        }

        if (!destFile.delete()) {
            System.out.println("Can't delete " + destFile.getAbsolutePath() + ".");
        }
    }

    public void testSymlinkRuntime() {
        FileCompatForTest.symlinkRuntimeTest(sourceFile, destFile);
        assertTrue(destFile.getAbsolutePath() + " should exist after symlinking", destFile.exists());
    }

    public void testSymlinkLibcore() {

        if (Build.VERSION.SDK_INT >= 19) {
            FileCompatForTest.symlinkLibcoreTest(sourceFile, destFile);
            assertTrue(destFile.getAbsolutePath() + " should exist after symlinking", destFile.exists());
        } else {
            Log.w(TAG, "Cannot test symlink-libcore on this device. Requires android-19, but this has android-" + Build.VERSION.SDK_INT);
        }
    }

    public void testSymlinkOs() {

        if (Build.VERSION.SDK_INT >= 21 ) {
            FileCompatForTest.symlinkOsTest(sourceFile, destFile);
            assertTrue(destFile.getAbsolutePath() + " should exist after symlinking", destFile.exists());
        } else {
            Log.w(TAG, "Cannot test symlink-os on this device. Requires android-21, but only has android-" + Build.VERSION.SDK_INT);
        }
    }

}
