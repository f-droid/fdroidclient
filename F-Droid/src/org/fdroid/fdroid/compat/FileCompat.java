package org.fdroid.fdroid.compat;

import android.annotation.TargetApi;
import android.os.Build;
import android.system.ErrnoException;
import android.util.Log;

import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.SanitizedFile;

import java.io.IOException;
import java.lang.reflect.Method;

public class FileCompat extends Compatibility {

    private static final String TAG = "FileCompat";

    public static boolean symlink(SanitizedFile source, SanitizedFile dest) {

        if (hasApi(21)) {
            symlinkOs(source, dest);
        } else if (hasApi(15)) {
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

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public void symlink(SanitizedFile source, SanitizedFile dest) {
            try {
                android.system.Os.symlink(source.getAbsolutePath(), dest.getAbsolutePath());
            } catch (ErrnoException e) {
                // Do nothing...
            }
        }

    }

    @TargetApi(21)
    protected static void symlinkOs(SanitizedFile source, SanitizedFile dest) {
        new Symlink21().symlink(source, dest);
    }

    protected static void symlinkRuntime(SanitizedFile source, SanitizedFile dest) {
        String[] commands = {
            "/system/bin/ln",
            source.getAbsolutePath(),
            dest.getAbsolutePath()
        };
        try {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Executing command: " + commands[0] + " " + commands[1] + " " + commands[2]);
            }
            Process proc = Runtime.getRuntime().exec(commands);
            Utils.consumeStream(proc.getInputStream());
            Utils.consumeStream(proc.getErrorStream());
        } catch (IOException e) {
            // Do nothing
        }
    }

    protected static void symlinkLibcore(SanitizedFile source, SanitizedFile dest) {
        try {
            Object os = Class.forName("libcore.io.Libcore").getField("os").get(null);
            Method symlink = os.getClass().getMethod("symlink", String.class, String.class);
            symlink.invoke(os, source.getAbsolutePath(), dest.getAbsolutePath());
        } catch (Exception e) {
            // Should catch more specific exceptions than just "Exception" here, but there are
            // some which come from libcore.io.Libcore, which we don't have access to at compile time.
            Log.e(TAG, "Could not symlink " + source.getAbsolutePath() + " to " + dest.getAbsolutePath() + ": " + e.getMessage());
        }
    }

    @TargetApi(9)
    public static boolean setReadable(SanitizedFile file, boolean readable, boolean ownerOnly) {

        if (hasApi(9)) {
            return file.setReadable(readable, ownerOnly);
        }
        String mode;
        if (readable) {
            mode = ownerOnly ? "0600" : "0644";
        } else {
            mode = "0000";
        }
        return setMode(file, mode);

    }

    private static boolean setMode(SanitizedFile file, String mode) {

        // The "file" must be a sanitized file, and hence only contain A-Za-z0-9.-_ already,
        // but it makes no assurances about the parent directory.
        final String[] args = {
            "/system/bin/chmod",
            mode,
            file.getAbsolutePath()
        };

        try {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Executing following command: " + args[0] + " " + args[1] + " " + args[2]);
            }
            Process proc = Runtime.getRuntime().exec(args);
            Utils.consumeStream(proc.getInputStream());
            Utils.consumeStream(proc.getErrorStream());
            return true;
        } catch (IOException e) {
            return false;
        }

    }

    @TargetApi(9)
    public static boolean setExecutable(SanitizedFile file, boolean readable, boolean ownerOnly) {

        if (hasApi(9)) {
            return file.setExecutable(readable, ownerOnly);
        }
        String mode;
        if (readable) {
            mode = ownerOnly ? "0700" : "0711";
        } else {
            mode = ownerOnly ? "0600" : "0600";
        }
        return setMode(file, mode);

    }

}
