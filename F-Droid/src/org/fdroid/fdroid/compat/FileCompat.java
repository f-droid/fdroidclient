package org.fdroid.fdroid.compat;

import android.annotation.TargetApi;
import android.util.Log;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.SanitizedFile;

import java.io.IOException;

public class FileCompat {

    private static final String TAG = "org.fdroid.fdroid.compat.FileCompat";

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
