/*
 * Copyright (C) 2010-12  Ciaran Gultnieks, ciaran@ciarang.com
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.fdroid.fdroid;

import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.fdroid.CompatibilityChecker;
import org.fdroid.CompatibilityCheckerImpl;
import org.fdroid.database.DbUpdateChecker;
import org.fdroid.database.FDroidDatabase;
import org.fdroid.database.Repository;
import org.fdroid.database.UpdatableApp;
import org.fdroid.download.Mirror;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.DBHelper;
import org.fdroid.fdroid.installer.InstallManagerService;
import org.fdroid.fdroid.net.BluetoothDownloader;
import org.fdroid.fdroid.net.ConnectivityMonitorService;
import org.fdroid.fdroid.net.DownloaderFactory;
import org.fdroid.index.IndexUpdateResult;
import org.fdroid.index.RepoUpdater;
import org.fdroid.index.RepoUriBuilder;
import org.fdroid.index.TempFileProvider;
import org.fdroid.index.v1.IndexV1Updater;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class UpdateService extends JobIntentService {

    private static final String TAG = "UpdateService";

    public static final String LOCAL_ACTION_STATUS = "status";

    private static final String EXTRA_MESSAGE = "msg";
    private static final String EXTRA_REPO_FINGERPRINT = "fingerprint";
    private static final String EXTRA_REPO_ERRORS = "repoErrors";
    public static final String EXTRA_STATUS_CODE = "status";
    private static final String EXTRA_MANUAL_UPDATE = "manualUpdate";
    private static final String EXTRA_FORCED_UPDATE = "forcedUpdate";
    private static final String EXTRA_PROGRESS = "progress";

    public static final int STATUS_COMPLETE_WITH_CHANGES = 0;
    private static final int STATUS_COMPLETE_AND_SAME = 1;
    private static final int STATUS_ERROR_GLOBAL = 2;
    private static final int STATUS_ERROR_LOCAL = 3;
    private static final int STATUS_ERROR_LOCAL_SMALL = 4;
    public static final int STATUS_INFO = 5;

    /**
     * This number should never change, it is used by ROMs to trigger
     * the first background update of F-Droid during setup.
     *
     * @see <a href="https://gitlab.com/fdroid/fdroidclient/-/issues/2147">Add a way to trigger an index update externally</a>
     * @see <a href="https://review.calyxos.org/c/CalyxOS/platform_packages_apps_SetupWizard/+/3461"/>Schedule F-Droid index update on initialization and network connection</a>
     */
    private static final int JOB_ID = 0xfedcba;

    private static final int NOTIFY_ID_UPDATING = 0;

    private static UpdateService updateService;
    private static boolean isForcedUpdate;

    private FDroidDatabase db;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private AppUpdateStatusManager appUpdateStatusManager;

    public static void updateNow(Context context) {
        updateRepoNow(context, null);
    }

    public static void updateRepoNow(Context context, String address) {
        updateNewRepoNow(context, address, null);
    }

    public static Intent getIntent(Context context, String address, @Nullable String fingerprint) {
        Intent intent = new Intent(context, UpdateService.class);
        intent.putExtra(EXTRA_MANUAL_UPDATE, true);
        intent.putExtra(EXTRA_REPO_FINGERPRINT, fingerprint);
        if (!TextUtils.isEmpty(address)) {
            intent.setData(Uri.parse(address));
        }
        return intent;
    }

    @UiThread
    public static void updateNewRepoNow(Context context, String address, @Nullable String fingerprint) {
        enqueueWork(context, getIntent(context, address, fingerprint));
    }

    /**
     * For when an automatic process needs to force an index update, like
     * when the system language changes, or the underlying OS was upgraded.
     * This wipes the existing database before running the update!
     */
    @UiThread
    public static void forceUpdateRepo(Context context) {
        Intent intent = new Intent(context, UpdateService.class);
        intent.putExtra(EXTRA_FORCED_UPDATE, true);
        enqueueWork(context, intent);
    }

    /**
     * Add work to the queue for processing now.
     * <p>
     * This also shows a {@link Toast} if the Data/WiFi Settings make it so the
     * update process is not allowed to run and the device is attached to a
     * network (e.g. is not offline or in Airplane Mode).
     *
     * @see JobIntentService#enqueueWork(Context, Class, int, Intent)
     */
    @UiThread
    private static void enqueueWork(Context context, @NonNull Intent intent) {
        if (FDroidApp.networkState > 0 && !Preferences.get().isOnDemandDownloadAllowed()) {
            Toast.makeText(context, R.string.updates_disabled_by_settings, Toast.LENGTH_LONG).show();
        }

        enqueueWork(context, UpdateService.class, JOB_ID, intent);
    }

    /**
     * Schedule this service to update the app index while canceling any previously
     * scheduled updates, according to the current preferences. Should be called
     * a) at boot, b) if the preference is changed, or c) on startup, in case we get
     * upgraded. It works differently on {@code android-21} and newer, versus older,
     * due to the {@link JobScheduler} API handling it very nicely for us.
     *
     * @see <a href="https://developer.android.com/about/versions/android-5.0.html#Power">Project Volta: Scheduling jobs</a>
     */
    public static void schedule(Context context) {
        Preferences prefs = Preferences.get();
        long interval = prefs.getUpdateInterval();
        int data = prefs.getOverData();
        int wifi = prefs.getOverWifi();
        boolean scheduleNewJob =
                interval != Preferences.UPDATE_INTERVAL_DISABLED
                        && !(data == Preferences.OVER_NETWORK_NEVER && wifi == Preferences.OVER_NETWORK_NEVER);

        Utils.debugLog(TAG, "Using android-21 JobScheduler for updates");
        JobScheduler jobScheduler = ContextCompat.getSystemService(context, JobScheduler.class);
        ComponentName componentName = new ComponentName(context, UpdateJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, componentName)
                .setRequiresDeviceIdle(true)
                .setPeriodic(interval);
        if (Build.VERSION.SDK_INT >= 26) {
            builder.setRequiresBatteryNotLow(true)
                    .setRequiresStorageNotLow(true);
        }
        if (data == Preferences.OVER_NETWORK_ALWAYS && wifi == Preferences.OVER_NETWORK_ALWAYS) {
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        } else {
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);
        }

        jobScheduler.cancel(JOB_ID);
        if (scheduleNewJob) {
            jobScheduler.schedule(builder.build());
            Utils.debugLog(TAG, "Update scheduler alarm set");
        } else {
            Utils.debugLog(TAG, "Update scheduler alarm not set");
        }
    }

    /**
     * Whether or not a repo update is currently in progress. Used to show feedback throughout
     * the app to users, so they know something is happening.
     *
     * @see <a href="https://stackoverflow.com/a/608600">set a global variable when it is running that your client can check</a>
     */
    public static boolean isUpdating() {
        return updateService != null;
    }

    /**
     * Whether or not a forced repo update is currently in progress.
     * This is typically the case when the phone was updated to a new major OS version
     * or the client DB was updated, so that all data needed to be purged.
     */
    public static boolean isUpdatingForced() {
        return updateService != null && isForcedUpdate;
    }

    static void stopNow() {
        if (updateService != null) {
            updateService.stopSelf(JOB_ID);
            updateService = null;
        }
    }

    /**
     * Return the repos in the {@code repos} {@link List} that have either a
     * local canonical URL or a local mirror URL.  These are repos that can be
     * updated and used without using the Internet.
     */
    public static List<Repository> getLocalRepos(List<Repository> repos) {
        ArrayList<Repository> localRepos = new ArrayList<>();
        for (Repository repo : repos) {
            if (isLocalRepoAddress(repo.getAddress())) {
                localRepos.add(repo);
            } else {
                for (Mirror mirror : repo.getMirrors()) {
                    if (!mirror.isHttp()) {
                        localRepos.add(repo);
                        break;
                    }
                }
            }
        }
        return localRepos;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        updateService = this;
        db = DBHelper.getDb(getApplicationContext());

        notificationManager = ContextCompat.getSystemService(this, NotificationManager.class);

        notificationBuilder = new NotificationCompat.Builder(this, NotificationHelper.CHANNEL_UPDATES)
                .setSmallIcon(R.drawable.ic_refresh)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentTitle(getString(R.string.banner_updating_repositories));
        appUpdateStatusManager = AppUpdateStatusManager.getInstance(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        notificationManager.cancel(NOTIFY_ID_UPDATING);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateStatusReceiver);
        isForcedUpdate = false;
        updateService = null;
    }

    private static void sendStatus(Context context, int statusCode) {
        sendStatus(context, statusCode, null, -1);
    }

    private static void sendStatus(Context context, int statusCode, String message) {
        sendStatus(context, statusCode, message, -1);
    }

    private static void sendStatus(Context context, int statusCode, String message, int progress) {
        Intent intent = new Intent(LOCAL_ACTION_STATUS);
        intent.putExtra(EXTRA_STATUS_CODE, statusCode);
        if (!TextUtils.isEmpty(message)) {
            intent.putExtra(EXTRA_MESSAGE, message);
        }
        intent.putExtra(EXTRA_PROGRESS, progress);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void sendRepoErrorStatus(int statusCode, ArrayList<CharSequence> repoErrors) {
        Intent intent = new Intent(LOCAL_ACTION_STATUS);
        intent.putExtra(EXTRA_STATUS_CODE, statusCode);
        intent.putExtra(EXTRA_REPO_ERRORS, repoErrors.toArray(new CharSequence[0]));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // For receiving results from the UpdateService when we've told it to
    // update in response to a user request.
    private final BroadcastReceiver updateStatusReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.isEmpty(action)) {
                return;
            }

            if (!action.equals(LOCAL_ACTION_STATUS)) {
                return;
            }

            final String message = intent.getStringExtra(EXTRA_MESSAGE);
            int resultCode = intent.getIntExtra(EXTRA_STATUS_CODE, -1);
            int progress = intent.getIntExtra(EXTRA_PROGRESS, -1);

            String text;
            switch (resultCode) {
                case STATUS_INFO:
                    notificationBuilder.setContentText(message)
                            .setCategory(NotificationCompat.CATEGORY_SERVICE);
                    if (progress > -1) {
                        notificationBuilder.setProgress(100, progress, false);
                    } else {
                        notificationBuilder.setProgress(100, 0, true);
                    }
                    setNotification();
                    break;
                case STATUS_ERROR_GLOBAL:
                    text = context.getString(R.string.global_error_updating_repos, message);
                    notificationBuilder.setContentText(text)
                            .setCategory(NotificationCompat.CATEGORY_ERROR)
                            .setSmallIcon(android.R.drawable.ic_dialog_alert);
                    setNotification();
                    Toast.makeText(context, text, Toast.LENGTH_LONG).show();
                    break;
                case STATUS_ERROR_LOCAL:
                case STATUS_ERROR_LOCAL_SMALL:
                    StringBuilder msgBuilder = new StringBuilder();
                    CharSequence[] repoErrors = intent.getCharSequenceArrayExtra(EXTRA_REPO_ERRORS);
                    for (CharSequence error : repoErrors) {
                        if (msgBuilder.length() > 0) msgBuilder.append('\n');
                        msgBuilder.append(error);
                    }
                    if (resultCode == STATUS_ERROR_LOCAL_SMALL) {
                        msgBuilder.append('\n').append(context.getString(R.string.all_other_repos_fine));
                    }
                    text = msgBuilder.toString();
                    notificationBuilder.setContentText(text)
                            .setCategory(NotificationCompat.CATEGORY_ERROR)
                            .setSmallIcon(android.R.drawable.ic_dialog_info);
                    setNotification();
                    Toast.makeText(context, text, Toast.LENGTH_LONG).show();
                    break;
                case STATUS_COMPLETE_WITH_CHANGES:
                    break;
                case STATUS_COMPLETE_AND_SAME:
                    text = context.getString(R.string.repos_unchanged);
                    notificationBuilder.setContentText(text)
                            .setCategory(NotificationCompat.CATEGORY_SERVICE);
                    setNotification();
                    break;
            }
        }
    };

    private void setNotification() {
        if (Preferences.get().isUpdateNotificationEnabled()) {
            notificationManager.notify(NOTIFY_ID_UPDATING, notificationBuilder.build());
        }
    }

    private static boolean isLocalRepoAddress(String address) {
        return address != null &&
                (address.startsWith(BluetoothDownloader.SCHEME)
                        || address.startsWith(ContentResolver.SCHEME_CONTENT)
                        || address.startsWith(ContentResolver.SCHEME_FILE));
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

        final long startTime = System.currentTimeMillis();
        boolean manualUpdate = intent.getBooleanExtra(EXTRA_MANUAL_UPDATE, false);
        boolean forcedUpdate = intent.getBooleanExtra(EXTRA_FORCED_UPDATE, false);
        isForcedUpdate = forcedUpdate;
        String fingerprint = intent.getStringExtra(EXTRA_REPO_FINGERPRINT);
        String address = intent.getDataString();

        try {
            final Preferences fdroidPrefs = Preferences.get();
            // always get repos fresh from DB, because
            // * when an update is requested early at app start, the repos above might not be available, yet
            // * when an update is requested when adding a new repo, it might not be in the FDroidApp list, yet
            List<Repository> repos = db.getRepositoryDao().getRepositories();

            // See if it's time to actually do anything yet...
            int netState = ConnectivityMonitorService.getNetworkState(this);
            if (isLocalRepoAddress(address)) {
                Utils.debugLog(TAG, "skipping internet check, this is local: " + address);
            } else if (netState == ConnectivityMonitorService.FLAG_NET_UNAVAILABLE) {
                // keep track of repos that have a local copy in case internet is not available
                List<Repository> localRepos = getLocalRepos(repos);
                if (localRepos.size() > 0) {
                    repos = localRepos;
                } else {
                    Utils.debugLog(TAG, "No internet, cannot update");
                    if (manualUpdate) {
                        Utils.showToastFromService(this, getString(R.string.warning_no_internet), Toast.LENGTH_SHORT);
                    }
                    isForcedUpdate = false;
                    return;
                }
            } else if ((manualUpdate || forcedUpdate) && fdroidPrefs.isOnDemandDownloadAllowed()) {
                Utils.debugLog(TAG, "manually requested or forced update");
                if (forcedUpdate) DBHelper.resetTransient(this);
            } else if (!fdroidPrefs.isBackgroundDownloadAllowed() && !fdroidPrefs.isOnDemandDownloadAllowed()) {
                Utils.debugLog(TAG, "don't run update");
                isForcedUpdate = false;
                return;
            }

            setNotification();
            LocalBroadcastManager.getInstance(this).registerReceiver(updateStatusReceiver,
                    new IntentFilter(LOCAL_ACTION_STATUS));

            int unchangedRepos = 0;
            int updatedRepos = 0;
            int errorRepos = 0;
            ArrayList<CharSequence> repoErrors = new ArrayList<>();
            boolean changes = false;
            boolean singleRepoUpdate = !TextUtils.isEmpty(address);
            for (final Repository repo : repos) {
                if (!repo.getEnabled()) continue;
                if (singleRepoUpdate && !repo.getAddress().equals(address)) {
                    unchangedRepos++;
                    continue;
                }

                sendStatus(this, STATUS_INFO, getString(R.string.status_connecting_to_repo, repo.getAddress()));

                final RepoUriBuilder repoUriBuilder = (repository, pathElements) -> {
                    String address1 = Utils.getRepoAddress(repository);
                    return Utils.getUri(address1, pathElements);
                };
                final CompatibilityChecker compatChecker =
                        new CompatibilityCheckerImpl(getPackageManager(), Preferences.get().forceTouchApps());
                final UpdateServiceListener listener = new UpdateServiceListener(UpdateService.this);
                final File cacheDir = getApplicationContext().getCacheDir();
                final IndexUpdateResult result;
                if (Preferences.get().isForceOldIndexEnabled()) {
                    final TempFileProvider tempFileProvider = () ->
                            File.createTempFile("dl-", "", cacheDir);
                    final IndexV1Updater updater = new IndexV1Updater(db, tempFileProvider,
                            DownloaderFactory.INSTANCE, repoUriBuilder, compatChecker, listener);
                    result = updater.updateNewRepo(repo, fingerprint);
                } else {
                    final RepoUpdater updater = new RepoUpdater(cacheDir, db,
                            DownloaderFactory.INSTANCE, repoUriBuilder, compatChecker, listener);
                    result = updater.update(repo, fingerprint);
                }
                if (result instanceof IndexUpdateResult.Unchanged) {
                    unchangedRepos++;
                } else if (result instanceof IndexUpdateResult.Processed) {
                    updatedRepos++;
                    changes = true;
                } else if (result instanceof IndexUpdateResult.Error) {
                    errorRepos++;
                    Exception e = ((IndexUpdateResult.Error) result).getE();
                    Throwable cause = e.getCause();
                    String repoName = repo.getName(App.getLocales());
                    String repoPrefix = repoName == null ? "" : repoName + ": ";
                    if (cause == null) {
                        repoErrors.add(repoPrefix + e.getLocalizedMessage());
                    } else {
                        repoErrors.add(repoPrefix + e.getLocalizedMessage() + " ⇨ " +
                                cause.getLocalizedMessage());
                    }
                    Log.e(TAG, "Error updating repository " + repo.getAddress(), e);
                }
            }

            if (changes) {
                appUpdateStatusManager.checkForUpdates();
                // now that downloading the index is done, start downloading updates
                if (fdroidPrefs.isAutoDownloadEnabled() && fdroidPrefs.isBackgroundDownloadAllowed()) {
                    // this is using DbUpdateChecker#getUpdatableApps() again which isn't optimal
                    autoDownloadUpdates(this);
                }
            }

            fdroidPrefs.setLastUpdateCheck(System.currentTimeMillis());

            if (errorRepos == 0) {
                if (changes) {
                    sendStatus(this, STATUS_COMPLETE_WITH_CHANGES);
                } else {
                    sendStatus(this, STATUS_COMPLETE_AND_SAME);
                }
            } else {
                if (updatedRepos + unchangedRepos == 0) {
                    sendRepoErrorStatus(STATUS_ERROR_LOCAL, repoErrors);
                } else {
                    sendRepoErrorStatus(STATUS_ERROR_LOCAL_SMALL, repoErrors);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception during update processing", e);
            sendStatus(this, STATUS_ERROR_GLOBAL, e.getMessage());
        }

        isForcedUpdate = false;
        long time = System.currentTimeMillis() - startTime;
        Log.i(TAG, "Updating repo(s) complete, took " + time / 1000 + " seconds to complete.");
    }

    /**
     * Queues all apps needing update.  If this app itself (e.g. F-Droid) needs
     * to be updated, it is queued last.
     */
    public static Disposable autoDownloadUpdates(Context context) {
        DbUpdateChecker updateChecker = new DbUpdateChecker(DBHelper.getDb(context), context.getPackageManager());
        List<String> releaseChannels = Preferences.get().getBackendReleaseChannels();
        return Single.fromCallable(() -> updateChecker.getUpdatableApps(releaseChannels))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(throwable -> Log.e(TAG, "Error auto-downloading updates: ", throwable))
                .subscribe(updatableApps -> downloadUpdates(context, updatableApps));
    }

    private static void downloadUpdates(Context context, List<UpdatableApp> apps) {
        String ourPackageName = context.getPackageName();
        App updateLastApp = null;
        Apk updateLastApk = null;
        for (UpdatableApp app : apps) {
            Repository repo = FDroidApp.getRepoManager(context).getRepository(app.getUpdate().getRepoId());
            if (repo == null) continue; // repo could have been removed in the meantime
            // update our own APK at the end
            if (TextUtils.equals(ourPackageName, app.getUpdate().getPackageName())) {
                updateLastApp = new App(app);
                updateLastApk = new Apk(app.getUpdate(), repo);
                continue;
            }
            InstallManagerService.queue(context, new App(app), new Apk(app.getUpdate(), repo));
        }
        if (updateLastApp != null) {
            InstallManagerService.queue(context, updateLastApp, updateLastApk);
        }
    }

    static void reportDownloadProgress(Context context, String indexUrl,
                                       long bytesRead, long totalBytes) {
        Utils.debugLog(TAG, "Downloading " + indexUrl + "(" + bytesRead + "/" + totalBytes + ")");
        String downloadedSizeFriendly = Utils.getFriendlySize(bytesRead);
        int percent = -1;
        if (totalBytes > 0) {
            percent = Utils.getPercent(bytesRead, totalBytes);
        }
        String message;
        if (totalBytes == -1) {
            message = context.getString(R.string.status_download_unknown_size,
                    indexUrl, downloadedSizeFriendly);
        } else {
            String totalSizeFriendly = Utils.getFriendlySize(totalBytes);
            message = context.getString(R.string.status_download,
                    indexUrl, downloadedSizeFriendly, totalSizeFriendly, percent);
        }
        sendStatus(context, STATUS_INFO, message, percent);
    }

    /**
     * If an updater is unable to know how many apps it has to process (i.e. it
     * is streaming apps to the database or performing a large database query
     * which touches all apps, but is unable to report progress), then it call
     * this listener with `totalBytes = 0`. Doing so will result in a message of
     * "Saving app details" sent to the user. If you know how many apps you have
     * processed, then a message of "Saving app details (x/total)" is displayed.
     */
    static void reportProcessingAppsProgress(Context context, String indexUrl, int appsSaved, int totalApps) {
        Utils.debugLog(TAG, "Committing " + indexUrl + "(" + appsSaved + "/" + totalApps + ")");
        if (totalApps > 0) {
            String message = context.getString(R.string.status_inserting_x_apps,
                    appsSaved, totalApps, indexUrl);
            sendStatus(context, STATUS_INFO, message, Utils.getPercent(appsSaved, totalApps));
        } else {
            String message = context.getString(R.string.status_inserting_apps);
            sendStatus(context, STATUS_INFO, message);
        }
    }
}
