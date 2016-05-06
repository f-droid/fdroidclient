package org.fdroid.fdroid.installer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;

import org.fdroid.fdroid.AppDetails;
import org.fdroid.fdroid.FDroid;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.net.Downloader;
import org.fdroid.fdroid.net.DownloaderService;

import java.io.File;
import java.util.HashMap;

/**
 * Manages the whole process when a background update triggers an install or the user
 * requests an APK to be installed.  It handles checking whether the APK is cached,
 * downloading it, putting up and maintaining a {@link Notification}, and more.
 * <p>
 * Data is sent via {@link Intent}s so that Android handles the message queuing
 * and {@link Service} lifecycle for us, although it adds one layer of redirection
 * between the static method to send the {@code Intent} and the method to
 * actually process it.
 * <p>
 * The full URL for the APK file to download is also used as the unique ID to
 * represent the download itself throughout F-Droid.  This follows the model
 * of {@link Intent#setData(Uri)}, where the core data of an {@code Intent} is
 * a {@code Uri}.  The full download URL is guaranteed to be unique since it
 * points to files on a filesystem, where there cannot be multiple files with
 * the same name.  This provides a unique ID beyond just {@code packageName}
 * and {@code versionCode} since there could be different copies of the same
 * APK on different servers, signed by different keys, or even different builds.
 * <p><ul>
 * <li>for a {@link Uri} ID, use {@code Uri}, {@link Intent#getData()}
 * <li>for a {@code String} ID, use {@code urlString}, {@link Uri#toString()}, or
 * {@link Intent#getDataString()}
 * <li>for an {@code int} ID, use {@link String#hashCode()}
 * </ul></p>
 */
public class InstallManagerService extends Service {
    public static final String TAG = "InstallManagerService";

    private static final String ACTION_INSTALL = "org.fdroid.fdroid.InstallManagerService.action.INSTALL";
    private static final int NOTIFY_DOWNLOADING = 0x2344;

    /**
     * The collection of APKs that are actively going through this whole process.
     */
    private static final HashMap<String, Apk> ACTIVE_APKS = new HashMap<String, Apk>(3);

    /**
     * The array of active {@link BroadcastReceiver}s for each active APK. The key is the
     * download URL, as in {@link Apk#getUrl()} or {@code urlString}.
     */
    private final HashMap<String, BroadcastReceiver[]> receivers = new HashMap<String, BroadcastReceiver[]>(3);

    private LocalBroadcastManager localBroadcastManager;

    /**
     * This service does not use binding, so no need to implement this method
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Utils.debugLog(TAG, "creating Service");
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Utils.debugLog(TAG, "onStartCommand " + intent);
        String urlString = intent.getDataString();
        Apk apk = ACTIVE_APKS.get(urlString);

        Notification notification = createNotification(intent.getDataString(), apk.packageName).build();
        startForeground(NOTIFY_DOWNLOADING, notification);

        registerDownloaderReceivers(urlString);

        File apkFilePath = Utils.getApkDownloadPath(this, intent.getData());
        long apkFileSize = apkFilePath.length();
        if (!apkFilePath.exists() || apkFileSize < apk.size) {
            Utils.debugLog(TAG, "download " + urlString + " " + apkFilePath);
            DownloaderService.queue(this, urlString);
        } else if (apkFileSize == apk.size) {
            Utils.debugLog(TAG, "skip download, we have it, straight to install " + urlString + " " + apkFilePath);
            sendBroadcast(intent.getData(), Downloader.ACTION_STARTED, apkFilePath);
            sendBroadcast(intent.getData(), Downloader.ACTION_COMPLETE, apkFilePath);
        } else {
            Utils.debugLog(TAG, " delete and download again " + urlString + " " + apkFilePath);
            apkFilePath.delete();
            DownloaderService.queue(this, urlString);
        }
        return START_REDELIVER_INTENT; // if killed before completion, retry Intent
    }

    private void sendBroadcast(Uri uri, String action, File file) {
        Intent intent = new Intent(action);
        intent.setData(uri);
        intent.putExtra(Downloader.EXTRA_DOWNLOAD_PATH, file.getAbsolutePath());
        localBroadcastManager.sendBroadcast(intent);
    }

    private void unregisterDownloaderReceivers(String urlString) {
        for (BroadcastReceiver receiver : receivers.get(urlString)) {
            localBroadcastManager.unregisterReceiver(receiver);
        }
    }

    private void registerDownloaderReceivers(String urlString) {
        BroadcastReceiver startedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
            }
        };
        BroadcastReceiver progressReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String urlString = intent.getDataString();
                Apk apk = ACTIVE_APKS.get(urlString);
                int bytesRead = intent.getIntExtra(Downloader.EXTRA_BYTES_READ, 0);
                int totalBytes = intent.getIntExtra(Downloader.EXTRA_TOTAL_BYTES, 0);
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                Notification notification = createNotification(urlString, apk.packageName)
                        .setProgress(totalBytes, bytesRead, false)
                        .build();
                nm.notify(NOTIFY_DOWNLOADING, notification);
            }
        };
        BroadcastReceiver completeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String urlString = intent.getDataString();
                Apk apk = ACTIVE_APKS.remove(urlString);
                notifyDownloadComplete(apk.packageName, intent.getDataString());
                unregisterDownloaderReceivers(urlString);
            }
        };
        BroadcastReceiver interruptedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String urlString = intent.getDataString();
                ACTIVE_APKS.remove(urlString);
                unregisterDownloaderReceivers(urlString);
            }
        };
        localBroadcastManager.registerReceiver(startedReceiver,
                DownloaderService.getIntentFilter(urlString, Downloader.ACTION_STARTED));
        localBroadcastManager.registerReceiver(progressReceiver,
                DownloaderService.getIntentFilter(urlString, Downloader.ACTION_PROGRESS));
        localBroadcastManager.registerReceiver(completeReceiver,
                DownloaderService.getIntentFilter(urlString, Downloader.ACTION_COMPLETE));
        localBroadcastManager.registerReceiver(interruptedReceiver,
                DownloaderService.getIntentFilter(urlString, Downloader.ACTION_INTERRUPTED));
        receivers.put(urlString, new BroadcastReceiver[]{
                startedReceiver, progressReceiver, completeReceiver, interruptedReceiver,
        });
    }

    private NotificationCompat.Builder createNotification(String urlString, @Nullable String packageName) {
        int downloadUrlId = urlString.hashCode();
        return new NotificationCompat.Builder(this)
                .setAutoCancel(true)
                .setContentIntent(getAppDetailsIntent(downloadUrlId, apk.packageName))
                .setContentTitle(getNotificationTitle(packageName))
                .addAction(R.drawable.ic_cancel_black_24dp, getString(R.string.cancel),
                        DownloaderService.getCancelPendingIntent(this, urlString))
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
                    getContentResolver(), packageName, new String[]{AppProvider.DataColumns.NAME});
            title = getString(R.string.downloading_apk, app.name);
        } else {
            title = getString(R.string.downloading);
        }
        return title;
    }

    private PendingIntent getAppDetailsIntent(int requestCode, String packageName) {
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
                        .setContentIntent(getAppDetailsIntent(downloadUrlId, packageName))
                        .setContentText(getString(R.string.tap_to_install));
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(downloadUrlId, builder.build());
    }

    /**
     * Install an APK, checking the cache and downloading if necessary before starting the process.
     * All notifications are sent as an {@link Intent} via local broadcasts to be received by
     *
     * @param context this app's {@link Context}
     */
    public static void queue(Context context, App app, Apk apk) {
        String urlString = apk.getUrl();
        Utils.debugLog(TAG, "queue " + app.packageName + " " + apk.versionCode + " from " + urlString);
        ACTIVE_APKS.put(urlString, apk);
        Intent intent = new Intent(context, InstallManagerService.class);
        intent.setAction(ACTION_INSTALL);
        intent.setData(Uri.parse(urlString));
        context.startService(intent);
    }
}
