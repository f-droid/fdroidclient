package org.fdroid.fdroid.installer;

import static vendored.org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_256;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.pm.PackageInfoCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.fdroid.fdroid.AppUpdateStatusManager;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.compat.PackageManagerCompat;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.SanitizedFile;
import org.fdroid.fdroid.net.DownloaderService;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

/**
 * Manages the whole process when a background update triggers an install or the user
 * requests an APK to be installed.  It handles checking whether the APK is cached,
 * downloading it, putting up and maintaining a {@link Notification}, and more. This
 * class tracks packages that are in the process as "Pending Installs".
 * Then {@link DownloaderService} and {@link InstallerService} individually track
 * packages for those phases of the whole install process.  Each of those
 * {@code Services} have their own related events.  For tracking status during the
 * whole process, {@link AppUpdateStatusManager} tracks the status as represented by
 * {@link AppUpdateStatusManager.AppUpdateStatus}.
 * <p>
 * The {@link App} and {@link Apk} instances are sent via
 * {@link Intent#putExtra(String, android.os.Bundle)}
 * so that Android handles the message queuing and {@link Service} lifecycle for us.
 * For example, if {@link DownloaderService} gets killed, Android will cache
 * and then redeliver the {@link Intent} for us, which includes all of the data needed
 * for {@code InstallManagerService} to do its job for the whole lifecycle of an install.
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
 * <li>for a {@code String} ID, use {@code canonicalUrl}, {@link Uri#toString()}, or
 * {@link Intent#getDataString()}
 * <li>for an {@code int} ID, use {@link String#hashCode()} or {@link Uri#hashCode()}
 * <li>for an {@link Intent} extra, use {@link DownloaderService#EXTRA_CANONICAL_URL} and include a
 * {@link String} instance
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
 * @see <a href="https://gitlab.com/fdroid/fdroidclient/-/merge_requests/1089#note_822501322">forced to vendor Apache Commons Codec</a>
 */
@SuppressWarnings("LineLength")
public class InstallManagerService {
    private static final String TAG = "InstallManagerService";

    private static final String ACTION_CANCEL = "org.fdroid.fdroid.installer.action.CANCEL";

    @SuppressLint("StaticFieldLeak") // we are using ApplicationContext, so hopefully that's fine
    private static InstallManagerService instance;

    private final Context context;
    private final LocalBroadcastManager localBroadcastManager;
    private final AppUpdateStatusManager appUpdateStatusManager;

    public static InstallManagerService getInstance(Context context) {
        if (instance == null) {
            instance = new InstallManagerService(context.getApplicationContext());
        }
        return instance;
    }

    public InstallManagerService(Context context) {
        this.context = context;
        this.localBroadcastManager = LocalBroadcastManager.getInstance(context);
        this.appUpdateStatusManager = AppUpdateStatusManager.getInstance(context);
        // cancel intent can't use LocalBroadcastManager, because it comes from system process
        IntentFilter cancelFilter = new IntentFilter();
        cancelFilter.addAction(ACTION_CANCEL);
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Received cancel intent: " + intent);
                if (!ACTION_CANCEL.equals(intent.getAction())) return;
                cancel(context, intent.getStringExtra(DownloaderService.EXTRA_CANONICAL_URL));
            }
        }, cancelFilter);
    }

    private void onCancel(String canonicalUrl) {
        DownloaderService.cancel(canonicalUrl);
        Apk apk = appUpdateStatusManager.getApk(canonicalUrl);
        if (apk != null) {
            Utils.debugLog(TAG, "also canceling OBB downloads");
            DownloaderService.cancel(apk.getPatchObbUrl());
            DownloaderService.cancel(apk.getMainObbUrl());
        }
    }

    /**
     * This goes through a series of checks to make sure that the incoming
     * {@link Intent} is still valid.
     * <p>
     * For example, if F-Droid is killed while installing, it might not receive
     * the message that the install completed successfully. The checks need to be
     * as specific as possible so as not to block things like installing updates
     * with the same {@link PackageInfo#versionCode}, which happens sometimes,
     * and is allowed by Android.
     */
    private void queue(String canonicalUrl, String packageName, @NonNull App app, @NonNull Apk apk) {
        Utils.debugLog(TAG, "queue " + packageName);

        if (TextUtils.isEmpty(canonicalUrl)) {
            Utils.debugLog(TAG, "empty canonicalUrl, nothing to do");
            return;
        }

        PackageInfo packageInfo = Utils.getPackageInfo(context, packageName);
        if (packageInfo != null && PackageInfoCompat.getLongVersionCode(packageInfo) == apk.versionCode
                && TextUtils.equals(packageInfo.versionName, apk.versionName)) {
            Log.i(TAG, "Install action no longer valid since its installed, ignoring: " + packageName);
            return;
        }

        appUpdateStatusManager.addApk(app, apk, AppUpdateStatusManager.Status.Downloading, null);

        getMainObb(canonicalUrl, apk);
        getPatchObb(canonicalUrl, apk);

        Utils.runOffUiThread(() -> ApkCache.getApkCacheState(context, apk), pair -> {
            ApkCache.ApkCacheState state = pair.first;
            SanitizedFile apkFilePath = pair.second;
            if (state == ApkCache.ApkCacheState.MISS_OR_PARTIAL) {
                Utils.debugLog(TAG, "download " + canonicalUrl + " " + apkFilePath);
                DownloaderService.queue(context, canonicalUrl, app, apk);
            } else if (state == ApkCache.ApkCacheState.CACHED) {
                Utils.debugLog(TAG, "skip download, we have it, straight to install " + canonicalUrl + " " + apkFilePath);
                Uri canonicalUri = Uri.parse(canonicalUrl);
                onDownloadStarted(canonicalUri);
                onDownloadComplete(canonicalUri, apkFilePath, app, apk);
            } else {
                Utils.debugLog(TAG, "delete and download again " + canonicalUrl + " " + apkFilePath);
                Utils.runOffUiThread(apkFilePath::delete);
                DownloaderService.queue(context, canonicalUrl, app, apk);
            }
        });
    }

    public void onDownloadStarted(Uri canonicalUri) {
        // App should currently be in the "PendingDownload" state, so this changes it to "Downloading".
        appUpdateStatusManager.updateApk(canonicalUri.toString(),
                AppUpdateStatusManager.Status.Downloading, getDownloadCancelIntent(canonicalUri));
    }

    public void onDownloadProgress(Uri canonicalUri, App app, Apk apk, long bytesRead, long totalBytes) {
        if (appUpdateStatusManager.get(canonicalUri.toString()) == null) {
            // if our app got killed, we need to re-add the APK here
            appUpdateStatusManager.addApk(app, apk, AppUpdateStatusManager.Status.Downloading,
                    getDownloadCancelIntent(canonicalUri));
        }
        appUpdateStatusManager.updateApkProgress(canonicalUri.toString(), totalBytes, bytesRead);
    }

    public void onDownloadComplete(Uri canonicalUri, File file, App intentApp, Apk intentApk) {
        String canonicalUrl = canonicalUri.toString();
        Uri localApkUri = Uri.fromFile(file);

        Utils.debugLog(TAG, "download completed of " + canonicalUri + " to " + localApkUri);
        appUpdateStatusManager.updateApk(canonicalUrl,
                AppUpdateStatusManager.Status.ReadyToInstall, null);

        App app = appUpdateStatusManager.getApp(canonicalUrl);
        Apk apk = appUpdateStatusManager.getApk(canonicalUrl);
        if (app == null || apk == null) {
            // These may be null if our app was killed and the download job restarted.
            // Then, we can take the objects we saved in the intent which survive app death.
            app = intentApp;
            apk = intentApk;
        }
        if (app != null && apk != null) {
            registerInstallReceiver(canonicalUrl);
            InstallerService.install(context, localApkUri, canonicalUri, app, apk);
        } else {
            Log.e(TAG, "Could not install " + canonicalUrl + " because no app or apk available.");
        }
    }

    public void onDownloadFailed(Uri canonicalUri, String errorMsg) {
        appUpdateStatusManager.setDownloadError(canonicalUri.toString(), errorMsg);
    }

    private PendingIntent getDownloadCancelIntent(Uri canonicalUri) {
        Intent intentObject = new Intent(ACTION_CANCEL);
        intentObject.putExtra(DownloaderService.EXTRA_CANONICAL_URL, canonicalUri.toString());
        return PendingIntent.getBroadcast(context, 0, intentObject,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void getMainObb(final String canonicalUrl, Apk apk) {
        getObb(canonicalUrl, apk.getMainObbUrl(), apk.getMainObbFile(), apk.obbMainFileSha256, apk.repoId);
    }

    private void getPatchObb(final String canonicalUrl, Apk apk) {
        getObb(canonicalUrl, apk.getPatchObbUrl(), apk.getPatchObbFile(), apk.obbPatchFileSha256, apk.repoId);
    }

    /**
     * Check if any OBB files are available, and if so, download and install them. This
     * also deletes any obsolete OBB files, per the spec, since there can be only one
     * "main" and one "patch" OBB installed at a time.
     *
     * @see <a href="https://developer.android.com/google/play/expansion-files.html">APK Expansion Files</a>
     */
    private void getObb(final String canonicalUrl, String obbUrlString,
                        final File obbDestFile, final String hash, final long repoId) {
        if (obbDestFile == null || obbDestFile.exists() || TextUtils.isEmpty(obbUrlString)) {
            return;
        }
        final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (DownloaderService.ACTION_STARTED.equals(action)) {
                    Utils.debugLog(TAG, action + " " + intent);
                } else if (DownloaderService.ACTION_PROGRESS.equals(action)) {

                    long bytesRead = intent.getLongExtra(DownloaderService.EXTRA_BYTES_READ, 0);
                    long totalBytes = intent.getLongExtra(DownloaderService.EXTRA_TOTAL_BYTES, 0);
                    appUpdateStatusManager.updateApkProgress(canonicalUrl, totalBytes, bytesRead);
                } else if (DownloaderService.ACTION_COMPLETE.equals(action)) {
                    localBroadcastManager.unregisterReceiver(this);
                    File localFile = new File(intent.getStringExtra(DownloaderService.EXTRA_DOWNLOAD_PATH));
                    Uri localApkUri = Uri.fromFile(localFile);
                    Utils.debugLog(TAG, "OBB download completed " + intent.getDataString()
                            + " to " + localApkUri);

                    try {
                        if (Utils.isFileMatchingHash(localFile, hash, SHA_256)) {
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
                            Utils.debugLog(TAG, localFile + " deleted, did not match hash: " + hash);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        FileUtils.deleteQuietly(localFile);
                    }
                } else if (DownloaderService.ACTION_INTERRUPTED.equals(action)) {
                    localBroadcastManager.unregisterReceiver(this);
                } else if (DownloaderService.ACTION_CONNECTION_FAILED.equals(action)) {
                    localBroadcastManager.unregisterReceiver(this);
                } else {
                    throw new RuntimeException("intent action not handled!");
                }
            }
        };
        DownloaderService.queue(context, repoId, obbUrlString, obbUrlString);
        localBroadcastManager.registerReceiver(downloadReceiver,
                DownloaderService.getIntentFilter(obbUrlString));
    }

    /**
     * Register a {@link BroadcastReceiver} for tracking install progress for a
     * give {@link Uri}.  There can be multiple of these registered at a time.
     */
    private void registerInstallReceiver(String canonicalUrl) {
        BroadcastReceiver installReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String canonicalUrl = intent.getDataString();
                App app;
                Apk apk;
                switch (intent.getAction()) {
                    case Installer.ACTION_INSTALL_STARTED:
                        appUpdateStatusManager.updateApk(canonicalUrl,
                                AppUpdateStatusManager.Status.Installing, null);
                        break;
                    case Installer.ACTION_INSTALL_COMPLETE:
                        appUpdateStatusManager.updateApk(canonicalUrl,
                                AppUpdateStatusManager.Status.Installed, null);
                        Apk apkComplete = appUpdateStatusManager.getApk(canonicalUrl);

                        if (apkComplete != null && apkComplete.isApk()) {
                            try {
                                PackageManagerCompat.setInstaller(context, context.getPackageManager(), apkComplete.packageName);
                            } catch (SecurityException e) {
                                // Will happen if we fell back to DefaultInstaller for some reason.
                            }
                        }
                        localBroadcastManager.unregisterReceiver(this);
                        break;
                    case Installer.ACTION_INSTALL_INTERRUPTED:
                        app = intent.getParcelableExtra(Installer.EXTRA_APP);
                        apk = intent.getParcelableExtra(Installer.EXTRA_APK);
                        String errorMessage =
                                intent.getStringExtra(Installer.EXTRA_ERROR_MESSAGE);
                        if (!TextUtils.isEmpty(errorMessage)) {
                            appUpdateStatusManager.setApkError(app, apk, errorMessage);
                        } else {
                            appUpdateStatusManager.removeApk(canonicalUrl);
                        }
                        localBroadcastManager.unregisterReceiver(this);
                        break;
                    case Installer.ACTION_INSTALL_USER_INTERACTION:
                        app = intent.getParcelableExtra(Installer.EXTRA_APP);
                        apk = intent.getParcelableExtra(Installer.EXTRA_APK);
                        PendingIntent installPendingIntent = intent.getParcelableExtra(Installer.EXTRA_USER_INTERACTION_PI);
                        appUpdateStatusManager.addApk(app, apk, AppUpdateStatusManager.Status.ReadyToInstall, installPendingIntent);
                        break;
                    default:
                        throw new RuntimeException("intent action not handled!");
                }
            }
        };

        localBroadcastManager.registerReceiver(installReceiver,
                Installer.getInstallIntentFilter(Uri.parse(canonicalUrl)));
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
    public static void queue(Context context, @NonNull App app, @NonNull Apk apk) {
        String canonicalUrl = apk.getCanonicalUrl();
        AppUpdateStatusManager.getInstance(context).addApk(app, apk, AppUpdateStatusManager.Status.PendingInstall, null);
        Utils.debugLog(TAG, "queue " + app.packageName + " " + apk.versionCode + " from " + canonicalUrl);
        InstallManagerService.getInstance(context).queue(canonicalUrl, apk.packageName, app, apk);
    }

    public static void cancel(Context context, String canonicalUrl) {
        InstallManagerService.getInstance(context).onCancel(canonicalUrl);
    }
}
