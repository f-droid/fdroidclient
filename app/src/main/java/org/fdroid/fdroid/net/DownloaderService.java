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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PatternMatcher;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import org.fdroid.fdroid.AppDetails;
import org.fdroid.fdroid.FDroid;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.SanitizedFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * DownloaderService is a service that handles asynchronous download requests
 * (expressed as {@link Intent}s) on demand.  Clients send download requests
 * through {@link android.content.Context#startService(Intent)} calls; the
 * service is started as needed, handles each Intent in turn using a worker
 * thread, and stops itself when it runs out of work.
 * <p>
 * This "work queue processor" pattern is commonly used to offload tasks
 * from an application's main thread.  The DownloaderService class exists to
 * simplify this pattern and take care of the mechanics. DownloaderService
 * will receive the Intents, launch a worker thread, and stop the service as
 * appropriate.
 * <p>
 * All requests are handled on a single worker thread -- they may take as
 * long as necessary (and will not block the application's main loop), but
 * only one request will be processed at a time.
 * <p>
 * The full URL for the file to download is also used as the unique ID to
 * represent the download itself throughout F-Droid.  This follows the model
 * of {@link Intent#setData(Uri)}, where the core data of an {@code Intent} is
 * a {@code Uri}.  For places that need an {@code int} ID,
 * {@link String#hashCode()} should be used to get a reproducible, unique {@code int}
 * from any {@code urlString}.  The full URL is guaranteed to be unique since
 * it points to a file on a filesystem.  This is more important with media files
 * than with APKs since there is not reliable standard for a unique ID for
 * media files, unlike APKs with {@code packageName} and {@code versionCode}.
 *
 * @see android.app.IntentService
 */
public class DownloaderService extends Service {
    private static final String TAG = "DownloaderService";

    private static final String EXTRA_PACKAGE_NAME = "org.fdroid.fdroid.net.DownloaderService.extra.PACKAGE_NAME";

    private static final String ACTION_QUEUE = "org.fdroid.fdroid.net.DownloaderService.action.QUEUE";
    private static final String ACTION_CANCEL = "org.fdroid.fdroid.net.DownloaderService.action.CANCEL";

    private static final int NOTIFY_DOWNLOADING = 0x2344;

    private volatile Looper serviceLooper;
    private static volatile ServiceHandler serviceHandler;
    private static volatile Downloader downloader;
    private LocalBroadcastManager localBroadcastManager;

    private final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            handleIntent((Intent) msg.obj);
            stopSelf(msg.arg1);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Utils.debugLog(TAG, "Creating downloader service.");

        HandlerThread thread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Utils.debugLog(TAG, "Received Intent for downloading: " + intent + " (with a startId of " + startId + ")");
        String uriString = intent.getDataString();
        if (uriString == null) {
            Log.e(TAG, "Received Intent with no URI: " + intent);
            return;
        }
        if (ACTION_CANCEL.equals(intent.getAction())) {
            Utils.debugLog(TAG, "Cancelling download of " + uriString);
            Integer whatToRemove = uriString.hashCode();
            if (serviceHandler.hasMessages(whatToRemove)) {
                serviceHandler.removeMessages(whatToRemove);
            } else if (isActive(uriString)) {
                downloader.cancelDownload();
            } else {
                Log.e(TAG, "ACTION_CANCEL called on something not queued or running");
            }
        } else if (ACTION_QUEUE.equals(intent.getAction())) {
            Message msg = serviceHandler.obtainMessage();
            msg.arg1 = startId;
            msg.obj = intent;
            msg.what = uriString.hashCode();
            serviceHandler.sendMessage(msg);
            Utils.debugLog(TAG, "Queued download of " + uriString);
        } else {
            Log.e(TAG, "Received Intent with unknown action: " + intent);
        }
    }

    private NotificationCompat.Builder createNotification(String urlString, @Nullable String packageName) {
        return new NotificationCompat.Builder(this)
                .setAutoCancel(true)
                .setContentIntent(createAppDetailsIntent(0, packageName))
                .setContentTitle(getNotificationTitle(packageName))
                .addAction(R.drawable.ic_cancel_black_24dp, getString(R.string.cancel),
                        createCancelDownloadIntent(this, 0, urlString))
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentText(urlString)
                .setProgress(100, 0, true);
    }

    /**
     * If downloading an apk (i.e. <code>packageName != null</code>) then the title will indicate
     * the name of the app which the apk belongs to. Otherwise, it will be a generic "Downloading..."
     * message.
     */
    private String getNotificationTitle(@Nullable String packageName) {
        if (packageName != null) {
            final App app = AppProvider.Helper.findByPackageName(
                    getContentResolver(), packageName, new String[]{AppProvider.DataColumns.NAME});
            if (app != null) {
                return getString(R.string.downloading_apk, app.name);
            }
        }
        return getString(R.string.downloading);
    }

    private PendingIntent createAppDetailsIntent(int requestCode, String packageName) {
        TaskStackBuilder stackBuilder;
        if (packageName != null) {
            Intent notifyIntent = new Intent(getApplicationContext(), AppDetails.class)
                    .putExtra(AppDetails.EXTRA_APPID, packageName);

            stackBuilder = TaskStackBuilder
                    .create(getApplicationContext())
                    .addParentStack(AppDetails.class)
                    .addNextIntent(notifyIntent);
        } else {
            Intent notifyIntent = new Intent(getApplicationContext(), FDroid.class);
            stackBuilder = TaskStackBuilder
                    .create(getApplicationContext())
                    .addParentStack(FDroid.class)
                    .addNextIntent(notifyIntent);
        }

        return stackBuilder.getPendingIntent(requestCode, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static PendingIntent createCancelDownloadIntent(@NonNull Context context, int
            requestCode, @NonNull String urlString) {
        Intent cancelIntent = new Intent(context.getApplicationContext(), DownloaderService.class)
                .setData(Uri.parse(urlString))
                .setAction(ACTION_CANCEL);
        return PendingIntent.getService(context.getApplicationContext(),
                requestCode,
                cancelIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStart(intent, startId);
        Utils.debugLog(TAG, "onStartCommand " + intent);
        return START_REDELIVER_INTENT; // if killed before completion, retry Intent
    }

    @Override
    public void onDestroy() {
        Utils.debugLog(TAG, "Destroying downloader service. Will move to background and stop our Looper.");
        stopForeground(true);
        serviceLooper.quit(); //NOPMD - this is copied from IntentService, no super call needed
    }

    /**
     * This service does not use binding, so no need to implement this method
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * This method is invoked on the worker thread with a request to process.
     * Only one Intent is processed at a time, but the processing happens on a
     * worker thread that runs independently from other application logic.
     * So, if this code takes a long time, it will hold up other requests to
     * the same DownloaderService, but it will not hold up anything else.
     * When all requests have been handled, the DownloaderService stops itself,
     * so you should not ever call {@link #stopSelf}.
     * <p/>
     * Downloads are put into subdirectories based on hostname/port of each repo
     * to prevent files with the same names from conflicting.  Each repo enforces
     * unique APK file names on the server side.
     *
     * @param intent The {@link Intent} passed via {@link
     *               android.content.Context#startService(Intent)}.
     */
    protected void handleIntent(Intent intent) {
        final Uri uri = intent.getData();
        File downloadDir = new File(Utils.getApkCacheDir(this), uri.getHost() + "-" + uri.getPort());
        downloadDir.mkdirs();
        final SanitizedFile localFile = new SanitizedFile(downloadDir, uri.getLastPathSegment());
        final String packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
        sendBroadcast(uri, Downloader.ACTION_STARTED, localFile);

        if (Preferences.get().isUpdateNotificationEnabled()) {
            Notification notification = createNotification(intent.getDataString(), intent.getStringExtra(EXTRA_PACKAGE_NAME)).build();
            startForeground(NOTIFY_DOWNLOADING, notification);
        }

        try {
            downloader = DownloaderFactory.create(this, uri, localFile);
            downloader.setListener(new Downloader.DownloaderProgressListener() {
                @Override
                public void sendProgress(URL sourceUrl, int bytesRead, int totalBytes) {
                    Intent intent = new Intent(Downloader.ACTION_PROGRESS);
                    intent.setData(uri);
                    intent.putExtra(Downloader.EXTRA_BYTES_READ, bytesRead);
                    intent.putExtra(Downloader.EXTRA_TOTAL_BYTES, totalBytes);
                    localBroadcastManager.sendBroadcast(intent);

                    if (Preferences.get().isUpdateNotificationEnabled()) {
                        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        Notification notification = createNotification(uri.toString(), packageName)
                                .setProgress(totalBytes, bytesRead, false)
                                .build();
                        nm.notify(NOTIFY_DOWNLOADING, notification);
                    }
                }
            });
            downloader.download();
            sendBroadcast(uri, Downloader.ACTION_COMPLETE, localFile);
            notifyDownloadComplete(packageName, intent.getDataString());
        } catch (InterruptedException e) {
            sendBroadcast(uri, Downloader.ACTION_INTERRUPTED, localFile);
        } catch (IOException e) {
            e.printStackTrace();
            sendBroadcast(uri, Downloader.ACTION_INTERRUPTED, localFile,
                    e.getLocalizedMessage());
        } finally {
            if (downloader != null) {
                downloader.close();
            }
        }
        downloader = null;
    }

    /**
     * Post a notification about a completed download.  {@code packageName} must be a valid
     * and currently in the app index database.
     */
    private void notifyDownloadComplete(String packageName, String urlString) {
        String title;
        try {
            PackageManager pm = getPackageManager();
            title = String.format(getString(R.string.tap_to_update_format),
                    pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)));
        } catch (PackageManager.NameNotFoundException e) {
            App app = AppProvider.Helper.findByPackageName(getContentResolver(), packageName,
                    new String[]{
                            AppProvider.DataColumns.NAME,
                    });
            title = String.format(getString(R.string.tap_to_install_format), app.name);
        }

        int downloadUrlId = urlString.hashCode();
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setAutoCancel(true)
                        .setContentTitle(title)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        .setContentIntent(createAppDetailsIntent(downloadUrlId, packageName))
                        .setContentText(getString(R.string.tap_to_install));
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(downloadUrlId, builder.build());
    }

    private void sendBroadcast(Uri uri, String action, File file) {
        sendBroadcast(uri, action, file, null);
    }

    private void sendBroadcast(Uri uri, String action, File file, String errorMessage) {
        Intent intent = new Intent(action);
        intent.setData(uri);
        intent.putExtra(Downloader.EXTRA_DOWNLOAD_PATH, file.getAbsolutePath());
        if (!TextUtils.isEmpty(errorMessage)) {
            intent.putExtra(Downloader.EXTRA_ERROR_MESSAGE, errorMessage);
        }
        localBroadcastManager.sendBroadcast(intent);
    }

    /**
     * Add a URL to the download queue.
     * <p/>
     * All notifications are sent as an {@link Intent} via local broadcasts to be received by
     *
     * @param context     this app's {@link Context}
     * @param packageName The packageName of the app being downloaded
     * @param urlString   The URL to add to the download queue
     * @see #cancel(Context, String)
     */
    public static void queue(Context context, String packageName, String urlString) {
        Utils.debugLog(TAG, "Preparing " + urlString + " to go into the download queue");
        Intent intent = new Intent(context, DownloaderService.class);
        intent.setAction(ACTION_QUEUE);
        intent.setData(Uri.parse(urlString));
        if (!TextUtils.isEmpty(packageName)) {
            intent.putExtra(EXTRA_PACKAGE_NAME, packageName);
        }
        context.startService(intent);
    }

    /**
     * Remove a URL to the download queue, even if it is currently downloading.
     * <p/>
     * All notifications are sent as an {@link Intent} via local broadcasts to be received by
     *
     * @param context   this app's {@link Context}
     * @param urlString The URL to remove from the download queue
     * @see #queue(Context, String, String)
     */
    public static void cancel(Context context, String urlString) {
        Utils.debugLog(TAG, "Preparing cancellation of " + urlString + " download");
        Intent intent = new Intent(context, DownloaderService.class);
        intent.setAction(ACTION_CANCEL);
        intent.setData(Uri.parse(urlString));
        context.startService(intent);
    }

    /**
     * Check if a URL is waiting in the queue for downloading or if actively being downloaded.
     * This is useful for checking whether to re-register {@link android.content.BroadcastReceiver}s
     * in {@link android.app.Activity#onResume()}.
     */
    public static boolean isQueuedOrActive(String urlString) {
        if (TextUtils.isEmpty(urlString)) { //NOPMD - suggests unreadable format
            return false;
        }
        return serviceHandler.hasMessages(urlString.hashCode()) || isActive(urlString);
    }

    /**
     * Check if a URL is actively being downloaded.
     */
    public static boolean isActive(String urlString) {
        return downloader != null && TextUtils.equals(urlString, downloader.sourceUrl.toString());
    }

    /**
     * Get a prepared {@link IntentFilter} for use for matching this service's action events.
     *
     * @param urlString The full file URL to match.
     * @param action    {@link Downloader#ACTION_STARTED}, {@link Downloader#ACTION_PROGRESS},
     *                  {@link Downloader#ACTION_INTERRUPTED}, or {@link Downloader#ACTION_COMPLETE},
     */
    public static IntentFilter getIntentFilter(String urlString, String action) {
        Uri uri = Uri.parse(urlString);
        IntentFilter intentFilter = new IntentFilter(action);
        intentFilter.addDataScheme(uri.getScheme());
        intentFilter.addDataAuthority(uri.getHost(), String.valueOf(uri.getPort()));
        intentFilter.addDataPath(uri.getPath(), PatternMatcher.PATTERN_LITERAL);
        return intentFilter;
    }
}
