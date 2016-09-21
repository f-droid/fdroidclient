package org.fdroid.fdroid.compat;

import android.annotation.TargetApi;
import android.os.Build;
import android.system.ErrnoException;
import android.util.Log;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.SanitizedFile;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * This class works only with {@link SanitizedFile} instances to enforce
 * filtering of the file names from files downloaded from the internet.
 * This helps prevent things like SQL injection, shell command injection
 * and other attacks based on putting various characters into filenames.
 */
public class FileCompat {

    private static final String TAG = "FileCompat";

    public static boolean symlink(SanitizedFile source, SanitizedFile dest) {

        if (Build.VERSION.SDK_INT >= 21) {
            symlinkOs(source, dest);
        } else if (Build.VERSION.SDK_INT >= 15) {
            symlinkLibcore(source, dest);
        } else {
            symlinkRuntime(source, dest);
        }

        return dest.exists();
    }

    /**
     * Moved into a separate class rather than just a method, so that phones without API 21 will
     * not attempt to load this class at runtime. Otherwise, using the Os.symlink method will cause
     * a VerifyError to be thrown at runtime when the FileCompat class is first used.
     */
    private static class Symlink21 {

        @TargetApi(21)
        void symlink(SanitizedFile source, SanitizedFile dest) {
            try {
                android.system.Os.symlink(source.getAbsolutePath(), dest.getAbsolutePath());
            } catch (ErrnoException e) {
                // Do nothing...
            }
        }

    }

    @TargetApi(21)
    static void symlinkOs(SanitizedFile source, SanitizedFile dest) {
        new Symlink21().symlink(source, dest);
    }

    static void symlinkRuntime(SanitizedFile source, SanitizedFile dest) {
        String[] commands = {
            FDroidApp.SYSTEM_DIR_NAME + "/bin/ln",
            "-s",
            source.getAbsolutePath(),
            dest.getAbsolutePath(),
        };
        try {
            Utils.debugLog(TAG, "Executing command: " + commands[0] + " " + commands[1]
                    + " " + commands[2] + " " + commands[3]);
            Process proc = Runtime.getRuntime().exec(commands);
            Utils.consumeStream(proc.getInputStream());
            Utils.consumeStream(proc.getErrorStream());
        } catch (IOException e) {
            // Do nothing
        }
    }

    static void symlinkLibcore(SanitizedFile source, SanitizedFile dest) {
        try {
            Object os = Class.forName("libcore.io.Libcore").getField("os").get(null);
            Method symlink = os.getClass().getMethod("symlink", String.class, String.class);
            symlink.invoke(os, source.getAbsolutePath(), dest.getAbsolutePath());
        } catch (Exception e) {
            // Should catch more specific exceptions than just "Exception" here, but there are
            // some which come from libcore.io.Libcore, which we don't have access to at compile time.
            Log.e(TAG, "Could not symlink " + source.getAbsolutePath() + " to " + dest.getAbsolutePath(), e);
        }
    }

}
