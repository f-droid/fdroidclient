/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (C) 2016 Hans-Christoph Steiner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fdroid.fdroid.net;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.PatternMatcher;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.fdroid.database.Repository;
import org.fdroid.download.Downloader;
import org.fdroid.download.NotFoundException;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.SanitizedFile;
import org.fdroid.fdroid.installer.ApkCache;
import org.fdroid.fdroid.installer.InstallManagerService;
import org.fdroid.fdroid.installer.Installer;
import org.fdroid.index.v2.FileV1;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpRetryException;
import java.net.NoRouteToHostException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLKeyException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLProtocolException;

/**
 * DownloaderService is a service that handles asynchronous download requests
 * (expressed as {@link Intent}s) on demand.  Clients send download requests
 * through {@link #queue(Context, long, String, String, FileV1)}  calls.  The
 * service is started as needed, it handles each {@code Intent} using a worker
 * thread, and stops itself when it runs out of work.  Requests can be canceled
 * using {@link #cancel(Context, String)}.  Bad requests will be ignored,
 * including on restart after killing via {@link Service#START_NOT_STICKY}.
 * <p>
 * This "work queue processor" pattern is commonly used to offload tasks
 * from an application's main thread.  The DownloaderService class exists to
 * simplify this pattern and take care of the mechanics. DownloaderService
 * will receive the Intents, use a worker thread, and stop the service as
 * appropriate.
 * <p>
 * All requests are handled on a single worker thread -- they may take as
 * long as necessary (and will not block the application's main loop), but
 * only one request will be processed at a time.
 * <p>
 * The Canonical URL for the file to download is also used as the unique ID to
 * represent the download itself throughout F-Droid.  This follows the model
 * of {@link Intent#setData(Uri)}, where the core data of an {@code Intent} is
 * a {@code Uri}.  For places that need an {@code int} ID,
 * {@link String#hashCode()} should be used to get a reproducible, unique {@code int}
 * from any {@code canonicalUrl}.  That full URL is guaranteed to be unique since
 * it points to a file on a filesystem.  This is more important with media files
 * than with APKs since there is not reliable standard for a unique ID for
 * media files, unlike APKs with {@code packageName} and {@code versionCode}.
 *
 * @see androidx.core.app.JobIntentService
 * @see org.fdroid.fdroid.installer.InstallManagerService
 */
public class DownloaderService extends JobIntentService {
    private static final String TAG = "DownloaderService";
    private static final int JOB_ID = TAG.hashCode();

    private static final String ACTION_QUEUE = "org.fdroid.fdroid.net.DownloaderService.action.QUEUE";

    public static final String ACTION_STARTED = "org.fdroid.fdroid.net.Downloader.action.STARTED";
    public static final String ACTION_PROGRESS = "org.fdroid.fdroid.net.Downloader.action.PROGRESS";
    public static final String ACTION_INTERRUPTED = "org.fdroid.fdroid.net.Downloader.action.INTERRUPTED";
    public static final String ACTION_CONNECTION_FAILED = "org.fdroid.fdroid.net.Downloader.action.CONNECTION_FAILED";
    public static final String ACTION_COMPLETE = "org.fdroid.fdroid.net.Downloader.action.COMPLETE";

    public static final String EXTRA_DOWNLOAD_PATH = "org.fdroid.fdroid.net.Downloader.extra.DOWNLOAD_PATH";
    public static final String EXTRA_BYTES_READ = "org.fdroid.fdroid.net.Downloader.extra.BYTES_READ";
    public static final String EXTRA_TOTAL_BYTES = "org.fdroid.fdroid.net.Downloader.extra.TOTAL_BYTES";
    public static final String EXTRA_ERROR_MESSAGE = "org.fdroid.fdroid.net.Downloader.extra.ERROR_MESSAGE";
    private static final String EXTRA_REPO_ID = "org.fdroid.fdroid.net.Downloader.extra.REPO_ID";
    private static final String EXTRA_MIRROR_URL = "org.fdroid.fdroid.net.Downloader.extra.MIRROR_URL";
    /**
     * Unique ID used to represent this specific package's install process,
     * including {@link android.app.Notification}s, also known as {@code canonicalUrl}.
     * Careful about types, this should always be a {@link String}, so it can
     * be handled on the receiving side by {@link android.content.Intent#getStringArrayExtra(String)}.
     *
     * @see org.fdroid.fdroid.installer.InstallManagerService
     * @see android.content.Intent#EXTRA_ORIGINATING_URI
     */
    public static final String EXTRA_CANONICAL_URL = "org.fdroid.fdroid.net.Downloader.extra.CANONICAL_URL";

    private static volatile Downloader downloader;
    private static volatile String activeCanonicalUrl;
    private InstallManagerService installManagerService;
    private LocalBroadcastManager localBroadcastManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Utils.debugLog(TAG, "Creating downloader service.");
        installManagerService = InstallManagerService.getInstance(this);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Utils.debugLog(TAG, "Received Intent for downloading: " + intent);

        String canonicalUrl = intent.getDataString();
        if (canonicalUrl == null) {
            Utils.debugLog(TAG, "Received Intent with no URI: " + intent);
            return;
        }
        if (ACTION_QUEUE.equals(intent.getAction())) {
            handleIntent(intent);
        } else {
            Utils.debugLog(TAG, "Received Intent with unknown action: " + intent);
        }
    }

    @Override
    public void onDestroy() {
        Utils.debugLog(TAG, "Destroying downloader service.");
    }

    /**
     * This method is invoked on the worker thread with a request to process.
     * Only one Intent is processed at a time, but the processing happens on a
     * worker thread that runs independently from other application logic.
     * So, if this code takes a long time, it will hold up other requests to
     * the same DownloaderService, but it will not hold up anything else.
     * When all requests have been handled, the DownloaderService stops itself,
     * so you should not ever call {@link #stopSelf}.
     * <p>
     * Downloads are put into subdirectories based on hostname/port of each repo
     * to prevent files with the same names from conflicting.  Each repo enforces
     * unique APK file names on the server side.
     * <p>
     * Swap repos are not maintained in the database.  If this is handling a
     * download from a swap repo, it will detect that is so by looking at the
     * URL.  {@code http://} URLs are only allowed for swap, and swap repos
     * will never be on a System Port, only on a User Port. And swap repos use a
     * hardcoded path.
     *
     * @param intent The {@link Intent} passed via {@link
     *               android.content.Context#startService(Intent)}.
     */
    private void handleIntent(Intent intent) {
        final Uri canonicalUrl = intent.getData();
        final App app = intent.getParcelableExtra(Installer.EXTRA_APP);
        final Apk apk = intent.getParcelableExtra(Installer.EXTRA_APK);
        final long repoId = intent.getLongExtra(DownloaderService.EXTRA_REPO_ID, apk.repoId);
        final String extraUrl = intent.getStringExtra(DownloaderService.EXTRA_CANONICAL_URL);
        final Uri downloadUrl = Uri.parse(extraUrl == null ? apk.getDownloadUrl() : extraUrl);
        final FileV1 fileV1 = apk.apkFile;
        final SanitizedFile localFile = ApkCache.getApkDownloadPath(this, canonicalUrl);

        Utils.debugLog(TAG, "Queued download of " + canonicalUrl.hashCode() + "/" + canonicalUrl
                + " using " + downloadUrl);

        sendBroadcast(canonicalUrl, DownloaderService.ACTION_STARTED, localFile, repoId, canonicalUrl);
        installManagerService.onDownloadStarted(canonicalUrl);

        try {
            activeCanonicalUrl = canonicalUrl.toString();
            Context context = getApplicationContext();
            Repository repo = FDroidApp.getRepoManager(context).getRepository(repoId);
            if (repo == null) {
                String path = canonicalUrl.getPath();
                if (canonicalUrl.getPort() > 1023
                        && "http".equals(canonicalUrl.getScheme())
                        && path != null && path.startsWith("/fdroid/repo")) {
                    String url = canonicalUrl.buildUpon().path("/fdroid/repo").build().toString();
                    repo = FDroidApp.createSwapRepo(url, null);
                } else return; // repo might have been deleted in the meantime
            }
            downloader = DownloaderFactory.INSTANCE.create(repo, downloadUrl, fileV1, localFile);
            final long[] lastProgressSent = {0};
            downloader.setListener((bytesRead, totalBytes) -> {
                // don't send a progress updates out to frequently, to not hit notification rate-limiting
                // this can cause us to miss critical notification updates
                long now = System.currentTimeMillis();
                if (now - lastProgressSent[0] < 1_000) return;
                lastProgressSent[0] = now;
                Intent intent1 = new Intent(DownloaderService.ACTION_PROGRESS);
                intent1.setData(canonicalUrl);
                intent1.putExtra(DownloaderService.EXTRA_BYTES_READ, bytesRead);
                intent1.putExtra(DownloaderService.EXTRA_TOTAL_BYTES, totalBytes);
                localBroadcastManager.sendBroadcast(intent1);
                installManagerService.onDownloadProgress(canonicalUrl, app, apk, bytesRead, totalBytes);
            });
            downloader.download();
            sendBroadcast(canonicalUrl, DownloaderService.ACTION_COMPLETE, localFile, repoId, canonicalUrl);
            installManagerService.onDownloadComplete(canonicalUrl, localFile, app, apk);
        } catch (InterruptedException e) {
            sendBroadcast(canonicalUrl, DownloaderService.ACTION_INTERRUPTED, localFile, repoId, canonicalUrl);
            installManagerService.onDownloadFailed(canonicalUrl, null);
        } catch (ConnectException | HttpRetryException | NoRouteToHostException |
                 SocketTimeoutException
                 | SSLHandshakeException | SSLKeyException | SSLPeerUnverifiedException |
                 SSLProtocolException
                 | ProtocolException | UnknownHostException | NotFoundException e) {
            // if the above list of exceptions changes, also change it in IndexV1Updater.update()
            Log.e(TAG, "CONNECTION_FAILED: " + e.getLocalizedMessage());
            sendBroadcast(canonicalUrl, DownloaderService.ACTION_CONNECTION_FAILED, localFile, repoId, canonicalUrl);
            installManagerService.onDownloadFailed(canonicalUrl, e.getLocalizedMessage());
        } catch (IOException e) {
            Log.e(TAG, "Error downloading: ", e);
            sendBroadcast(canonicalUrl, DownloaderService.ACTION_INTERRUPTED, localFile,
                    e.getLocalizedMessage(), repoId, canonicalUrl);
            installManagerService.onDownloadFailed(canonicalUrl, e.getLocalizedMessage());
        } finally {
            if (downloader != null) {
                downloader.close();
            }
        }
        downloader = null;
        activeCanonicalUrl = null;
    }

    private void sendBroadcast(Uri uri, String action, File file, long repoId, Uri canonicalUrl) {
        sendBroadcast(uri, action, file, null, repoId, canonicalUrl);
    }

    private void sendBroadcast(Uri uri, String action, File file, String errorMessage, long repoId,
                               Uri canonicalUrl) {
        Intent intent = new Intent(action);
        if (canonicalUrl != null) {
            intent.setData(canonicalUrl);
        }
        if (file != null) {
            intent.putExtra(DownloaderService.EXTRA_DOWNLOAD_PATH, file.getAbsolutePath());
        }
        if (!TextUtils.isEmpty(errorMessage)) {
            intent.putExtra(DownloaderService.EXTRA_ERROR_MESSAGE, errorMessage);
        }
        intent.putExtra(DownloaderService.EXTRA_REPO_ID, repoId);
        intent.putExtra(DownloaderService.EXTRA_MIRROR_URL, uri.toString());
        localBroadcastManager.sendBroadcast(intent);
    }

    /**
     * Add a URL to the download queue.
     * <p>
     * All notifications are sent as an {@link Intent} via local broadcasts to be received by
     *
     * @param context      this app's {@link Context}
     * @param repoId       the database ID number representing one repo
     * @param canonicalUrl the URL used as the unique ID throughout F-Droid
     * @see #cancel(String)
     */
    public static void queue(Context context, long repoId, String canonicalUrl,
                             String downloadUrl) {
        if (TextUtils.isEmpty(canonicalUrl)) {
            return;
        }
        Utils.debugLog(TAG, "Queue download " + canonicalUrl.hashCode() + "/" + canonicalUrl);
        Intent intent = new Intent(context, DownloaderService.class);
        intent.setAction(ACTION_QUEUE);
        intent.setData(Uri.parse(canonicalUrl));
        intent.putExtra(DownloaderService.EXTRA_REPO_ID, repoId);
        intent.putExtra(DownloaderService.EXTRA_CANONICAL_URL, downloadUrl);
        JobIntentService.enqueueWork(context, DownloaderService.class, JOB_ID, intent);
    }

    public static void queue(Context context, String canonicalUrl, @NonNull App app, @NonNull Apk apk) {
        if (TextUtils.isEmpty(canonicalUrl) || apk.apkFile == null) {
            return;
        }
        Utils.debugLog(TAG, "Queue download " + canonicalUrl.hashCode() + "/" + canonicalUrl);
        Intent intent = new Intent(context, DownloaderService.class);
        intent.setAction(ACTION_QUEUE);
        intent.setData(Uri.parse(canonicalUrl));
        intent.putExtra(Installer.EXTRA_APP, app);
        intent.putExtra(Installer.EXTRA_APK, apk);
        JobIntentService.enqueueWork(context, DownloaderService.class, JOB_ID, intent);
    }

    /**
     * Remove a URL to the download queue, even if it is currently downloading.
     * <p>
     * All notifications are sent as an {@link Intent} via local broadcasts to be received by
     *
     * @param canonicalUrl The URL to remove from the download queue
     * @see #queue(Context, String, App, Apk)
     */
    public static void cancel(String canonicalUrl) {
        if (TextUtils.isEmpty(canonicalUrl)) {
            return;
        }
        Utils.debugLog(TAG, "Cancelling download of " + canonicalUrl.hashCode() + "/" + canonicalUrl
                + " downloading from " + canonicalUrl);
        int whatToRemove = canonicalUrl.hashCode();
        if (isActive(canonicalUrl)) {
            downloader.cancelDownload();
        } else {
            Utils.debugLog(TAG, "ACTION_CANCEL called on something not queued or running"
                    + " (expected to find message with ID of " + whatToRemove + " in queue).");
        }
    }

    /**
     * Check if a URL is actively being downloaded.
     */
    private static boolean isActive(String downloadUrl) {
        return downloader != null && TextUtils.equals(downloadUrl, activeCanonicalUrl);
    }

    /**
     * Get a prepared {@link IntentFilter} for use for matching this service's action events.
     *
     * @param canonicalUrl the URL used as the unique ID for the specific package
     */
    public static IntentFilter getIntentFilter(String canonicalUrl) {
        Uri uri = Uri.parse(canonicalUrl);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DownloaderService.ACTION_STARTED);
        intentFilter.addAction(DownloaderService.ACTION_PROGRESS);
        intentFilter.addAction(DownloaderService.ACTION_COMPLETE);
        intentFilter.addAction(DownloaderService.ACTION_INTERRUPTED);
        intentFilter.addAction(DownloaderService.ACTION_CONNECTION_FAILED);
        intentFilter.addDataScheme(uri.getScheme());
        intentFilter.addDataAuthority(uri.getHost(), String.valueOf(uri.getPort()));
        intentFilter.addDataPath(uri.getPath(), PatternMatcher.PATTERN_LITERAL);
        return intentFilter;
    }
}
