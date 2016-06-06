package org.fdroid.fdroid;

import android.app.Instrumentation;
import android.content.Context;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TestUtils {

    private static final String TAG = "TestUtils";

    @Nullable
    public static File copyAssetToDir(Context context, String assetName, File directory) {
        File tempFile;
        InputStream input = null;
        OutputStream output = null;
        try {
            tempFile = File.createTempFile(assetName + "-", ".testasset", directory);
            Log.i(TAG, "Copying asset file " + assetName + " to directory " + directory);
            input = context.getAssets().open(assetName);
            output = new FileOutputStream(tempFile);
            Utils.copy(input, output);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            Utils.closeQuietly(output);
            Utils.closeQuietly(input);
        }
        return tempFile;
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
