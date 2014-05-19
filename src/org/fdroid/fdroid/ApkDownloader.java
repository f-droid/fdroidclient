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

import android.os.Bundle;
import android.util.Log;

import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.net.AsyncDownloadWrapper;
import org.fdroid.fdroid.net.Downloader;
import org.fdroid.fdroid.net.HttpDownloader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;

public class ApkDownloader implements AsyncDownloadWrapper.Listener {

    private static final String TAG = "org.fdroid.fdroid.ApkDownloader";

    public static final String EVENT_APK_DOWNLOAD_COMPLETE = "apkDownloadComplete";
    public static final String EVENT_APK_DOWNLOAD_CANCELLED = "apkDownloadCancelled";
    public static final String EVENT_ERROR = "apkDownloadError";

    public static final int ERROR_HASH_MISMATCH = 101;
    public static final int ERROR_DOWNLOAD_FAILED = 102;
    public static final int ERROR_UNKNOWN = 103;

    /**
     * Used as a key to pass data through with an error event, explaining the type of event.
     */
    public static final String EVENT_DATA_ERROR_TYPE = "apkDownloadErrorType";

    private Apk curApk;
    private String repoAddress;
    private File localFile;

    private ProgressListener listener;
    private AsyncDownloadWrapper dlWrapper = null;
    private int progress  = 0;
    private int totalSize = 0;

    public void setProgressListener(ProgressListener listener) {
        this.listener = listener;
    }

    public void removeProgressListener() {
        setProgressListener(null);
    }

    ApkDownloader(Apk apk, String repoAddress, File destDir) {
        curApk = apk;
        this.repoAddress = repoAddress;
        localFile = new File(destDir, curApk.apkName);
    }

    // The downloaded APK. Valid only when getStatus() has returned STATUS.DONE.
    public File localFile() {
        return localFile;
    }

    public String getRemoteAddress() {
         return repoAddress + "/" + curApk.apkName.replace(" ", "%20");
    }

    private Hasher createHasher() {
        Hasher hasher;
        try {
            hasher = new Hasher(curApk.hashType, localFile);
        } catch (NoSuchAlgorithmException e) {
            Log.e("FDroid", "Error verifying hash of cached apk at " + localFile + ". " +
                    "I don't understand what the " + curApk.hashType + " hash algorithm is :(");
            hasher = null;
        }
        return hasher;
    }

    private boolean hashMatches() {
        if (!localFile.exists()) {
            return false;
        }
        Hasher hasher = createHasher();
        return hasher != null && hasher.match(curApk.hash);
    }

    /**
     * If an existing cached version exists, and matches the hash of the apk we
     * want to download, then we will return true. Otherwise, we return false
     * (and remove the cached file - if it exists and didn't match the correct hash).
     */
    private boolean verifyOrDeleteCachedVersion() {
        if (localFile.exists()) {
            if (hashMatches()) {
                Log.d("FDroid", "Using cached apk at " + localFile);
                return true;
            } else {
                Log.d("FDroid", "Not using cached apk at " + localFile);
                deleteLocalFile();
            }
        }
        return false;
    }

    private void deleteLocalFile() {
        if (localFile != null && localFile.exists()) {
            localFile.delete();
        }
    }

    public void download() {

        // Can we use the cached version?
        if (verifyOrDeleteCachedVersion()) {
            sendMessage(EVENT_APK_DOWNLOAD_COMPLETE);
            return;
        }

        String remoteAddress = getRemoteAddress();
        Log.d(TAG, "Downloading apk from " + remoteAddress);

        try {

            Downloader downloader = new HttpDownloader(remoteAddress, localFile);
            dlWrapper = new AsyncDownloadWrapper(downloader, this);
            dlWrapper.download();

        } catch (MalformedURLException e) {
            onErrorDownloading(e.getLocalizedMessage());
        } catch (IOException e) {
            onErrorDownloading(e.getLocalizedMessage());
        }
    }

    private void sendMessage(String type) {
        sendProgressEvent(new ProgressListener.Event(type));
    }

    private void sendError(int errorType) {
        Bundle data = new Bundle(1);
        data.putInt(EVENT_DATA_ERROR_TYPE, errorType);
        sendProgressEvent(new Event(EVENT_ERROR, data));
    }

    private void sendProgressEvent(Event event) {
        if (event.type.equals(Downloader.EVENT_PROGRESS)) {
            // Keep a copy of these ourselves, so people can interrogate us for the
            // info (in addition to receiving events with the info).
            totalSize = event.total;
            progress  = event.progress;
        }

        if (listener != null) {
            listener.onProgress(event);
        }
    }

    @Override
    public void onReceiveTotalDownloadSize(int size) {
        // Do nothing...
        // Rather, we will obtain the total download size from the progress events
        // when they start coming through.
    }

    @Override
    public void onReceiveCacheTag(String cacheTag) {
        // Do nothing...
    }

    @Override
    public void onErrorDownloading(String localisedExceptionDetails) {
        Log.e("FDroid", "Download failed: " + localisedExceptionDetails);
        sendError(ERROR_DOWNLOAD_FAILED);
        deleteLocalFile();
    }

    @Override
    public void onDownloadComplete() {

        if (!verifyOrDeleteCachedVersion()) {
            sendError(ERROR_HASH_MISMATCH);
            return;
        }

        Log.d("FDroid", "Download finished: " + localFile);
        sendMessage(EVENT_APK_DOWNLOAD_COMPLETE);
    }

    @Override
    public void onDownloadCancelled() {
        sendMessage(EVENT_APK_DOWNLOAD_CANCELLED);
    }

    @Override
    public void onProgress(Event event) {
        sendProgressEvent(event);
    }

    /**
     * Attempts to cancel the download (if in progress) and also removes the progress
     * listener (to prevent
     */
    public void cancel() {
        removeProgressListener();
        if (dlWrapper != null) {
            dlWrapper.attemptCancel();
        }
    }

    public Apk getApk() {
        return curApk;
    }

    public int getProgress() {
        return progress;
    }

    public int getTotalSize() {
        return totalSize;
    }
}