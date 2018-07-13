package org.fdroid.fdroid.installer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.fdroid.fdroid.AppUpdateStatusManager;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Hasher;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.compat.PackageManagerCompat;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.net.Downloader;
import org.fdroid.fdroid.net.DownloaderService;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

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
 * This {@code Service} never stops itself after completing the action, e.g.
 * {@code {@link #stopSelf(int)}}, so {@code Intent}s are sometimes redelivered even
 * though they are no longer valid.  {@link #onStartCommand(Intent, int, int)} checks
 * first that the incoming {@code Intent} is not an invalid, redelivered {@code Intent}.
 * <p>
 * The canonical URL for the APK file to download is also used as the unique ID to
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
 * <li>for an {@code int} ID, use {@link String#hashCode()} or {@link Uri#hashCode()}
 * </ul></p>
 * The implementations of {@link Uri#toString()} and {@link Intent#getDataString()} both
 * include caching of the generated {@code String}, so it should be plenty fast.
 * <p>
 * This also handles downloading OBB "APK Extension" files for any APK that has one
 * assigned to it.  OBB files are queued up for download before the APK so that they
 * are hopefully in place before the APK starts.  That is not guaranteed though.
 * <p>
 * There may be multiple, available APK files with the same hash. Although it
 * is not a security issue to install one or the other, they may have different
 * metadata to display in the client.  Thus, it may result in weirdness if one
 * has a different name/description/summary, etc).
 *
 * @see <a href="https://developer.android.com/google/play/expansion-files.html">APK Expansion Files</a>
 */
@SuppressWarnings("LineLength")
public class InstallManagerService extends Service {
    private static final String TAG = "InstallManagerService";

    private static final String ACTION_INSTALL = "org.fdroid.fdroid.installer.action.INSTALL";
    private static final String ACTION_CANCEL = "org.fdroid.fdroid.installer.action.CANCEL";

    private static final String EXTRA_APP = "org.fdroid.fdroid.installer.extra.APP";
    private static final String EXTRA_APK = "org.fdroid.fdroid.installer.extra.APK";

    private static SharedPreferences pendingInstalls;

    private LocalBroadcastManager localBroadcastManager;
    private AppUpdateStatusManager appUpdateStatusManager;
    private BroadcastReceiver broadcastReceiver;
    private boolean running = false;

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
        appUpdateStatusManager = AppUpdateStatusManager.getInstance(this);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getData() == null) return;
                String packageName = intent.getData().getSchemeSpecificPart();
                for (AppUpdateStatusManager.AppUpdateStatus status : appUpdateStatusManager.getByPackageName(packageName)) {
                    appUpdateStatusManager.updateApk(status.getUniqueKey(), AppUpdateStatusManager.Status.Installed, null);
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addDataScheme("package");
        registerReceiver(broadcastReceiver, intentFilter);
        running = true;
        pendingInstalls = getPendingInstalls(this);
    }

    /**
     * If this {@link Service} is stopped, then all of the various
     * {@link BroadcastReceiver}s need to unregister themselves if they get
     * called.  There can be multiple {@code BroadcastReceiver}s registered,
     * so it can't be done with a simple call here. So {@link #running} is the
     * signal to all the existing {@code BroadcastReceiver}s to unregister.
     */
    @Override
    public void onDestroy() {
        running = false;
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    /**
     * This goes through a series of checks to make sure that the incoming
     * {@link Intent} is still valid.  The default {@link Intent#getAction() action}
     * in the logic is {@link #ACTION_INSTALL} since it is the most complicate
     * case.  Since the {@code Intent} will be redelivered by Android if the
     * app was killed, this needs to check that it still makes sense to handle.
     * <p>
     * For example, if F-Droid is killed while installing, it might not receive
     * the message that the install completed successfully. The checks need to be
     * as specific as possible so as not to block things like installing updates
     * with the same {@link PackageInfo#versionCode}, which happens sometimes,
     * and is allowed by Android.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Utils.debugLog(TAG, "onStartCommand " + intent);

        String urlString = intent.getDataString();
        if (TextUtils.isEmpty(urlString)) {
            Utils.debugLog(TAG, "empty urlString, nothing to do");
            return START_NOT_STICKY;
        }

        String action = intent.getAction();

        if (ACTION_CANCEL.equals(action)) {
            DownloaderService.cancel(this, urlString);
            Apk apk = appUpdateStatusManager.getApk(urlString);
            if (apk != null) {
                DownloaderService.cancel(this, apk.getPatchObbUrl());
                DownloaderService.cancel(this, apk.getMainObbUrl());
            }
            appUpdateStatusManager.removeApk(urlString);
            return START_NOT_STICKY;
        } else if (ACTION_INSTALL.equals(action)) {
            if (!isPendingInstall(urlString)) {
                Log.i(TAG, "Ignoring INSTALL that is not Pending Install: " + intent);
                return START_NOT_STICKY;
            }
        } else {
            Log.i(TAG, "Ignoring unknown intent action: " + intent);
            return START_NOT_STICKY;
        }

        if (!intent.hasExtra(EXTRA_APP) || !intent.hasExtra(EXTRA_APK)) {
            Utils.debugLog(TAG, urlString + " did not include both an App and Apk instance, ignoring");
            return START_NOT_STICKY;
        }

        if ((flags & START_FLAG_REDELIVERY) == START_FLAG_REDELIVERY
                && !DownloaderService.isQueuedOrActive(urlString)) {
            Utils.debugLog(TAG, urlString + " finished downloading while InstallManagerService was killed.");
            appUpdateStatusManager.removeApk(urlString);
            return START_NOT_STICKY;
        }

        App app = intent.getParcelableExtra(EXTRA_APP);
        Apk apk = intent.getParcelableExtra(EXTRA_APK);
        if (app == null || apk == null) {
            Utils.debugLog(TAG, "Intent had null EXTRA_APP and/or EXTRA_APK: " + intent);
            return START_NOT_STICKY;
        }

        PackageInfo packageInfo = Utils.getPackageInfo(this, apk.packageName);
        if ((flags & START_FLAG_REDELIVERY) == START_FLAG_REDELIVERY
                && packageInfo != null && packageInfo.versionCode == apk.versionCode
                && TextUtils.equals(packageInfo.versionName, apk.versionName)) {
            Log.i(TAG, "INSTALL Intent no longer valid since its installed, ignoring: " + intent);
            return START_NOT_STICKY;
        }

        FDroidApp.resetMirrorVars();
        DownloaderService.setTimeout(FDroidApp.getTimeout());

        appUpdateStatusManager.addApk(apk, AppUpdateStatusManager.Status.Downloading, null);

        registerPackageDownloaderReceivers(urlString);
        getObb(urlString, apk.getMainObbUrl(), apk.getMainObbFile(), apk.obbMainFileSha256);
        getObb(urlString, apk.getPatchObbUrl(), apk.getPatchObbFile(), apk.obbPatchFileSha256);

        File apkFilePath = ApkCache.getApkDownloadPath(this, intent.getData());
        long apkFileSize = apkFilePath.length();
        if (!apkFilePath.exists() || apkFileSize < apk.size) {
            Utils.debugLog(TAG, "download " + urlString + " " + apkFilePath);
            DownloaderService.queue(this, urlString, apk.repoId, urlString);
        } else if (ApkCache.apkIsCached(apkFilePath, apk)) {
            Utils.debugLog(TAG, "skip download, we have it, straight to install " + urlString + " " + apkFilePath);
            sendBroadcast(intent.getData(), Downloader.ACTION_STARTED, apkFilePath);
            sendBroadcast(intent.getData(), Downloader.ACTION_COMPLETE, apkFilePath);
        } else {
            Utils.debugLog(TAG, "delete and download again " + urlString + " " + apkFilePath);
            apkFilePath.delete();
            DownloaderService.queue(this, urlString, apk.repoId, urlString);
        }

        return START_REDELIVER_INTENT; // if killed before completion, retry Intent
    }

    private void sendBroadcast(Uri uri, String action, File file) {
        Intent intent = new Intent(action);
        intent.setData(uri);
        intent.putExtra(Downloader.EXTRA_DOWNLOAD_PATH, file.getAbsolutePath());
        localBroadcastManager.sendBroadcast(intent);
    }

    /**
     * Check if any OBB files are available, and if so, download and install them. This
     * also deletes any obsolete OBB files, per the spec, since there can be only one
     * "main" and one "patch" OBB installed at a time.
     *
     * @see <a href="https://developer.android.com/google/play/expansion-files.html">APK Expansion Files</a>
     */
    private void getObb(final String urlString, String obbUrlString,
                        final File obbDestFile, final String sha256) {
        if (obbDestFile == null || obbDestFile.exists() || TextUtils.isEmpty(obbUrlString)) {
            return;
        }
        final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!running) {
                    localBroadcastManager.unregisterReceiver(this);
                    return;
                }
                String action = intent.getAction();
                if (Downloader.ACTION_STARTED.equals(action)) {
                    Utils.debugLog(TAG, action + " " + intent);
                } else if (Downloader.ACTION_PROGRESS.equals(action)) {

                    long bytesRead = intent.getLongExtra(Downloader.EXTRA_BYTES_READ, 0);
                    long totalBytes = intent.getLongExtra(Downloader.EXTRA_TOTAL_BYTES, 0);
                    appUpdateStatusManager.updateApkProgress(urlString, totalBytes, bytesRead);
                } else if (Downloader.ACTION_COMPLETE.equals(action)) {
                    localBroadcastManager.unregisterReceiver(this);
                    File localFile = new File(intent.getStringExtra(Downloader.EXTRA_DOWNLOAD_PATH));
                    Uri localApkUri = Uri.fromFile(localFile);
                    Utils.debugLog(TAG, "OBB download completed " + intent.getDataString()
                            + " to " + localApkUri);

                    try {
                        if (Hasher.isFileMatchingHash(localFile, sha256, "SHA-256")) {
                            Utils.debugLog(TAG, "Installing OBB " + localFile + " to " + obbDestFile);
                            FileUtils.forceMkdirParent(obbDestFile);
                            FileUtils.copyFile(localFile, obbDestFile);
                            FileFilter filter = new WildcardFileFilter(
                                    obbDestFile.getName().substring(0, 4) + "*.obb");
                            for (File f : obbDestFile.getParentFile().listFiles(filter)) {
                                if (!f.equals(obbDestFile)) {
                                    Utils.debugLog(TAG, "Deleting obsolete OBB " + f);
                                    FileUtils.deleteQuietly(f);
                                }
                            }
                        } else {
                            Utils.debugLog(TAG, localFile + " deleted, did not match hash: " + sha256);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        FileUtils.deleteQuietly(localFile);
                    }
                } else if (Downloader.ACTION_INTERRUPTED.equals(action)) {
                    localBroadcastManager.unregisterReceiver(this);
                } else if (Downloader.ACTION_CONNECTION_FAILED.equals(action)) {
                    DownloaderService.queue(context, urlString, 0, urlString);
                } else {
                    throw new RuntimeException("intent action not handled!");
                }
            }
        };
        DownloaderService.queue(this, obbUrlString, 0, obbUrlString);
        localBroadcastManager.registerReceiver(downloadReceiver,
                DownloaderService.getIntentFilter(obbUrlString));
    }

    /**
     * Register a {@link BroadcastReceiver} for tracking download progress for a
     * give {@code urlString}.  There can be multiple of these registered at a time.
     */
    private void registerPackageDownloaderReceivers(String urlString) {

        BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!running) {
                    localBroadcastManager.unregisterReceiver(this);
                    return;
                }
                Uri downloadUri = intent.getData();
                String urlString = downloadUri.toString();
                long repoId = intent.getLongExtra(Downloader.EXTRA_REPO_ID, 0);
                String mirrorUrlString = intent.getStringExtra(Downloader.EXTRA_MIRROR_URL);

                switch (intent.getAction()) {
                    case Downloader.ACTION_STARTED:
                        // App should currently be in the "PendingDownload" state, so this changes it to "Downloading".
                        Intent intentObject = new Intent(context, InstallManagerService.class);
                        intentObject.setAction(ACTION_CANCEL);
                        intentObject.setData(downloadUri);
                        PendingIntent action = PendingIntent.getService(context, 0, intentObject, 0);
                        appUpdateStatusManager.updateApk(urlString, AppUpdateStatusManager.Status.Downloading, action);
                        break;
                    case Downloader.ACTION_PROGRESS:
                        long bytesRead = intent.getLongExtra(Downloader.EXTRA_BYTES_READ, 0);
                        long totalBytes = intent.getLongExtra(Downloader.EXTRA_TOTAL_BYTES, 0);
                        appUpdateStatusManager.updateApkProgress(urlString, totalBytes, bytesRead);
                        break;
                    case Downloader.ACTION_COMPLETE:
                        File localFile = new File(intent.getStringExtra(Downloader.EXTRA_DOWNLOAD_PATH));
                        Uri localApkUri = Uri.fromFile(localFile);

                        Utils.debugLog(TAG, "download completed of " + mirrorUrlString + " to " + localApkUri);
                        appUpdateStatusManager.updateApk(urlString, AppUpdateStatusManager.Status.ReadyToInstall, null);

                        localBroadcastManager.unregisterReceiver(this);
                        registerInstallReceiver(downloadUri);

                        Apk apk = appUpdateStatusManager.getApk(urlString);
                        if (apk != null) {
                            InstallerService.install(context, localApkUri, downloadUri, apk);
                        }
                        break;
                    case Downloader.ACTION_INTERRUPTED:
                        appUpdateStatusManager.setDownloadError(urlString, intent.getStringExtra(Downloader.EXTRA_ERROR_MESSAGE));
                        localBroadcastManager.unregisterReceiver(this);
                        break;
                    case Downloader.ACTION_CONNECTION_FAILED:
                        try {
                            DownloaderService.queue(context, FDroidApp.getMirror(mirrorUrlString, repoId), repoId, urlString);
                            DownloaderService.setTimeout(FDroidApp.getTimeout());
                        } catch (IOException e) {
                            appUpdateStatusManager.setDownloadError(urlString, intent.getStringExtra(Downloader.EXTRA_ERROR_MESSAGE));
                            localBroadcastManager.unregisterReceiver(this);
                        }
                        break;
                    default:
                        throw new RuntimeException("intent action not handled!");
                }
            }
        };

        localBroadcastManager.registerReceiver(downloadReceiver,
                DownloaderService.getIntentFilter(urlString));
    }

    /**
     * Register a {@link BroadcastReceiver} for tracking install progress for a
     * give {@link Uri}.  There can be multiple of these registered at a time.
     */
    private void registerInstallReceiver(Uri downloadUri) {

        BroadcastReceiver installReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!running) {
                    localBroadcastManager.unregisterReceiver(this);
                    return;
                }
                String downloadUrl = intent.getDataString();
                Apk apk;
                switch (intent.getAction()) {
                    case Installer.ACTION_INSTALL_STARTED:
                        appUpdateStatusManager.updateApk(downloadUrl, AppUpdateStatusManager.Status.Installing, null);
                        break;
                    case Installer.ACTION_INSTALL_COMPLETE:
                        appUpdateStatusManager.updateApk(downloadUrl, AppUpdateStatusManager.Status.Installed, null);
                        Apk apkComplete = appUpdateStatusManager.getApk(downloadUrl);

                        if (apkComplete != null && apkComplete.isApk()) {
                            try {
                                PackageManagerCompat.setInstaller(context, getPackageManager(), apkComplete.packageName);
                            } catch (SecurityException e) {
                                // Will happen if we fell back to DefaultInstaller for some reason.
                            }
                        }
                        localBroadcastManager.unregisterReceiver(this);
                        break;
                    case Installer.ACTION_INSTALL_INTERRUPTED:
                        apk = intent.getParcelableExtra(Installer.EXTRA_APK);
                        String errorMessage =
                                intent.getStringExtra(Installer.EXTRA_ERROR_MESSAGE);
                        if (!TextUtils.isEmpty(errorMessage)) {
                            appUpdateStatusManager.setApkError(apk, errorMessage);
                        } else {
                            appUpdateStatusManager.removeApk(downloadUrl);
                        }
                        localBroadcastManager.unregisterReceiver(this);
                        break;
                    case Installer.ACTION_INSTALL_USER_INTERACTION:
                        apk = intent.getParcelableExtra(Installer.EXTRA_APK);
                        PendingIntent installPendingIntent = intent.getParcelableExtra(Installer.EXTRA_USER_INTERACTION_PI);
                        appUpdateStatusManager.addApk(apk, AppUpdateStatusManager.Status.ReadyToInstall, installPendingIntent);
                        break;
                    default:
                        throw new RuntimeException("intent action not handled!");
                }
            }
        };

        localBroadcastManager.registerReceiver(installReceiver,
                Installer.getInstallIntentFilter(downloadUri));
    }

    /**
     * Install an APK, checking the cache and downloading if necessary before
     * starting the process.  All notifications are sent as an {@link Intent}
     * via local broadcasts to be received by {@link BroadcastReceiver}s per
     * {@code urlString}.  This also marks a given APK as in the process of
     * being installed, with the {@code urlString} of the download used as the
     * unique ID,
     * <p>
     * and the file hash used to verify that things are the same.
     *
     * @param context this app's {@link Context}
     */
    public static void queue(Context context, App app, @NonNull Apk apk) {
        String urlString = apk.getUrl();
        putPendingInstall(context, urlString, apk.packageName);
        Uri downloadUri = Uri.parse(urlString);
        Installer.sendBroadcastInstall(context, downloadUri, Installer.ACTION_INSTALL_STARTED, apk,
                null, null);
        Utils.debugLog(TAG, "queue " + app.packageName + " " + apk.versionCode + " from " + urlString);
        Intent intent = new Intent(context, InstallManagerService.class);
        intent.setAction(ACTION_INSTALL);
        intent.setData(downloadUri);
        intent.putExtra(EXTRA_APP, app);
        intent.putExtra(EXTRA_APK, apk);
        context.startService(intent);
    }

    public static void cancel(Context context, String urlString) {
        removePendingInstall(context, urlString);
        Intent intent = new Intent(context, InstallManagerService.class);
        intent.setAction(ACTION_CANCEL);
        intent.setData(Uri.parse(urlString));
        context.startService(intent);
    }

    /**
     * Is the APK that matches the provided {@code hash} still waiting to be
     * installed?  This restarts the install process for this APK if it was
     * interrupted somehow, like if F-Droid was killed before the download
     * completed, or the device lost power in the middle of the install
     * process.
     */
    public boolean isPendingInstall(String urlString) {
        return pendingInstalls.contains(urlString);
    }

    /**
     * Mark a given APK as in the process of being installed, with
     * the {@code urlString} of the download used as the unique ID,
     * and the file hash used to verify that things are the same.
     *
     * @see #isPendingInstall(String)
     */
    public static void putPendingInstall(Context context, String urlString, String packageName) {
        if (pendingInstalls == null) {
            pendingInstalls = getPendingInstalls(context);
        }
        pendingInstalls.edit().putString(urlString, packageName).apply();
    }

    public static void removePendingInstall(Context context, String urlString) {
        if (pendingInstalls == null) {
            pendingInstalls = getPendingInstalls(context);
        }
        pendingInstalls.edit().remove(urlString).apply();
    }

    private static SharedPreferences getPendingInstalls(Context context) {
        return context.getSharedPreferences("pending-installs", Context.MODE_PRIVATE);
    }
}
