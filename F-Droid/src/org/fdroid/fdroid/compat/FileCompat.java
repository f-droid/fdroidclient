package org.fdroid.fdroid.compat;

import android.annotation.TargetApi;
import android.os.Build;
import android.system.ErrnoException;
import android.util.Log;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.SanitizedFile;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FileCompat {

    private static final String TAG = "org.fdroid.fdroid.compat.FileCompat";

    public static boolean symlink(SanitizedFile source, SanitizedFile dest) {

        if (Compatibility.hasApi(21)) {
            symlinkOs(source, dest);
        } else if (Compatibility.hasApi(19)) {
            symlinkLibcore(source, dest);
        } else {
            symlinkRuntime(source, dest);
        }

        return dest.exists();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected static void symlinkOs(SanitizedFile source, SanitizedFile dest) {
        try {
            android.system.Os.symlink(source.getAbsolutePath(), dest.getAbsolutePath());
        } catch (ErrnoException e) {
            // Do nothing...
        }
    }

    protected static void symlinkRuntime(SanitizedFile source, SanitizedFile dest) {
        String commands[] = {
            "/system/bin/ln",
            source.getAbsolutePath(),
            dest.getAbsolutePath()
        };
        try {
            Log.d(TAG, "Executing command: " + commands[0] + " " + commands[1] + " " + commands[2]);
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
        } catch (InvocationTargetException | NoSuchMethodException | ClassNotFoundException | IllegalAccessException | NoSuchFieldException e) {
            // Do nothing
        }
    }

    @TargetApi(9)
    public static boolean setReadable(SanitizedFile file, boolean readable, boolean ownerOnly) {

        if (Compatibility.hasApi(9)) {
            return file.setReadable(readable, ownerOnly);
        } else {
            String mode;
            if (readable) {
                mode = ownerOnly ? "0600" : "0644";
            } else {
                mode = "0000";
            }
            return setMode(file, mode);
        }

    }

    private static boolean setMode(SanitizedFile file, String mode) {

        // The "file" must be a sanitized file, and hence only contain A-Za-z0-9.-_ already,
        // but it makes no assurances about the parent directory.
        String[] args = {
            "/system/bin/chmod",
            mode,
            file.getAbsolutePath()
        };

        try {
            Log.d(TAG, "Executing following command: " + args[0] + " " + args[1] + " " + args[2]);
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

        if (Compatibility.hasApi(9)) {
            return file.setExecutable(readable, ownerOnly);
        } else {
            String mode;
            if ( readable ) {
                mode = ownerOnly ? "0700" : "0711";
            } else {
                mode = ownerOnly ? "0600" : "0600";
            }
            return setMode( file, mode );
        }

    }

}
