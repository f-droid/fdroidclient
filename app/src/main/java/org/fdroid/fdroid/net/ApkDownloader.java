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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.fdroid.fdroid.Hasher;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.SanitizedFile;

import java.io.File;
import java.security.NoSuchAlgorithmException;

/**
 * Downloads and verifies (against the Apk.hash) the apk file.
 * If the file has previously been downloaded, it will make use of that
 * instead, without going to the network to download a new one.
 */
public class ApkDownloader {

    private static final String TAG = "ApkDownloader";

    public final String urlString;

    @NonNull private final Apk curApk;
    @NonNull private final Context context;
    @NonNull private SanitizedFile localFile;
    @NonNull private final SanitizedFile potentiallyCachedFile;
    private final LocalBroadcastManager localBroadcastManager;

    public ApkDownloader(@NonNull final Context context, @NonNull final App app, @NonNull final Apk apk, @NonNull final String repoAddress) {
        this.context = context;
        curApk = apk;
        potentiallyCachedFile = new SanitizedFile(Utils.getApkCacheDir(context), apk.apkName);
        urlString = Utils.getApkUrl(repoAddress, apk);
        localBroadcastManager = LocalBroadcastManager.getInstance(context);
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
                Utils.debugLog(TAG, "Using cached apk at " + apkFile);
                return true;
            }
            Utils.debugLog(TAG, "Not using cached apk at " + apkFile + "(hash doesn't match, will delete file)");
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

    private void sendDownloadComplete() {
        Utils.debugLog(TAG, "Download finished: " + localFile);
        localBroadcastManager.unregisterReceiver(downloadCompleteReceiver);
    }

    public void download() {
        // Can we use the cached version?
        if (verifyOrDelete(potentiallyCachedFile)) {
            delete(localFile);
            Utils.copyQuietly(potentiallyCachedFile, localFile);
            sendDownloadComplete();
            return;
        }

        Utils.debugLog(TAG, "Downloading apk from " + urlString + " to " + localFile);
        localBroadcastManager.registerReceiver(downloadCompleteReceiver,
                DownloaderService.getIntentFilter(urlString, Downloader.ACTION_COMPLETE));

        DownloaderService.queue(context, urlString);
    }

    private void sendProgressEvent(String status) {
        Intent intent = new Intent(status);
        intent.setData(Uri.parse(urlString));
        intent.putExtra(Downloader.EXTRA_DOWNLOAD_PATH, localFile.getAbsolutePath());
        localBroadcastManager.sendBroadcast(intent);
    }

    // TODO move this code to somewhere more appropriate
    BroadcastReceiver downloadCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            localFile = SanitizedFile.knownSanitized(intent.getStringExtra(Downloader.EXTRA_DOWNLOAD_PATH));
            if (!verifyOrDelete(localFile)) {
                sendProgressEvent(Downloader.ACTION_INTERRUPTED);
                Toast.makeText(context, R.string.corrupt_download, Toast.LENGTH_LONG).show();
                return;
            }

            if (Preferences.get().shouldCacheApks()) {
                Utils.debugLog(TAG, "Copying .apk file to cache at " + potentiallyCachedFile.getAbsolutePath());
                Utils.copyQuietly(localFile, potentiallyCachedFile);
            }

            sendDownloadComplete();
        }
    };
}
