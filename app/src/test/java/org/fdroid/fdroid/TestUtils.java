package org.fdroid.fdroid;

import android.content.ContentResolver;
import android.content.ContextWrapper;

import org.mockito.AdditionalAnswers;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowContentResolver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class TestUtils {

    @SuppressWarnings("unused")
    private static final String TAG = "TestUtils"; // NOPMD

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
                assertTrue(tempFile.delete());
            }
            fail();
            return null;
        } finally {
            Utils.closeQuietly(output);
            Utils.closeQuietly(input);
        }
        return tempFile;
    }

    /**
     * The way that Robolectric has to implement shadows for Android classes such as {@link android.content.ContentProvider}
     * is by using a special annotation that means the classes will implement the correct methods at runtime.
     * However this means that the shadow of a content provider does not actually extend
     * {@link android.content.ContentProvider}. As such, we need to do some special mocking using
     * Mockito in order to provide a {@link ContextWrapper} which is able to return a proper
     * content resolver that delegates to the Robolectric shadow object.
     */
    public static ContextWrapper createContextWithContentResolver(ShadowContentResolver contentResolver) {
        final ContentResolver resolver = mock(ContentResolver.class, AdditionalAnswers.delegatesTo(contentResolver));
        return new ContextWrapper(RuntimeEnvironment.application.getApplicationContext()) {
            @Override
            public ContentResolver getContentResolver() {
                return resolver;
            }
        };
    }
}
