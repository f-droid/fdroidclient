/*
 * Copyright (C) 2010-2012 Ciaran Gultnieks <ciaran@ciarang.com>
 * Copyright (C) 2011 Henrik Tunedal <tunedal@gmail.com>
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

package org.fdroid.fdroid;

import java.io.*;

import android.util.Log;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.net.Downloader;
import org.fdroid.fdroid.net.HttpDownloader;

public class ApkDownloader extends Thread {
    private static final String TAG = "ApkDownloader";

    public static final int EVENT_APK_DOWNLOAD_COMPLETE = 100;
    public static final int EVENT_ERROR_HASH_MISMATCH = 101;
    public static final int EVENT_ERROR_DOWNLOAD_FAILED = 102;
    public static final int EVENT_ERROR_UNKNOWN = 103;
    private Apk curapk;
    private String repoaddress;
    private File destdir;
    private File localfile;

    private ProgressListener listener;

    public void setProgressListener(ProgressListener listener) {
        this.listener = listener;
    }

    // Constructor - creates a Downloader to download the given Apk,
    // which must have its detail populated.
    ApkDownloader(Apk apk, String repoaddress, File destdir) {
        curapk = apk;
        this.repoaddress = repoaddress;
        this.destdir = destdir;
    }

    // The downloaded APK. Valid only when getStatus() has returned STATUS.DONE.
    public File localFile() {
        return localfile;
    }

    public String remoteFile() {
         return repoaddress + "/" + curapk.apkName.replace(" ", "%20");
    }

    @Override
    public void run() {
        localfile = new File(destdir, curapk.apkName);

        try {

            // See if we already have this apk cached...
            if (localfile.exists()) {
                // We do - if its hash matches, we'll use it...
                Hasher hash = new Hasher(curapk.hashType, localfile);
                if (hash.match(curapk.hash)) {
                    Log.d("FDroid", "Using cached apk at " + localfile);
                    return;
                } else {
                    Log.d("FDroid", "Not using cached apk at " + localfile);
                    localfile.delete();
                }
            }

            // If we haven't got the apk locally, we'll have to download it...
            String remoteAddress = remoteFile();
            HttpDownloader downloader = new HttpDownloader(remoteAddress, localfile);

            if (listener != null) {
                downloader.setProgressListener(listener,
                        new ProgressListener.Event(Downloader.EVENT_PROGRESS, remoteAddress));
            }

            Log.d(TAG, "Downloading apk from " + remoteAddress);
            int httpStatus = downloader.downloadHttpFile();

            if (httpStatus != 200 || !localfile.exists()) {
                sendProgress(EVENT_ERROR_DOWNLOAD_FAILED);
                return;
            }

            Hasher hash = new Hasher(curapk.hashType, localfile);
            if (!hash.match(curapk.hash)) {
                Log.d("FDroid", "Downloaded file hash of " + hash.getHash()
                        + " did not match repo's " + curapk.hash);
                // No point keeping a bad file, whether we're
                // caching or not.
                localfile.delete();
                sendProgress(EVENT_ERROR_HASH_MISMATCH);
                return;
            }
        } catch (Exception e) {
            Log.e("FDroid", "Download failed:\n" + Log.getStackTraceString(e));
            if (localfile.exists()) {
                localfile.delete();
            }
            sendProgress(EVENT_ERROR_UNKNOWN);
            return;
        }

        Log.d("FDroid", "Download finished: " + localfile);
        sendProgress(EVENT_APK_DOWNLOAD_COMPLETE);
    }

    private void sendProgress(int type) {
        if (listener != null) {
            listener.onProgress(new ProgressListener.Event(type));
        }
    }

}
