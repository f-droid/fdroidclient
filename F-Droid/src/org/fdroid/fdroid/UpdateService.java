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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.net.Downloader;

import java.util.ArrayList;
import java.util.List;

public class UpdateService extends IntentService implements ProgressListener {

    private static final String TAG = "UpdateService";

    public static final String LOCAL_ACTION_STATUS = "status";

    public static final String EXTRA_MESSAGE = "msg";
    public static final String EXTRA_REPO_ERRORS = "repoErrors";
    public static final String EXTRA_STATUS_CODE = "status";
    public static final String EXTRA_ADDRESS = "address";
    public static final String EXTRA_MANUAL_UPDATE = "manualUpdate";
    public static final String EXTRA_PROGRESS = "progress";

    public static final int STATUS_COMPLETE_WITH_CHANGES = 0;
    public static final int STATUS_COMPLETE_AND_SAME = 1;
    public static final int STATUS_ERROR_GLOBAL = 2;
    public static final int STATUS_ERROR_LOCAL = 3;
    public static final int STATUS_ERROR_LOCAL_SMALL = 4;
    public static final int STATUS_INFO = 5;

    private LocalBroadcastManager localBroadcastManager;

    private static final int NOTIFY_ID_UPDATING = 0;
    private static final int NOTIFY_ID_UPDATES_AVAILABLE = 1;

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    public UpdateService() {
        super("UpdateService");
    }

    public static void updateNow(Context context) {
        updateRepoNow(null, context);
    }

    public static void updateRepoNow(String address, Context context) {
        Intent intent = new Intent(context, UpdateService.class);
        intent.putExtra(EXTRA_MANUAL_UPDATE, true);
        if (!TextUtils.isEmpty(address)) {
            intent.putExtra(EXTRA_ADDRESS, address);
        }
        context.startService(intent);
    }

    // Schedule (or cancel schedule for) this service, according to the
    // current preferences. Should be called a) at boot, b) if the preference
    // is changed, or c) on startup, in case we get upgraded.
    public static void schedule(Context ctx) {

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(ctx);
        String sint = prefs.getString(Preferences.PREF_UPD_INTERVAL, "0");
        int interval = Integer.parseInt(sint);

        Intent intent = new Intent(ctx, UpdateService.class);
        PendingIntent pending = PendingIntent.getService(ctx, 0, intent, 0);

        AlarmManager alarm = (AlarmManager) ctx
                .getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pending);
        if (interval > 0) {
            alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + 5000,
                    AlarmManager.INTERVAL_HOUR, pending);
            Utils.debugLog(TAG, "Update scheduler alarm set");
        } else {
            Utils.debugLog(TAG, "Update scheduler alarm not set");
        }

    }

    @Override
    public void onCreate() {
        super.onCreate();

        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(downloadProgressReceiver,
                new IntentFilter(Downloader.LOCAL_ACTION_PROGRESS));
        localBroadcastManager.registerReceiver(updateStatusReceiver,
                new IntentFilter(LOCAL_ACTION_STATUS));

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_refresh_white)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentTitle(getString(R.string.update_notification_title));

        // Android docs are a little sketchy, however it seems that Gingerbread is the last
        // sdk that made a content intent mandatory:
        //
        //   http://stackoverflow.com/a/20032920
        //
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
            Intent pendingIntent = new Intent(this, FDroid.class);
            pendingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            notificationBuilder.setContentIntent(PendingIntent.getActivity(this, 0, pendingIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        }

        notificationManager.notify(NOTIFY_ID_UPDATING, notificationBuilder.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        notificationManager.cancel(NOTIFY_ID_UPDATING);
        localBroadcastManager.unregisterReceiver(downloadProgressReceiver);
        localBroadcastManager.unregisterReceiver(updateStatusReceiver);
    }

    protected static void sendStatus(Context context, int statusCode) {
        sendStatus(context, statusCode, null, -1);
    }

    protected static void sendStatus(Context context, int statusCode, String message) {
        sendStatus(context, statusCode, message, -1);
    }

    protected static void sendStatus(Context context, int statusCode, String message, int progress) {
        Intent intent = new Intent(LOCAL_ACTION_STATUS);
        intent.putExtra(EXTRA_STATUS_CODE, statusCode);
        if (!TextUtils.isEmpty(message))
            intent.putExtra(EXTRA_MESSAGE, message);
        intent.putExtra(EXTRA_PROGRESS, progress);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    protected void sendRepoErrorStatus(int statusCode, ArrayList<CharSequence> repoErrors) {
        Intent intent = new Intent(LOCAL_ACTION_STATUS);
        intent.putExtra(EXTRA_STATUS_CODE, statusCode);
        intent.putExtra(EXTRA_REPO_ERRORS, repoErrors.toArray(new CharSequence[repoErrors.size()]));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private final BroadcastReceiver downloadProgressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.isEmpty(action))
                return;

            if (!action.equals(Downloader.LOCAL_ACTION_PROGRESS))
                return;

            String repoAddress = intent.getStringExtra(Downloader.EXTRA_ADDRESS);
            int downloadedSize = intent.getIntExtra(Downloader.EXTRA_BYTES_READ, -1);
            String downloadedSizeFriendly = Utils.getFriendlySize(downloadedSize);
            int totalSize = intent.getIntExtra(Downloader.EXTRA_TOTAL_BYTES, -1);
            int percent = (int) ((double) downloadedSize / totalSize * 100);
            String message;
            if (totalSize == -1) {
                message = getString(R.string.status_download_unknown_size, repoAddress, downloadedSizeFriendly);
                percent = -1;
            } else {
                String totalSizeFriendly = Utils.getFriendlySize(totalSize);
                message = getString(R.string.status_download, repoAddress, downloadedSizeFriendly, totalSizeFriendly, percent);
            }
            sendStatus(context, STATUS_INFO, message, percent);
        }
    };

    // For receiving results from the UpdateService when we've told it to
    // update in response to a user request.
    private final BroadcastReceiver updateStatusReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.isEmpty(action))
                return;

            if (!action.equals(LOCAL_ACTION_STATUS))
                return;

            final String message = intent.getStringExtra(EXTRA_MESSAGE);
            int resultCode = intent.getIntExtra(EXTRA_STATUS_CODE, -1);
            int progress = intent.getIntExtra(EXTRA_PROGRESS, -1);

            String text;
            switch (resultCode) {
                case STATUS_INFO:
                    notificationBuilder.setContentText(message)
                        .setCategory(NotificationCompat.CATEGORY_SERVICE);
                    if (progress != -1) {
                        notificationBuilder.setProgress(100, progress, false);
                    } else {
                        notificationBuilder.setProgress(100, 0, true);
                    }
                    notificationManager.notify(NOTIFY_ID_UPDATING, notificationBuilder.build());
                    break;
                case STATUS_ERROR_GLOBAL:
                    text = context.getString(R.string.global_error_updating_repos, message);
                    notificationBuilder.setContentText(text)
                        .setCategory(NotificationCompat.CATEGORY_ERROR)
                        .setSmallIcon(android.R.drawable.ic_dialog_alert);
                    notificationManager.notify(NOTIFY_ID_UPDATING, notificationBuilder.build());
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
                    notificationManager.notify(NOTIFY_ID_UPDATING, notificationBuilder.build());
                    Toast.makeText(context, text, Toast.LENGTH_LONG).show();
                    break;
                case STATUS_COMPLETE_WITH_CHANGES:
                    break;
                case STATUS_COMPLETE_AND_SAME:
                    text = context.getString(R.string.repos_unchanged);
                    notificationBuilder.setContentText(text)
                        .setCategory(NotificationCompat.CATEGORY_SERVICE);
                    notificationManager.notify(NOTIFY_ID_UPDATING, notificationBuilder.build());
                    break;
            }
        }
    };

    /**
     * Check whether it is time to run the scheduled update.
     * We don't want to run if:
     * - The time between scheduled runs is set to zero (though don't know
     * when that would occur)
     * - Last update was too recent
     * - Not on wifi, but the property for "Only auto update on wifi" is set.
     *
     * @return True if we are due for a scheduled update.
     */
    private boolean verifyIsTimeForScheduledRun() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String sint = prefs.getString(Preferences.PREF_UPD_INTERVAL, "0");
        int interval = Integer.parseInt(sint);
        if (interval == 0) {
            Log.i(TAG, "Skipping update - disabled");
            return false;
        }
        long lastUpdate = prefs.getLong(Preferences.PREF_UPD_LAST, 0);
        long elapsed = System.currentTimeMillis() - lastUpdate;
        if (elapsed < interval * 60 * 60 * 1000) {
            Log.i(TAG, "Skipping update - done " + elapsed
                    + "ms ago, interval is " + interval + " hours");
            return false;
        }

        return isNetworkAvailableForUpdate(this);
    }

    /**
     * If we are to update the repos only on wifi, make sure that connection is active
     */
    public static boolean isNetworkAvailableForUpdate(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // this could be cellular or wifi
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork == null)
            return false;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (activeNetwork.getType() != ConnectivityManager.TYPE_WIFI
                && prefs.getBoolean(Preferences.PREF_UPD_WIFI_ONLY, false)) {
            Log.i(TAG, "Skipping update - wifi not available");
            return false;
        }
        return activeNetwork.isConnectedOrConnecting();
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        final long startTime = System.currentTimeMillis();
        String address = intent.getStringExtra(EXTRA_ADDRESS);
        boolean manualUpdate = intent.getBooleanExtra(EXTRA_MANUAL_UPDATE, false);

        try {
            // See if it's time to actually do anything yet...
            if (manualUpdate) {
                Utils.debugLog(TAG, "Unscheduled (manually requested) update");
            } else if (!verifyIsTimeForScheduledRun()) {
                return;
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

            // Grab some preliminary information, then we can release the
            // database while we do all the downloading, etc...
            List<Repo> repos = RepoProvider.Helper.all(this);

            //List<Repo> swapRepos = new ArrayList<>();
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
                    //swapRepos.add(repo);
                    continue;
                }

                sendStatus(this, STATUS_INFO, getString(R.string.status_connecting_to_repo, repo.address));
                RepoUpdater updater = new RepoUpdater(getBaseContext(), repo);
                updater.setProgressListener(this);
                try {
                    updater.update();
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
            }

            if (!changes) {
                Utils.debugLog(TAG, "Not checking app details or compatibility, because all repos were up to date.");
            } else {
                notifyContentProviders();

                if (prefs.getBoolean(Preferences.PREF_UPD_NOTIFY, true)) {
                    performUpdateNotification();
                }
            }

            SharedPreferences.Editor e = prefs.edit();
            e.putLong(Preferences.PREF_UPD_LAST, System.currentTimeMillis());
            e.commit();

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
        Cursor cursor = getContentResolver().query(
                AppProvider.getCanUpdateUri(),
                AppProvider.DataColumns.ALL,
                null, null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                showAppUpdatesNotification(cursor);
            }
            cursor.close();
        }
    }

    private PendingIntent createNotificationIntent() {
        Intent notifyIntent = new Intent(this, FDroid.class).putExtra(FDroid.EXTRA_TAB_UPDATE, true);
        TaskStackBuilder stackBuilder = TaskStackBuilder
                .create(this).addParentStack(FDroid.class)
                .addNextIntent(notifyIntent);
        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static final int MAX_UPDATES_TO_SHOW = 5;

    private NotificationCompat.Style createNotificationBigStyle(Cursor hasUpdates) {

        final String contentText = hasUpdates.getCount() > 1
                ? getString(R.string.many_updates_available, hasUpdates.getCount())
                : getString(R.string.one_update_available);

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle(contentText);
        hasUpdates.moveToFirst();
        for (int i = 0; i < Math.min(hasUpdates.getCount(), MAX_UPDATES_TO_SHOW); i++) {
            App app = new App(hasUpdates);
            hasUpdates.moveToNext();
            inboxStyle.addLine(app.name + " (" + app.installedVersionName + " → " + app.getSuggestedVersion() + ")");
        }

        if (hasUpdates.getCount() > MAX_UPDATES_TO_SHOW) {
            int diff = hasUpdates.getCount() - MAX_UPDATES_TO_SHOW;
            inboxStyle.setSummaryText(getString(R.string.update_notification_more, diff));
        }

        return inboxStyle;
    }

    private void showAppUpdatesNotification(Cursor hasUpdates) {
        Utils.debugLog(TAG, "Notifying " + hasUpdates.getCount() + " updates.");

        final int icon = Build.VERSION.SDK_INT >= 11 ? R.drawable.ic_stat_notify_updates : R.drawable.ic_launcher;

        final String contentText = hasUpdates.getCount() > 1
                ? getString(R.string.many_updates_available, hasUpdates.getCount())
                : getString(R.string.one_update_available);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                    .setAutoCancel(true)
                    .setContentTitle(getString(R.string.fdroid_updates_available))
                    .setSmallIcon(icon)
                    .setContentIntent(createNotificationIntent())
                    .setContentText(contentText)
                    .setStyle(createNotificationBigStyle(hasUpdates));

        notificationManager.notify(NOTIFY_ID_UPDATES_AVAILABLE, builder.build());
    }

    /**
     * Received progress event from the RepoXMLHandler. It could be progress
     * downloading from the repo, or perhaps processing the info from the repo.
     */
    @Override
    public void onProgress(ProgressListener.Event event) {
        String message = "";
        String repoAddress = event.getData().getString(RepoUpdater.PROGRESS_DATA_REPO_ADDRESS);
        String downloadedSize = Utils.getFriendlySize(event.progress);
        String totalSize = Utils.getFriendlySize(event.total);
        int percent = event.total > 0 ? (int) ((double) event.progress / event.total * 100) : -1;
        switch (event.type) {
            case RepoUpdater.PROGRESS_TYPE_PROCESS_XML:
                message = getString(R.string.status_processing_xml_percent, repoAddress, downloadedSize, totalSize, percent);
                break;
            case RepoUpdater.PROGRESS_COMMITTING:
                message = getString(R.string.status_inserting_apps);
                break;
        }
        sendStatus(this, STATUS_INFO, message, percent);
    }
}
