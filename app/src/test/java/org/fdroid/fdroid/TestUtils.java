package org.fdroid.fdroid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.fail;

public class TestUtils {

    private static final String TAG = "TestUtils";

    public static File copyResourceToTempFile(String resourceName) {
        File tempFile = null;
        InputStream input = null;
        OutputStream output = null;
        try {
            tempFile = File.createTempFile(resourceName + "-", ".testasset");
            input = TestUtils.class.getClassLoader().getResourceAsStream(resourceName);
            output = new FileOutputStream(tempFile);
            Utils.copy(input, output);
        } catch (IOException e) {
            e.printStackTrace();
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
            fail();
            return null;
        } finally {
            Utils.closeQuietly(output);
            Utils.closeQuietly(input);
        }
        return tempFile;
    }

}
