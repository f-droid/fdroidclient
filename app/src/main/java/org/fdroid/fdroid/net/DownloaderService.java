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
import java.util.HashMap;

/**
 * DownloaderService is a service that handles asynchronous download requests
 * (expressed as {@link Intent}s) on demand.  Clients send download requests
 * through {@link android.content.Context#startService(Intent)} calls; the
 * service is started as needed, handles each Intent in turn using a worker
 * thread, and stops itself when it runs out of work.
 * <p/>
 * <p>This "work queue processor" pattern is commonly used to offload tasks
 * from an application's main thread.  The DownloaderService class exists to
 * simplify this pattern and take care of the mechanics. DownloaderService
 * will receive the Intents, launch a worker thread, and stop the service as
 * appropriate.
 * <p/>
 * <p>All requests are handled on a single worker thread -- they may take as
 * long as necessary (and will not block the application's main loop), but
 * only one request will be processed at a time.
 * <p/>
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For a detailed discussion about how to create services, read the
 * <a href="{@docRoot}guide/topics/fundamentals/services.html">Services</a> developer guide.</p>
 * </div>
 *
 * @see android.os.AsyncTask
 */
public class DownloaderService extends Service {
    public static final String TAG = "DownloaderService";

    static final String EXTRA_PACKAGE_NAME = "org.fdroid.fdroid.net.DownloaderService.extra.PACKAGE_NAME";

    private static final String ACTION_QUEUE = "org.fdroid.fdroid.net.DownloaderService.action.QUEUE";
    private static final String ACTION_CANCEL = "org.fdroid.fdroid.net.DownloaderService.action.CANCEL";

    private static final int NOTIFY_DOWNLOADING = 0x2344;

    private volatile Looper serviceLooper;
    private static volatile ServiceHandler serviceHandler;
    private static volatile Downloader downloader;
    private LocalBroadcastManager localBroadcastManager;

    private static final HashMap<String, Integer> QUEUE_WHATS = new HashMap<String, Integer>();
    private int what;

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
        Log.i(TAG, "onCreate");

        HandlerThread thread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.i(TAG, "onStart " + startId + " " + intent);
        String uriString = intent.getDataString();
        if (uriString == null) {
            Log.e(TAG, "Received Intent with no URI: " + intent);
            return;
        }
        if (ACTION_CANCEL.equals(intent.getAction())) {
            Log.i(TAG, "Removed " + intent);
            if (isQueued(uriString)) {
                serviceHandler.removeMessages(what);
            } else if (isActive(uriString)) {
                downloader.cancelDownload();
            } else {
                Log.e(TAG, "CANCEL called on something not queued or running: " + startId + " " + intent);
            }

            QUEUE_WHATS.remove(uriString);
            if (isQueueEmpty()) {
                stopForeground(true);
            }
        } else if (ACTION_QUEUE.equals(intent.getAction())) {
            if (Preferences.get().isUpdateNotificationEnabled()) {
                Notification notification = createNotification(intent.getDataString(), getPackageNameFromIntent(intent)).build();
                startForeground(NOTIFY_DOWNLOADING, notification);
            }
            Log.i(TAG, "Queued " + intent);
            Message msg = serviceHandler.obtainMessage();
            msg.arg1 = startId;
            msg.obj = intent;
            msg.what = what++;
            serviceHandler.sendMessage(msg);
            Log.i(TAG, "QUEUE_WHATS.put(" + uriString + ", " + msg.what);
            QUEUE_WHATS.put(uriString, msg.what);
        } else {
            Log.e(TAG, "Received Intent with unknown action: " + intent);
        }
    }

    @Nullable
    private static String getPackageNameFromIntent(@NonNull Intent intent) {
        return intent.hasExtra(EXTRA_PACKAGE_NAME) ? intent.getStringExtra(EXTRA_PACKAGE_NAME) : null;
    }

    private NotificationCompat.Builder createNotification(String urlString, @Nullable String packageName) {
        return new NotificationCompat.Builder(this)
                .setAutoCancel(true)
                .setContentIntent(createAppDetailsIntent(this, 0, packageName))
                .setContentTitle(getNotificationTitle(packageName))
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
        String title;
        if (packageName != null) {
            App app = AppProvider.Helper.findByPackageName(
                    getContentResolver(), packageName, new String[] { AppProvider.DataColumns.NAME });
            title = getString(R.string.downloading_apk, app.name);
        } else {
            title = getString(R.string.downloading);
        }
        return title;
    }

    public static PendingIntent createAppDetailsIntent(@NonNull Context context, int requestCode, @Nullable String packageName) {
        TaskStackBuilder stackBuilder;
        if (packageName != null) {
            Intent notifyIntent = new Intent(context.getApplicationContext(), AppDetails.class)
                    .putExtra(AppDetails.EXTRA_APPID, packageName);

            stackBuilder = TaskStackBuilder
                    .create(context.getApplicationContext())
                    .addParentStack(AppDetails.class)
                    .addNextIntent(notifyIntent);
        } else {
            Intent notifyIntent = new Intent(context.getApplicationContext(), FDroid.class);
            stackBuilder = TaskStackBuilder
                    .create(context.getApplicationContext())
                    .addParentStack(FDroid.class)
                    .addNextIntent(notifyIntent);
        }

        return stackBuilder.getPendingIntent(requestCode, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStart(intent, startId);
        Log.i(TAG, "onStartCommand " + intent);
        return START_REDELIVER_INTENT; // if killed before completion, retry Intent
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
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
        final String packageName = getPackageNameFromIntent(intent);
        sendBroadcast(uri, Downloader.ACTION_STARTED, localFile);
        try {
            downloader = DownloaderFactory.create(this, uri, localFile);
            downloader.setListener(new Downloader.DownloaderProgressListener() {
                @Override
                public void sendProgress(URL sourceUrl, int bytesRead, int totalBytes) {
                    if (isActive(uri.toString())) {
                        Intent intent = new Intent(Downloader.ACTION_PROGRESS);
                        intent.setData(uri);
                        intent.putExtra(Downloader.EXTRA_BYTES_READ, bytesRead);
                        intent.putExtra(Downloader.EXTRA_TOTAL_BYTES, totalBytes);
                        localBroadcastManager.sendBroadcast(intent);

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
            DownloadCompleteService.notify(this, packageName, intent.getDataString());
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
            // May have already been removed in response to a cancel intent, but that wont cause
            // problems if we ask to remove it again.
            QUEUE_WHATS.remove(uri.toString());
        }
        downloader = null;
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
     * @param context
     * @param packageName The packageName of the app being downloaded
     * @param urlString   The URL to add to the download queue
     * @see #cancel(Context, String)
     */
    public static void queue(Context context, String packageName, String urlString) {
        Log.i(TAG, "queue " + urlString);
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
     * @param context
     * @param urlString The URL to remove from the download queue
     * @see #queue(Context, String, String)
     */
    public static void cancel(Context context, String urlString) {
        Log.i(TAG, "cancel " + urlString);
        Intent intent = new Intent(context, DownloaderService.class);
        intent.setAction(ACTION_CANCEL);
        intent.setData(Uri.parse(urlString));
        context.startService(intent);
    }

    /**
     * Check if a URL is waiting in the queue for downloading or if actively being downloaded.
     * This is useful for checking whether to re-register {@link android.content.BroadcastReceiver}s
     * in {@link android.app.Activity#onResume()}.
     * @see DownloaderService#isQueued(String)
     * @see DownloaderService#isActive(String)
     */
    public static boolean isQueuedOrActive(String urlString) {
         return isQueued(urlString) || isActive(urlString);
    }

    public static boolean isQueueEmpty() {
        return QUEUE_WHATS.isEmpty();
    }

    /**
     * Check if a URL is waiting in the queue for downloading.
     */
    public static boolean isQueued(String urlString) {
        if (TextUtils.isEmpty(urlString)) {
            return false;
        }
        Integer what = QUEUE_WHATS.get(urlString);
        return (what != null && serviceHandler.hasMessages(what));
    }

    /**
     * Check if a URL is actively being downloaded.
     */
    public static boolean isActive(String urlString) {
        return downloader != null && QUEUE_WHATS.containsKey(urlString) && TextUtils.equals(urlString, downloader.sourceUrl.toString());
    }

    /**
     * Get a prepared {@link IntentFilter} for use for matching this service's action events.
     *
     * @param urlString The full file URL to match.
     * @param action    {@link Downloader#ACTION_STARTED}, {@link Downloader#ACTION_PROGRESS},
     *                  {@link Downloader#ACTION_INTERRUPTED}, or {@link Downloader#ACTION_COMPLETE},
     * @return
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
