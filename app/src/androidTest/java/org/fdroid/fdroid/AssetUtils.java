package org.fdroid.fdroid;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.fail;

public class AssetUtils {

    private static final String TAG = "Utils";

    @Nullable
    public static File copyAssetToDir(Context context, String assetName, File directory) {
        File tempFile = null;
        InputStream input = null;
        OutputStream output = null;
        try {
            tempFile = File.createTempFile(assetName, ".testasset", directory);
            Log.i(TAG, "Copying asset file " + assetName + " to directory " + directory);
            input = context.getAssets().open(assetName);
            output = new FileOutputStream(tempFile);
            Utils.copy(input, output);
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            Utils.closeQuietly(output);
            Utils.closeQuietly(input);
        }
        return tempFile;
    }

}
