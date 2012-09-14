/*
 * Copyright (C) 2010-2012 Ciaran Gultnieks <ciaran@ciarang.com>
 * Copyright (C) 2011 Henrik Tunedal <tunedal@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
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

import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;

public class Downloader extends Thread {

    private DB.Apk curapk;
    private String filename;
    private File localfile;

    public static enum Status {
        STARTING, RUNNING, ERROR, DONE, CANCELLED
    };

    public static enum Error {
        CORRUPT, UNKNOWN
    };

    private Status status = Status.STARTING;
    private Error error;
    private int progress;
    private int max;
    private String errorMessage;

    Downloader(DB.Apk apk) {
        synchronized (this) {
            curapk = apk;
        }
    }

    public synchronized Status getStatus() {
        return status;
    }

    // Current progress and maximum value for progress dialog
    public synchronized int getProgress() {
        return progress;
    }

    public synchronized int getMax() {
        return max;
    }

    // Error code and error message, only valid if status is ERROR
    public synchronized Error getErrorType() {
        return error;
    }

    public synchronized String getErrorMessage() {
        return errorMessage;
    }

    // The URL being downloaded or path to a cached file
    public synchronized String remoteFile() {
        return filename;
    }

    // The downloaded APK. Valid only when getStatus() has returned STATUS.DONE.
    public File localFile() {
        return localfile;
    }

    // The APK being downloaded
    public synchronized DB.Apk getApk() {
        return curapk;
    }

    public void run() {

        String apkname = curapk.apkName;
        localfile = new File(DB.getDataPath(), apkname);
        try {

            // See if we already have this apk cached...
            if (localfile.exists()) {
                // We do - if its hash matches, we'll use it...
                Hasher hash = new Hasher(curapk.hashType, localfile);
                if (hash.match(curapk.hash)) {
                    Log.d("FDroid", "Using cached apk at " + localfile);
                    synchronized (this) {
                        progress = 1;
                        max = 1;
                        status = Status.DONE;
                        return;
                    }
                } else {
                    Log.d("FDroid", "Not using cached apk at " + localfile);
                    localfile.delete();
                }
            }

            // If we haven't got the apk locally, we'll have to download it...
            String remotefile;
            if (curapk.apkSource == null) {
                remotefile = curapk.server + "/" + apkname.replace(" ", "%20");
            } else {
                remotefile = curapk.apkSource;
            }
            Log.d("FDroid", "Downloading apk from " + remotefile);
            synchronized (this) {
                filename = remotefile;
                progress = 0;
                max = curapk.size;
                status = Status.RUNNING;
            }

            BufferedInputStream getit = new BufferedInputStream(new URL(
                    remotefile).openStream(), 8192);

            FileOutputStream saveit = new FileOutputStream(localfile);
            BufferedOutputStream bout = new BufferedOutputStream(saveit, 1024);
            byte data[] = new byte[1024];

            int totalRead = 0;
            int bytesRead = getit.read(data, 0, 1024);
            while (bytesRead != -1) {
                if (isInterrupted()) {
                    Log.d("FDroid", "Download cancelled!");
                    break;
                }
                bout.write(data, 0, bytesRead);
                totalRead += bytesRead;
                synchronized (this) {
                    progress = totalRead;
                }
                bytesRead = getit.read(data, 0, 1024);
            }
            bout.close();
            getit.close();
            saveit.close();

            if (isInterrupted()) {
                localfile.delete();
                synchronized (this) {
                    status = Status.CANCELLED;
                }
                return;
            }
            Hasher hash = new Hasher(curapk.hashType, localfile);
            if (!hash.match(curapk.hash)) {
                synchronized (this) {
                    Log.d("FDroid", "Downloaded file hash of " + hash.getHash()
                            + " did not match repo's " + curapk.hash);
                    // No point keeping a bad file, whether we're
                    // caching or not.
                    localfile.delete();
                    error = Error.CORRUPT;
                    errorMessage = null;
                    status = Status.ERROR;
                    return;
                }
            }
        } catch (Exception e) {
            Log.e("FDroid", "Download failed:\n" + Log.getStackTraceString(e));
            synchronized (this) {
                localfile.delete();
                error = Error.UNKNOWN;
                errorMessage = e.toString();
                status = Status.ERROR;
                return;
            }
        }

        Log.d("FDroid", "Download finished: " + localfile);
        synchronized (this) {
            status = Status.DONE;
        }
    }
}
