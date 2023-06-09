package org.fdroid.fdroid.compat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.app.Instrumentation;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.fdroid.fdroid.AssetUtils;
import org.fdroid.fdroid.data.SanitizedFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.UUID;


/**
 * This test needs to run on the emulator, even though it technically could
 * run as a plain JUnit test, because it is testing the specifics of
 * Android's symlink handling.
 */
@RunWith(AndroidJUnit4.class)
public class FileCompatTest {

    private static final String TAG = "FileCompatTest";

    private SanitizedFile sourceFile;
    private SanitizedFile destFile;

    @Before
    public void setUp() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        File dir = getWriteableDir(instrumentation);
        sourceFile = SanitizedFile.knownSanitized(
                AssetUtils.copyAssetToDir(instrumentation.getContext(), "simpleIndex.jar", dir));
        destFile = new SanitizedFile(dir, "dest-" + UUID.randomUUID() + ".testproduct");
        assertFalse(destFile.exists());
        assertTrue(sourceFile.getAbsolutePath() + " should exist.", sourceFile.exists());
    }

    @After
    public void tearDown() {
        if (!sourceFile.delete()) {
            Log.w(TAG, "Can't delete " + sourceFile.getAbsolutePath() + ".");
        }

        if (!destFile.delete()) {
            Log.w(TAG, "Can't delete " + destFile.getAbsolutePath() + ".");
        }
    }

    @Test
    public void testSymlinkRuntime() {
        FileCompat.symlinkRuntime(sourceFile, destFile);
        assertTrue(destFile.getAbsolutePath() + " should exist after symlinking", destFile.exists());
    }

    @Test
    public void testSymlinkLibcore() {
        assumeTrue(Build.VERSION.SDK_INT >= 19);
        FileCompat.symlinkLibcore(sourceFile, destFile);
        assertTrue(destFile.getAbsolutePath() + " should exist after symlinking", destFile.exists());
    }

    @Test
    public void testSymlinkOs() {
        assumeTrue(Build.VERSION.SDK_INT >= 21);
        FileCompat.symlinkOs(sourceFile, destFile);
        assertTrue(destFile.getAbsolutePath() + " should exist after symlinking", destFile.exists());
    }

    /**
     * Prefer internal over external storage, because external tends to be FAT filesystems,
     * which don't support symlinks (which we test using this method).
     */
    public static File getWriteableDir(Instrumentation instrumentation) {
        Context context = instrumentation.getContext();
        Context targetContext = instrumentation.getTargetContext();

        File[] dirsToTry = new File[]{
                context.getCacheDir(),
                context.getFilesDir(),
                targetContext.getCacheDir(),
                targetContext.getFilesDir(),
                context.getExternalCacheDir(),
                context.getExternalFilesDir(null),
                targetContext.getExternalCacheDir(),
                targetContext.getExternalFilesDir(null),
                Environment.getExternalStorageDirectory(),
        };

        return getWriteableDir(dirsToTry);
    }

    private static File getWriteableDir(File[] dirsToTry) {

        for (File dir : dirsToTry) {
            if (dir != null && dir.canWrite()) {
                return dir;
            }
        }

        return null;
    }
}
