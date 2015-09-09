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

package org.fdroid.fdroid.net;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.fdroid.fdroid.Hasher;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.ProgressListener;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.compat.FileCompat;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.SanitizedFile;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Downloads and verifies (against the Apk.hash) the apk file.
 * If the file has previously been downloaded, it will make use of that
 * instead, without going to the network to download a new one.
 */
public class ApkDownloader implements AsyncDownloader.Listener {

    private static final String TAG = "ApkDownloader";

    public static final String EVENT_APK_DOWNLOAD_COMPLETE = "apkDownloadComplete";
    public static final String EVENT_APK_DOWNLOAD_CANCELLED = "apkDownloadCancelled";
    public static final String EVENT_ERROR = "apkDownloadError";

    public static final String ACTION_STATUS = "apkDownloadStatus";
    public static final String EXTRA_TYPE = "apkDownloadStatusType";
    public static final String EXTRA_URL = "apkDownloadUrl";

    public static final int ERROR_HASH_MISMATCH = 101;
    public static final int ERROR_DOWNLOAD_FAILED = 102;

    private static final String EVENT_SOURCE_ID = "sourceId";
    private static long downloadIdCounter = 0;

    /**
     * Used as a key to pass data through with an error event, explaining the type of event.
     */
    public static final String EVENT_DATA_ERROR_TYPE = "apkDownloadErrorType";

    @NonNull private final App app;
    @NonNull private final Apk curApk;
    @NonNull private final Context context;
    @NonNull private final String repoAddress;
    @NonNull private final SanitizedFile localFile;
    @NonNull private final SanitizedFile potentiallyCachedFile;

    private ProgressListener listener;
    private AsyncDownloader dlWrapper = null;
    private boolean isComplete = false;

    private final long id = ++downloadIdCounter;

    public void setProgressListener(ProgressListener listener) {
        this.listener = listener;
    }

    public void removeProgressListener() {
        setProgressListener(null);
    }

    public ApkDownloader(@NonNull final Context context, @NonNull final App app, @NonNull final Apk apk, @NonNull final String repoAddress) {
        this.context = context;
        this.app = app;
        curApk = apk;
        this.repoAddress = repoAddress;
        localFile = new SanitizedFile(Utils.getApkDownloadDir(context), apk.apkName);
        potentiallyCachedFile = new SanitizedFile(Utils.getApkCacheDir(context), apk.apkName);
    }

    /**
     * The downloaded APK. Valid only when getStatus() has returned STATUS.DONE.
     */
    public SanitizedFile localFile() {
        return localFile;
    }

    /**
     * When stopping/starting downloaders multiple times (on different threads), it can
     * get weird whereby different threads are sending progress events. It is important
     * to be able to see which downloader these progress events are coming from.
     */
    public boolean isEventFromThis(Event event) {
        return event.getData().containsKey(EVENT_SOURCE_ID) && event.getData().getLong(EVENT_SOURCE_ID) == id;
    }

    private Hasher createHasher(File apkFile) {
        Hasher hasher;
        try {
            hasher = new Hasher(curApk.hashType, apkFile);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error verifying hash of cached apk at " + apkFile + ". " +
                    "I don't understand what the " + curApk.hashType + " hash algorithm is :(");
            hasher = null;
        }
        return hasher;
    }

    private boolean hashMatches(@NonNull final File apkFile) {
        if (!apkFile.exists()) {
            return false;
        }
        Hasher hasher = createHasher(apkFile);
        return hasher != null && hasher.match(curApk.hash);
    }

    /**
     * If an existing cached version exists, and matches the hash of the apk we
     * want to download, then we will return true. Otherwise, we return false
     * (and remove the cached file - if it exists and didn't match the correct hash).
     */
    private boolean verifyOrDelete(@NonNull final File apkFile) {
        if (apkFile.exists()) {
            if (hashMatches(apkFile)) {
                Utils.DebugLog(TAG, "Using cached apk at " + apkFile);
                return true;
            }
            Utils.DebugLog(TAG, "Not using cached apk at " + apkFile + "(hash doesn't match, will delete file)");
            delete(apkFile);
        }
        return false;
    }

    private void delete(@NonNull final File file) {
        if (file.exists()) {
            if (!file.delete()) {
                Log.w(TAG, "Could not delete file " + file);
            }
        }
    }

    private void prepareApkFileAndSendCompleteMessage() {

        // Need the apk to be world readable, so that the installer is able to read it.
        // Note that saving it into external storage for the purpose of letting the installer
        // have access is insecure, because apps with permission to write to the external
        // storage can overwrite the app between F-Droid asking for it to be installed and
        // the installer actually installing it.
        FileCompat.setReadable(localFile, true, false);

        isComplete = true;
        sendMessage(EVENT_APK_DOWNLOAD_COMPLETE);
    }

    public boolean isComplete() {
        return this.isComplete;
    }

    /**
     * If the download successfully spins up a new thread to start downloading, then we return
     * true, otherwise false. This is useful, e.g. when we use a cached version, and so don't
     * want to bother with progress dialogs et al.
     */
    public boolean download() {

        // Can we use the cached version?
        if (verifyOrDelete(potentiallyCachedFile)) {
            delete(localFile);
            Utils.copyQuietly(potentiallyCachedFile, localFile);
            prepareApkFileAndSendCompleteMessage();
            return false;
        }

        String remoteAddress = Utils.getApkUrl(repoAddress, curApk);
        Utils.DebugLog(TAG, "Downloading apk from " + remoteAddress + " to " + localFile);

        try {
            dlWrapper = DownloaderFactory.createAsync(context, remoteAddress, localFile, app.name + " " + curApk.version, curApk.id, this);
            dlWrapper.download();
            return true;
        } catch (IOException e) {
            onErrorDownloading(e.getLocalizedMessage());
        }

        return false;
    }

    private void sendMessage(String type) {
        sendProgressEvent(new ProgressListener.Event(type));
    }

    private void sendError(int errorType) {
        Bundle data = new Bundle(1);
        data.putInt(EVENT_DATA_ERROR_TYPE, errorType);
        sendProgressEvent(new Event(EVENT_ERROR, data));
    }

    // TODO: Completely remove progress listener, only use broadcasts...
    private void sendProgressEvent(Event event) {

        event.getData().putLong(EVENT_SOURCE_ID, id);

        if (listener != null) {
            listener.onProgress(event);
        }

        Intent intent = new Intent(ACTION_STATUS);
        intent.putExtras(event.getData());
        intent.putExtra(EXTRA_TYPE, event.type);
        intent.putExtra(EXTRA_URL, Utils.getApkUrl(repoAddress, curApk));
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    public void onErrorDownloading(String localisedExceptionDetails) {
        Log.e(TAG, "Download failed: " + localisedExceptionDetails);
        sendError(ERROR_DOWNLOAD_FAILED);
        delete(localFile);
    }

    private void cacheIfRequired() {
        if (Preferences.get().shouldCacheApks()) {
            Utils.DebugLog(TAG, "Copying .apk file to cache at " + potentiallyCachedFile.getAbsolutePath());
            Utils.copyQuietly(localFile, potentiallyCachedFile);
        }
    }

    @Override
    public void onDownloadComplete() {

        if (!verifyOrDelete(localFile)) {
            sendError(ERROR_HASH_MISMATCH);
            return;
        }

        cacheIfRequired();

        Utils.DebugLog(TAG, "Download finished: " + localFile);
        prepareApkFileAndSendCompleteMessage();
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
     * listener
     *
     * @param userRequested - true if the user requested the cancel (via button click), otherwise false.
     */
    public void cancel(boolean userRequested) {
        if (dlWrapper != null) {
            dlWrapper.attemptCancel(userRequested);
        }
    }

    public Apk getApk() { return curApk; }

    public int getBytesRead() { return dlWrapper != null ? dlWrapper.getBytesRead() : 0; }

    public int getTotalBytes() { return dlWrapper != null ? dlWrapper.getTotalBytes() : 0; }
}
