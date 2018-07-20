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

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.DBHelper;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.data.Schema;
import org.fdroid.fdroid.installer.InstallManagerService;
import org.fdroid.fdroid.net.BluetoothDownloader;
import org.fdroid.fdroid.net.ConnectivityMonitorService;
import org.fdroid.fdroid.views.main.MainActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class UpdateService extends JobIntentService {

    private static final String TAG = "UpdateService";

    public static final String LOCAL_ACTION_STATUS = "status";

    public static final String EXTRA_MESSAGE = "msg";
    public static final String EXTRA_REPO_ERRORS = "repoErrors";
    public static final String EXTRA_STATUS_CODE = "status";
    public static final String EXTRA_MANUAL_UPDATE = "manualUpdate";
    public static final String EXTRA_FORCED_UPDATE = "forcedUpdate";
    public static final String EXTRA_PROGRESS = "progress";

    public static final int STATUS_COMPLETE_WITH_CHANGES = 0;
    public static final int STATUS_COMPLETE_AND_SAME = 1;
    public static final int STATUS_ERROR_GLOBAL = 2;
    public static final int STATUS_ERROR_LOCAL = 3;
    public static final int STATUS_ERROR_LOCAL_SMALL = 4;
    public static final int STATUS_INFO = 5;

    private static final int JOB_ID = 0xfedcba;

    private static final int NOTIFY_ID_UPDATING = 0;

    private static UpdateService updateService;
    private static Handler toastHandler;

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private AppUpdateStatusManager appUpdateStatusManager;

    public static void updateNow(Context context) {
        updateRepoNow(context, null);
    }

    public static void updateRepoNow(Context context, String address) {
        Intent intent = new Intent(context, UpdateService.class);
        intent.putExtra(EXTRA_MANUAL_UPDATE, true);
        if (!TextUtils.isEmpty(address)) {
            intent.setData(Uri.parse(address));
        }
        enqueueWork(context, intent);
    }

    /**
     * For when an automatic process needs to force an index update, like
     * when the system language changes, or the underlying OS was upgraded.
     * This wipes the existing database before running the update!
     */
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
                        && data != Preferences.OVER_NETWORK_NEVER
                        && wifi != Preferences.OVER_NETWORK_NEVER;

        if (Build.VERSION.SDK_INT < 21) {
            Intent intent = new Intent(context, UpdateService.class);
            PendingIntent pending = PendingIntent.getService(context, 0, intent, 0);

            AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarm.cancel(pending);
            if (scheduleNewJob) {
                alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                        SystemClock.elapsedRealtime() + 5000, interval, pending);
                Utils.debugLog(TAG, "Update scheduler alarm set");
            } else {
                Utils.debugLog(TAG, "Update scheduler alarm not set");
            }
        } else {
            Utils.debugLog(TAG, "Using android-21 JobScheduler for updates");
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
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

    private static volatile boolean isScheduleIfStillOnWifiRunning;

    /**
     * Waits for a period of time for the WiFi to settle, then if the WiFi is
     * still active, it schedules an update.  This is to encourage the use of
     * unlimited networks over metered networks for index updates and auto
     * downloads of app updates. Starting with {@code android-21}, this uses
     * {@link android.app.job.JobScheduler} instead.
     */
    public static void scheduleIfStillOnWifi(Context context) {
        if (Build.VERSION.SDK_INT >= 21) {
            throw new IllegalStateException("This should never be used on android-21 or newer!");
        }
        if (isScheduleIfStillOnWifiRunning || !Preferences.get().isBackgroundDownloadAllowed()) {
            return;
        }
        isScheduleIfStillOnWifiRunning = true;
        new StillOnWifiAsyncTask(context).execute();
    }

    private static final class StillOnWifiAsyncTask extends AsyncTask<Void, Void, Void> {

        private final WeakReference<Context> contextWeakReference;

        private StillOnWifiAsyncTask(Context context) {
            this.contextWeakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Context context = contextWeakReference.get();
            try {
                Thread.sleep(120000);
                if (Preferences.get().isBackgroundDownloadAllowed()) {
                    Utils.debugLog(TAG, "scheduling update because there is good internet");
                    schedule(context);
                }
            } catch (Exception e) {
                Utils.debugLog(TAG, e.getMessage());
            }
            isScheduleIfStillOnWifiRunning = false;
            return null;
        }

    }

    public static void stopNow(Context context) {
        if (updateService != null) {
            updateService.stopSelf(JOB_ID);
            updateService = null;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        updateService = this;

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_refresh_white)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentTitle(getString(R.string.update_notification_title));
        appUpdateStatusManager = AppUpdateStatusManager.getInstance(this);

        // Android docs are a little sketchy, however it seems that Gingerbread is the last
        // sdk that made a content intent mandatory:
        //
        //   http://stackoverflow.com/a/20032920
        //
        if (Build.VERSION.SDK_INT <= 10) {
            Intent pendingIntent = new Intent(this, MainActivity.class);
            pendingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            notificationBuilder.setContentIntent(
                    PendingIntent.getActivity(this, 0, pendingIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        notificationManager.cancel(NOTIFY_ID_UPDATING);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateStatusReceiver);
        updateService = null;
    }

    public static void sendStatus(Context context, int statusCode) {
        sendStatus(context, statusCode, null, -1);
    }

    public static void sendStatus(Context context, int statusCode, String message) {
        sendStatus(context, statusCode, message, -1);
    }

    public static void sendStatus(Context context, int statusCode, String message, int progress) {
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
        intent.putExtra(EXTRA_REPO_ERRORS, repoErrors.toArray(new CharSequence[repoErrors.size()]));
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

    /**
     * In order to send a {@link Toast} from a {@link IntentService}, we have to do these tricks.
     */
    private void sendNoInternetToast() {
        if (toastHandler == null) {
            toastHandler = new Handler(Looper.getMainLooper());
        }
        toastHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),
                        R.string.warning_no_internet, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

        final long startTime = System.currentTimeMillis();
        boolean manualUpdate = intent.getBooleanExtra(EXTRA_MANUAL_UPDATE, false);
        boolean forcedUpdate = intent.getBooleanExtra(EXTRA_FORCED_UPDATE, false);
        String address = intent.getDataString();

        try {
            final Preferences fdroidPrefs = Preferences.get();
            // See if it's time to actually do anything yet...
            int netState = ConnectivityMonitorService.getNetworkState(this);
            if (address != null && address.startsWith(BluetoothDownloader.SCHEME)) {
                Utils.debugLog(TAG, "skipping internet check, this is bluetooth");
            } else if (netState == ConnectivityMonitorService.FLAG_NET_UNAVAILABLE) {
                Utils.debugLog(TAG, "No internet, cannot update");
                if (manualUpdate) {
                    sendNoInternetToast();
                }
                return;
            } else if ((manualUpdate || forcedUpdate) && fdroidPrefs.isOnDemandDownloadAllowed()) {
                Utils.debugLog(TAG, "manually requested or forced update");
            } else if (!fdroidPrefs.isBackgroundDownloadAllowed() && !fdroidPrefs.isOnDemandDownloadAllowed()) {
                Utils.debugLog(TAG, "don't run update");
                return;
            }

            setNotification();
            LocalBroadcastManager.getInstance(this).registerReceiver(updateStatusReceiver,
                    new IntentFilter(LOCAL_ACTION_STATUS));

            // Grab some preliminary information, then we can release the
            // database while we do all the downloading, etc...
            List<Repo> repos = RepoProvider.Helper.all(this);

            int unchangedRepos = 0;
            int updatedRepos = 0;
            int errorRepos = 0;
            ArrayList<CharSequence> repoErrors = new ArrayList<>();
            boolean changes = false;
            boolean singleRepoUpdate = !TextUtils.isEmpty(address);
            for (final Repo repo : repos) {
                if (!repo.inuse) {
                    continue;
                }
                if (singleRepoUpdate && !repo.address.equals(address)) {
                    unchangedRepos++;
                    continue;
                }
                if (!singleRepoUpdate && repo.isSwap) {
                    continue;
                }

                sendStatus(this, STATUS_INFO, getString(R.string.status_connecting_to_repo, repo.address));

                if (forcedUpdate) {
                    DBHelper.resetTransient(this);
                }

                try {
                    RepoUpdater updater = new IndexV1Updater(this, repo);
                    if (Preferences.get().isForceOldIndexEnabled() || !updater.update()) {
                        updater = new RepoUpdater(getBaseContext(), repo);
                        updater.update();
                    }

                    if (updater.hasChanged()) {
                        updatedRepos++;
                        changes = true;
                    } else {
                        unchangedRepos++;
                    }
                } catch (RepoUpdater.UpdateException e) {
                    errorRepos++;
                    repoErrors.add(e.getMessage());
                    Log.e(TAG, "Error updating repository " + repo.address, e);
                }

                // now that downloading the index is done, start downloading updates
                if (changes && fdroidPrefs.isAutoDownloadEnabled() && fdroidPrefs.isBackgroundDownloadAllowed()) {
                    autoDownloadUpdates(this);
                }
            }

            if (!changes) {
                Utils.debugLog(TAG, "Not checking app details or compatibility, because repos were up to date.");
            } else {
                notifyContentProviders();

                if (fdroidPrefs.isUpdateNotificationEnabled() && !fdroidPrefs.isAutoDownloadEnabled()) {
                    performUpdateNotification();
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

        long time = System.currentTimeMillis() - startTime;
        Log.i(TAG, "Updating repo(s) complete, took " + time / 1000 + " seconds to complete.");
    }

    private void notifyContentProviders() {
        getContentResolver().notifyChange(AppProvider.getContentUri(), null);
        getContentResolver().notifyChange(ApkProvider.getContentUri(), null);
    }

    private void performUpdateNotification() {
        List<App> canUpdate = AppProvider.Helper.findCanUpdate(this, Schema.AppMetadataTable.Cols.ALL);
        if (canUpdate.size() > 0) {
            showAppUpdatesNotification(canUpdate);
        }
    }

    public static void autoDownloadUpdates(Context context) {
        List<App> canUpdate = AppProvider.Helper.findCanUpdate(context, Schema.AppMetadataTable.Cols.ALL);
        for (App app : canUpdate) {
            Apk apk = ApkProvider.Helper.findSuggestedApk(context, app);
            InstallManagerService.queue(context, app, apk);
        }
    }

    private void showAppUpdatesNotification(List<App> canUpdate) {
        if (canUpdate.size() > 0) {
            List<Apk> apksToUpdate = new ArrayList<>(canUpdate.size());
            for (App app : canUpdate) {
                apksToUpdate.add(ApkProvider.Helper.findSuggestedApk(this, app));
            }
            appUpdateStatusManager.addApks(apksToUpdate, AppUpdateStatusManager.Status.UpdateAvailable);
        }
    }

    public static void reportDownloadProgress(Context context, RepoUpdater updater,
                                              long bytesRead, long totalBytes) {
        Utils.debugLog(TAG, "Downloading " + updater.indexUrl + "(" + bytesRead + "/" + totalBytes + ")");
        String downloadedSizeFriendly = Utils.getFriendlySize(bytesRead);
        int percent = -1;
        if (totalBytes > 0) {
            percent = Utils.getPercent(bytesRead, totalBytes);
        }
        String message;
        if (totalBytes == -1) {
            message = context.getString(R.string.status_download_unknown_size,
                    updater.indexUrl, downloadedSizeFriendly);
            percent = -1;
        } else {
            String totalSizeFriendly = Utils.getFriendlySize(totalBytes);
            message = context.getString(R.string.status_download,
                    updater.indexUrl, downloadedSizeFriendly, totalSizeFriendly, percent);
        }
        sendStatus(context, STATUS_INFO, message, percent);
    }

    public static void reportProcessIndexProgress(Context context, RepoUpdater updater,
                                                  long bytesRead, long totalBytes) {
        Utils.debugLog(TAG, "Processing " + updater.indexUrl + "(" + bytesRead + "/" + totalBytes + ")");
        String downloadedSize = Utils.getFriendlySize(bytesRead);
        String totalSize = Utils.getFriendlySize(totalBytes);
        int percent = -1;
        if (totalBytes > 0) {
            percent = Utils.getPercent(bytesRead, totalBytes);
        }
        String message = context.getString(R.string.status_processing_xml_percent,
                updater.indexUrl, downloadedSize, totalSize, percent);
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
    public static void reportProcessingAppsProgress(Context context, RepoUpdater updater,
                                                    int appsSaved, int totalApps) {
        Utils.debugLog(TAG, "Committing " + updater.indexUrl + "(" + appsSaved + "/" + totalApps + ")");
        if (totalApps > 0) {
            String message = context.getString(R.string.status_inserting_x_apps,
                    appsSaved, totalApps, updater.indexUrl);
            sendStatus(context, STATUS_INFO, message, Utils.getPercent(appsSaved, totalApps));
        } else {
            String message = context.getString(R.string.status_inserting_apps);
            sendStatus(context, STATUS_INFO, message);
        }
    }
}
