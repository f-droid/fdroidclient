package org.fdroid.fdroid.compat;

import android.annotation.TargetApi;
import android.os.Build;

import org.fdroid.fdroid.data.SanitizedFile;

/**
 * Used to expose the protected methods from FileCompat in a public manner so
 * that they can be called from a test harness.
 */
public class FileCompatForTest extends FileCompat {

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void symlinkOsTest(SanitizedFile source, SanitizedFile dest) {
        symlinkOs(source, dest);
    }

    public static void symlinkRuntimeTest(SanitizedFile source, SanitizedFile dest) {
        symlinkRuntime(source, dest);
    }

    public static void symlinkLibcoreTest(SanitizedFile source, SanitizedFile dest) {
        symlinkLibcore(source, dest);
    }

}
