package org.fdroid.fdroid.installer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import org.fdroid.fdroid.AppDetails;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.net.Downloader;
import org.fdroid.fdroid.net.DownloaderService;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manages the whole process when a background update triggers an install or the user
 * requests an APK to be installed.  It handles checking whether the APK is cached,
 * downloading it, putting up and maintaining a {@link Notification}, and more.
 * <p>
 * The {@link App} and {@link Apk} instances are sent via
 * {@link Intent#putExtra(String, android.os.Bundle)}
 * so that Android handles the message queuing and {@link Service} lifecycle for us.
 * For example, if this {@code InstallManagerService} gets killed, Android will cache
 * and then redeliver the {@link Intent} for us, which includes all of the data needed
 * for {@code InstallManagerService} to do its job for the whole lifecycle of an install.
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

    private static final String ACTION_INSTALL = "org.fdroid.fdroid.installer.action.INSTALL";

    private static final String EXTRA_APP = "org.fdroid.fdroid.installer.extra.APP";
    private static final String EXTRA_APK = "org.fdroid.fdroid.installer.extra.APK";

    /**
     * The collection of {@link Apk}s that are actively going through this whole process,
     * matching the {@link App}s in {@code ACTIVE_APPS}. The key is the download URL, as
     * in {@link Apk#getUrl()} or {@code urlString}.
     */
    private static final HashMap<String, Apk> ACTIVE_APKS = new HashMap<>(3);

    /**
     * The collection of {@link App}s that are actively going through this whole process,
     * matching the {@link Apk}s in {@code ACTIVE_APKS}. The key is the
     * {@code packageName} of the app.
     */
    private static final HashMap<String, App> ACTIVE_APPS = new HashMap<>(3);

    /**
     * The array of active {@link BroadcastReceiver}s for each active APK. The key is the
     * download URL, as in {@link Apk#getUrl()} or {@code urlString}.
     */
    private final HashMap<String, BroadcastReceiver[]> receivers = new HashMap<>(3);

    private LocalBroadcastManager localBroadcastManager;
    private NotificationManager notificationManager;

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
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        BroadcastReceiver br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String packageName = intent.getData().getSchemeSpecificPart();
                for (Map.Entry<String, Apk> entry : ACTIVE_APKS.entrySet()) {
                    if (TextUtils.equals(packageName, entry.getValue().packageName)) {
                        String urlString = entry.getKey();
                        cancelNotification(urlString);
                        break;
                    }
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addDataScheme("package");
        registerReceiver(br, intentFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Utils.debugLog(TAG, "onStartCommand " + intent);

        if (!ACTION_INSTALL.equals(intent.getAction())) {
            Utils.debugLog(TAG, "Ignoring " + intent + " as it is not an " + ACTION_INSTALL + " intent");
            return START_NOT_STICKY;
        }

        String urlString = intent.getDataString();
        if (TextUtils.isEmpty(urlString)) {
            Utils.debugLog(TAG, "empty urlString, nothing to do");
            return START_NOT_STICKY;
        }

        if (!intent.hasExtra(EXTRA_APP) || !intent.hasExtra(EXTRA_APK)) {
            Utils.debugLog(TAG, urlString + " did not include both an App and Apk instance, ignoring");
            return START_NOT_STICKY;
        }

        if ((flags & START_FLAG_REDELIVERY) == START_FLAG_REDELIVERY
                && !DownloaderService.isQueuedOrActive(urlString)) {
            Utils.debugLog(TAG, urlString + " finished downloading while InstallManagerService was killed.");
            cancelNotification(urlString);
            return START_NOT_STICKY;
        }

        App app = new App(intent.getParcelableExtra(EXTRA_APP));
        Apk apk = new Apk(intent.getParcelableExtra(EXTRA_APK));
        addToActive(urlString, app, apk);

        NotificationCompat.Builder builder = createNotificationBuilder(intent.getDataString(), apk);
        notificationManager.notify(urlString.hashCode(), builder.build());

        registerDownloaderReceivers(urlString, builder);

        File apkFilePath = Utils.getApkDownloadPath(this, intent.getData());
        long apkFileSize = apkFilePath.length();
        if (!apkFilePath.exists() || apkFileSize < apk.size) {
            Utils.debugLog(TAG, "download " + urlString + " " + apkFilePath);
            DownloaderService.queue(this, urlString);
        } else if (apkIsCached(apkFilePath, apk)) {
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

    /**
     * Verifies the size of the file on disk matches, and then hashes the file to compare with what
     * we received from the signed repo (i.e. {@link Apk#hash} and {@link Apk#hashType}).
     * Bails out if the file sizes don't match to prevent having to do the work of hashing the file.
     */
    private static boolean apkIsCached(File apkFile, Apk apkToCheck) {
        try {
            return apkFile.length() == apkToCheck.size &&
                    Installer.verifyApkFile(apkFile, apkToCheck.hash, apkToCheck.hashType);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
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

    private void registerDownloaderReceivers(String urlString, final NotificationCompat.Builder builder) {
        BroadcastReceiver startedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
            }
        };
        BroadcastReceiver progressReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String urlString = intent.getDataString();
                int bytesRead = intent.getIntExtra(Downloader.EXTRA_BYTES_READ, 0);
                int totalBytes = intent.getIntExtra(Downloader.EXTRA_TOTAL_BYTES, 0);
                builder.setProgress(totalBytes, bytesRead, false);
                notificationManager.notify(urlString.hashCode(), builder.build());
            }
        };
        BroadcastReceiver completeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // elsewhere called urlString
                Uri originatingUri = intent.getData();
                File localFile = new File(intent.getStringExtra(Downloader.EXTRA_DOWNLOAD_PATH));
                Uri localUri = Uri.fromFile(localFile);

                Utils.debugLog(TAG, "download completed of " + originatingUri
                        + " to " + localUri);

                unregisterDownloaderReceivers(intent.getDataString());

                registerInstallerReceivers(localUri);
                InstallerService.install(context, localUri, originatingUri);
            }
        };
        BroadcastReceiver interruptedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String urlString = intent.getDataString();
                Apk apk = removeFromActive(urlString);
                unregisterDownloaderReceivers(urlString);
                cancelNotification(urlString);
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

    private void registerInstallerReceivers(Uri uri) {

        BroadcastReceiver installReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case InstallHelper.ACTION_INSTALL_STARTED: {
                        Utils.debugLog(TAG, "ACTION_INSTALL_STARTED");

                        break;
                    }
                    case InstallHelper.ACTION_INSTALL_COMPLETE: {
                        Utils.debugLog(TAG, "ACTION_INSTALL_COMPLETE");

                        Uri originatingUri =
                                intent.getParcelableExtra(InstallHelper.EXTRA_ORIGINATING_URI);
                        String urlString = originatingUri.toString();
                        removeFromActive(urlString);

                        localBroadcastManager.unregisterReceiver(this);

                        break;
                    }
                    case InstallHelper.ACTION_INSTALL_INTERRUPTED: {
                        Utils.debugLog(TAG, "ACTION_INSTALL_INTERRUPTED");

                        localBroadcastManager.unregisterReceiver(this);

                        break;
                    }
                    case InstallHelper.ACTION_INSTALL_USER_INTERACTION: {
                        Utils.debugLog(TAG, "ACTION_INSTALL_USER_INTERACTION");

                        Uri originatingUri =
                                intent.getParcelableExtra(InstallHelper.EXTRA_ORIGINATING_URI);
                        PendingIntent installPendingIntent =
                                intent.getParcelableExtra(InstallHelper.EXTRA_USER_INTERACTION_PI);
                        // TODO
                        String urlString = originatingUri.toString();
                        Apk apk = getFromActive(urlString);
                        Utils.debugLog(TAG, "urlString: " + urlString);

                        if (AppDetails.isAppVisible(apk.packageName)) {
                            cancelNotification(urlString);
                        } else {
                            notifyDownloadComplete(apk, urlString, installPendingIntent);
                        }

                        break;
                    }
                    default: {
                        throw new RuntimeException("intent action not handled!");
                    }
                }
            }
        };

        localBroadcastManager.registerReceiver(installReceiver,
                InstallerService.getInstallIntentFilter(uri));
    }

    private NotificationCompat.Builder createNotificationBuilder(String urlString, Apk apk) {
        int downloadUrlId = urlString.hashCode();
        return new NotificationCompat.Builder(this)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(getAppDetailsIntent(downloadUrlId, apk))
                .setContentTitle(getString(R.string.downloading_apk, getAppName(urlString, apk)))
                .addAction(R.drawable.ic_cancel_black_24dp, getString(R.string.cancel),
                        DownloaderService.getCancelPendingIntent(this, urlString))
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentText(urlString)
                .setProgress(100, 0, true);
    }

    private String getAppName(String urlString, Apk apk) {
        App app = ACTIVE_APPS.get(apk.packageName);
        return app.name;
    }

    /**
     * Get a {@link PendingIntent} for a {@link Notification} to send when it
     * is clicked.  {@link AppDetails} handles {@code Intent}s that are missing
     * or bad {@link AppDetails#EXTRA_APPID}, so it does not need to be checked
     * here.
     */
    private PendingIntent getAppDetailsIntent(int requestCode, Apk apk) {
        Intent notifyIntent = new Intent(getApplicationContext(), AppDetails.class)
                .putExtra(AppDetails.EXTRA_APPID, apk.packageName);
        return TaskStackBuilder.create(getApplicationContext())
                .addParentStack(AppDetails.class)
                .addNextIntent(notifyIntent)
                .getPendingIntent(requestCode, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Post a notification about a completed download.  {@code packageName} must be a valid
     * and currently in the app index database.  This must create a new {@code Builder}
     * instance otherwise the progress/cancel stuff does not go away.
     *
     * @see <a href=https://code.google.com/p/android/issues/detail?id=47809> Issue 47809:
     * Removing the progress bar from a notification should cause the notification's content
     * text to return to normal size</a>
     */
    private void notifyDownloadComplete(Apk apk, String urlString, PendingIntent installPendingIntent) {
        String title;
        try {
            PackageManager pm = getPackageManager();
            title = String.format(getString(R.string.tap_to_update_format),
                    pm.getApplicationLabel(pm.getApplicationInfo(apk.packageName, 0)));
        } catch (PackageManager.NameNotFoundException e) {
            title = String.format(getString(R.string.tap_to_install_format), getAppName(urlString, apk));
        }

        int downloadUrlId = urlString.hashCode();
        notificationManager.cancel(downloadUrlId);
        Notification notification = new NotificationCompat.Builder(this)
                .setAutoCancel(true)
                .setOngoing(false)
                .setContentTitle(title)
                .setContentIntent(installPendingIntent)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentText(getString(R.string.tap_to_install))
                .build();
        notificationManager.notify(downloadUrlId, notification);
    }

    /**
     * Cancel the {@link Notification} tied to {@code urlString}, which is the
     * unique ID used to represent a given APK file. {@link String#hashCode()}
     * converts {@code urlString} to the required {@code int}.
     */
    private void cancelNotification(String urlString) {
        notificationManager.cancel(urlString.hashCode());
    }

    private static void addToActive(String urlString, App app, Apk apk) {
        ACTIVE_APKS.put(urlString, apk);
        ACTIVE_APPS.put(app.packageName, app);
    }

    private static Apk getFromActive(String urlString) {
        return ACTIVE_APKS.get(urlString);
    }

    /**
     * Remove the {@link App} and {@Apk} instances that are associated with
     * {@code urlString} from the {@link Map} of active apps.  This can be
     * called after this service has been destroyed and recreated based on the
     * {@link BroadcastReceiver}s, in which case {@code urlString} would not
     * find anything in the active maps.
     */
    private static Apk removeFromActive(String urlString) {
        Apk apk = ACTIVE_APKS.remove(urlString);
        if (apk != null) {
            ACTIVE_APPS.remove(apk.packageName);
        }
        return apk;
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
        Intent intent = new Intent(context, InstallManagerService.class);
        intent.setAction(ACTION_INSTALL);
        intent.setData(Uri.parse(urlString));
        intent.putExtra(EXTRA_APP, app.toContentValues());
        intent.putExtra(EXTRA_APK, apk.toContentValues());
        context.startService(intent);
    }

    /**
     * Returns a {@link Set} of the {@code urlString}s that are currently active.
     * {@code urlString}s are used as unique IDs throughout the
     * {@code InstallManagerService} process, either as a {@code String} or as an
     * {@code int} from {@link String#hashCode()}.
     */
    public static Set<String> getActiveDownloadUrls() {
        return ACTIVE_APKS.keySet();
    }

    /**
     * Returns a {@link Set} of the {@code packageName}s that are currently active.
     * {@code packageName}s are used as unique IDs for apps throughout all of
     * Android, F-Droid, and other apps stores.
     */
    public static Set<String> getActivePackageNames() {
        return ACTIVE_APPS.keySet();
    }
}
