/*
 * Copyright (C) 2018 Hans-Christoph Steiner <hans@eds.org>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.fdroid.fdroid.nearby;

import android.Manifest;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Process;
import android.util.Log;

import androidx.core.content.ContextCompat;

import org.fdroid.fdroid.Utils;
import org.fdroid.index.SigningException;
import org.fdroid.index.v1.IndexV1UpdaterKt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * An {@link IntentService} subclass for scanning removable "external storage"
 * for F-Droid package repos, e.g. SD Cards. This is intended to support
 * sharable package repos, so it ignores non-removable storage, like the fake
 * emulated sdcard from devices with only built-in storage.  This method will
 * only ever allow for reading repos, never writing.  It also will not work
 * for removable storage devices plugged in via USB, since do not show up as
 * "External Storage"
 * <p>
 * Scanning the removable storage requires that the user allowed it.  This
 * requires both the {@link org.fdroid.fdroid.Preferences#isScanRemovableStorageEnabled()}
 * and the {@link android.Manifest.permission#READ_EXTERNAL_STORAGE}
 * permission to be enabled.
 *
 * @see TreeUriScannerIntentService TreeUri method for writing repos to be shared
 * @see <a href="https://stackoverflow.com/a/40201333">Universal way to write to external SD card on Android</a>
 * @see <a href="https://commonsware.com/blog/2017/11/14/storage-situation-external-storage.html"> The Storage Situation: External Storage </a>
 */
public class SDCardScannerService extends IntentService {
    public static final String TAG = "SDCardScannerService";

    private static final String ACTION_SCAN = "org.fdroid.fdroid.nearby.SCAN";

    private static final List<String> SKIP_DIRS = Arrays.asList(".android_secure", "LOST.DIR");

    public SDCardScannerService() {
        super("SDCardScannerService");
    }

    public static void scan(Context context) {
        Intent intent = new Intent(context, SDCardScannerService.class);
        intent.setAction(ACTION_SCAN);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null || !ACTION_SCAN.equals(intent.getAction())) {
            return;
        }
        Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

        HashSet<File> files = new HashSet<>();
        for (File f : getExternalFilesDirs(null)) {
            Log.i(TAG, "getExternalFilesDirs " + f);
            if (f == null || !f.isDirectory()) {
                continue;
            }
            Log.i(TAG, "getExternalFilesDirs " + f);
            if (Environment.isExternalStorageRemovable(f)) {
                String state = Environment.getExternalStorageState(f);
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    // remove Android/data/org.fdroid.fdroid/files to get root
                    File sdcard = f.getParentFile().getParentFile().getParentFile().getParentFile();
                    Collections.addAll(files, checkExternalStorage(sdcard, state));
                } else {
                    Collections.addAll(files, checkExternalStorage(f, state));
                }
            }
        }

        Log.i(TAG, "sdcard files " + files.toString());
        ArrayList<String> filesList = new ArrayList<>();
        for (File dir : files) {
            if (!dir.isDirectory()) {
                continue;
            }
            searchDirectory(dir);
        }
    }

    private File[] checkExternalStorage(File sdcard, String state) {
        File[] files = null;
        if (sdcard != null &&
                (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state) || Environment.MEDIA_MOUNTED.equals(state))) {
            files = sdcard.listFiles();
        }

        if (files == null) {
            Utils.debugLog(TAG, "checkExternalStorage returned blank, F-Droid probably doesn't have Storage perm!");
            return new File[0];
        } else {
            return files;
        }
    }

    private void searchDirectory(File dir) {
        if (SKIP_DIRS.contains(dir.getName())) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                searchDirectory(file);
            } else {
                if (IndexV1UpdaterKt.SIGNED_FILE_NAME.equals(file.getName())) {
                    registerRepo(file);
                }
            }
        }
    }

    private void registerRepo(File file) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            TreeUriScannerIntentService.registerRepo(this, inputStream, Uri.fromFile(file.getParentFile()));
        } catch (IOException | SigningException e) {
            e.printStackTrace();
        } finally {
            Utils.closeQuietly(inputStream);
        }
    }

}