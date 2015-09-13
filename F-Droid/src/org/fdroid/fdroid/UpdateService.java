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
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.net.Downloader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpdateService extends IntentService implements ProgressListener {

    private static final String TAG = "UpdateService";

    public static final String LOCAL_ACTION_STATUS = "status";

    public static final String EXTRA_MESSAGE = "msg";
    public static final String EXTRA_REPO_ERRORS = "repoErrors";
    public static final String EXTRA_STATUS_CODE = "status";
    public static final String EXTRA_ADDRESS = "address";
    public static final String EXTRA_MANUAL_UPDATE = "manualUpdate";

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

    /**
     * When an app already exists in the db, and we are updating it on the off chance that some
     * values changed in the index, some fields should not be updated. Rather, they should be
     * ignored, because they were explicitly set by the user, and hence can't be automatically
     * overridden by the index.
     *
     * NOTE: In the future, these attributes will be moved to a join table, so that the app table
     * is essentially completely transient, and can be nuked at any time.
     */
    private static final String[] APP_FIELDS_TO_IGNORE = {
        AppProvider.DataColumns.IGNORE_ALLUPDATES,
        AppProvider.DataColumns.IGNORE_THISUPDATE
    };

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

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            Intent intent = new Intent(this, FDroid.class);
            // TODO: Is this the correct FLAG?
            notificationBuilder.setContentIntent(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
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

    protected void sendStatus(int statusCode) {
        sendStatus(statusCode, null);
    }

    protected void sendStatus(int statusCode, String message) {
        Intent intent = new Intent(LOCAL_ACTION_STATUS);
        intent.putExtra(EXTRA_STATUS_CODE, statusCode);
        if (!TextUtils.isEmpty(message))
            intent.putExtra(EXTRA_MESSAGE, message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
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
            int totalSize = intent.getIntExtra(Downloader.EXTRA_TOTAL_BYTES, -1);
            int percent = (int) ((double) downloadedSize / totalSize * 100);
            sendStatus(STATUS_INFO,
                    getString(R.string.status_download, repoAddress,
                            Utils.getFriendlySize(downloadedSize),
                            Utils.getFriendlySize(totalSize), percent));
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

            String text;
            switch (resultCode) {
                case STATUS_INFO:
                    notificationBuilder.setContentText(message)
                            .setProgress(0, 0, true)
                            .setCategory(NotificationCompat.CATEGORY_SERVICE);
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
     *  - The time between scheduled runs is set to zero (though don't know
     *    when that would occur)
     *  - Last update was too recent
     *  - Not on wifi, but the property for "Only auto update on wifi" is set.
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
        } else {
            return activeNetwork.isConnectedOrConnecting();
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {

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

            // Process each repo...
            Map<String, App> appsToUpdate = new HashMap<>();
            List<Apk> apksToUpdate = new ArrayList<>();
            //List<Repo> swapRepos = new ArrayList<>();
            List<Repo> unchangedRepos = new ArrayList<>();
            List<Repo> updatedRepos = new ArrayList<>();
            List<Repo> disabledRepos = new ArrayList<>();
            List<CharSequence> errorRepos = new ArrayList<>();
            ArrayList<CharSequence> repoErrors = new ArrayList<>();
            List<RepoUpdater.RepoUpdateRememberer> repoUpdateRememberers = new ArrayList<>();
            boolean changes = false;
            boolean singleRepoUpdate = !TextUtils.isEmpty(address);
            for (final Repo repo : repos) {

                if (!repo.inuse) {
                    disabledRepos.add(repo);
                    continue;
                } else if (singleRepoUpdate && !repo.address.equals(address)) {
                    unchangedRepos.add(repo);
                    continue;
                } else if (!singleRepoUpdate && repo.isSwap) {
                    //swapRepos.add(repo);
                    continue;
                }

                sendStatus(STATUS_INFO, getString(R.string.status_connecting_to_repo, repo.address));
                RepoUpdater updater = new RepoUpdater(getBaseContext(), repo);
                updater.setProgressListener(this);
                try {
                    updater.update();
                    if (updater.hasChanged()) {
                        for (final App app : updater.getApps()) {
                            appsToUpdate.put(app.id, app);
                        }
                        apksToUpdate.addAll(updater.getApks());
                        updatedRepos.add(repo);
                        changes = true;
                        repoUpdateRememberers.add(updater.getRememberer());
                    } else {
                        unchangedRepos.add(repo);
                    }
                } catch (RepoUpdater.UpdateException e) {
                    errorRepos.add(repo.address);
                    repoErrors.add(e.getMessage());
                    Log.e(TAG, "Error updating repository " + repo.address, e);
                }
            }

            if (!changes) {
                Utils.debugLog(TAG, "Not checking app details or compatibility, because all repos were up to date.");
            } else {
                sendStatus(STATUS_INFO, getString(R.string.status_checking_compatibility));

                List<App> listOfAppsToUpdate = new ArrayList<>();
                listOfAppsToUpdate.addAll(appsToUpdate.values());

                calcApkCompatibilityFlags(this, apksToUpdate);

                // Need to do this BEFORE updating the apks, otherwise when it continually
                // calls "get existing apks for repo X" then it will be getting the newly
                // created apks, rather than those from the fresh, juicy index we just processed.
                removeApksNoLongerInRepo(apksToUpdate, updatedRepos);

                int totalInsertsUpdates = listOfAppsToUpdate.size() + apksToUpdate.size();
                updateOrInsertApps(listOfAppsToUpdate, totalInsertsUpdates, 0);
                updateOrInsertApks(apksToUpdate, totalInsertsUpdates, listOfAppsToUpdate.size());
                removeApksFromRepos(disabledRepos);
                removeAppsWithoutApks();

                // This will sort out the icon urls, compatibility flags. and suggested version
                // for each app. It used to happen here in Java code, but was moved to SQL when
                // it became apparant we don't always have enough info (depending on which repos
                // were updated).
                AppProvider.Helper.calcDetailsFromIndex(this);

                notifyContentProviders();

                //we only remember the update if everything has gone well
                for (RepoUpdater.RepoUpdateRememberer rememberer : repoUpdateRememberers) {
                    rememberer.rememberUpdate();
                }

                if (prefs.getBoolean(Preferences.PREF_UPD_NOTIFY, true)) {
                    performUpdateNotification();
                }
            }

            SharedPreferences.Editor e = prefs.edit();
            e.putLong(Preferences.PREF_UPD_LAST, System.currentTimeMillis());
            e.commit();

            if (errorRepos.isEmpty()) {
                if (changes) {
                    sendStatus(STATUS_COMPLETE_WITH_CHANGES);
                } else {
                    sendStatus(STATUS_COMPLETE_AND_SAME);
                }
            } else {
                if (updatedRepos.size() + unchangedRepos.size() == 0) {
                    sendRepoErrorStatus(STATUS_ERROR_LOCAL, repoErrors);
                } else {
                    sendRepoErrorStatus(STATUS_ERROR_LOCAL_SMALL, repoErrors);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception during update processing", e);
            sendStatus(STATUS_ERROR_GLOBAL, e.getMessage());
        }
    }

    private void notifyContentProviders() {
        getContentResolver().notifyChange(AppProvider.getContentUri(), null);
        getContentResolver().notifyChange(ApkProvider.getContentUri(), null);
    }

    /**
     * This cannot be offloaded to the database (as we did with the query which
     * updates apps, depending on whether their apks are compatible or not).
     * The reason is that we need to interact with the CompatibilityChecker
     * in order to see if, and why an apk is not compatible.
     */
    private static void calcApkCompatibilityFlags(Context context, List<Apk> apks) {
        final CompatibilityChecker checker = new CompatibilityChecker(context);
        for (final Apk apk : apks) {
            final List<String> reasons = checker.getIncompatibleReasons(apk);
            if (reasons.size() > 0) {
                apk.compatible = false;
                apk.incompatible_reasons = Utils.CommaSeparatedList.make(reasons);
            } else {
                apk.compatible = true;
                apk.incompatible_reasons = null;
            }
        }
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

    private List<String> getKnownAppIds(List<App> apps) {
        List<String> knownAppIds = new ArrayList<>();
        if (apps.isEmpty()) {
            return knownAppIds;
        }
        if (apps.size() > AppProvider.MAX_APPS_TO_QUERY) {
            int middle = apps.size() / 2;
            List<App> apps1 = apps.subList(0, middle);
            List<App> apps2 = apps.subList(middle, apps.size());
            knownAppIds.addAll(getKnownAppIds(apps1));
            knownAppIds.addAll(getKnownAppIds(apps2));
        } else {
            knownAppIds.addAll(getKnownAppIdsFromProvider(apps));
        }
        return knownAppIds;
    }

    /**
     * Looks in the database to see which apps we already know about. Only
     * returns ids of apps that are in the database if they are in the "apps"
     * array.
     */
    private List<String> getKnownAppIdsFromProvider(List<App> apps) {

        final Uri uri = AppProvider.getContentUri(apps);
        final String[] fields = { AppProvider.DataColumns.APP_ID };
        Cursor cursor = getContentResolver().query(uri, fields, null, null, null);

        int knownIdCount = cursor != null ? cursor.getCount() : 0;
        List<String> knownIds = new ArrayList<>(knownIdCount);
        if (cursor != null) {
            if (knownIdCount > 0) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    knownIds.add(cursor.getString(0));
                    cursor.moveToNext();
                }
            }
            cursor.close();
        }

        return knownIds;
    }

    private void updateOrInsertApps(List<App> appsToUpdate, int totalUpdateCount, int currentCount) {

        List<ContentProviderOperation> operations = new ArrayList<>();
        List<String> knownAppIds = getKnownAppIds(appsToUpdate);
        for (final App app : appsToUpdate) {
            if (knownAppIds.contains(app.id)) {
                operations.add(updateExistingApp(app));
            } else {
                operations.add(insertNewApp(app));
            }
        }

        Utils.debugLog(TAG, "Updating/inserting " + operations.size() + " apps.");
        try {
            executeBatchWithStatus(AppProvider.getAuthority(), operations, currentCount, totalUpdateCount);
        } catch (RemoteException | OperationApplicationException e) {
            Log.e(TAG, "Could not update or insert apps", e);
        }
    }

    private void executeBatchWithStatus(String providerAuthority,
                                        List<ContentProviderOperation> operations,
                                        int currentCount,
                                        int totalUpdateCount)
            throws RemoteException, OperationApplicationException {
        int i = 0;
        while (i < operations.size()) {
            int count = Math.min(operations.size() - i, 100);
            ArrayList<ContentProviderOperation> o = new ArrayList<>(operations.subList(i, i + count));
            sendStatus(STATUS_INFO, getString(
                R.string.status_inserting,
                (int)((double)(currentCount + i) / totalUpdateCount * 100)));
            getContentResolver().applyBatch(providerAuthority, o);
            i += 100;
        }
    }

    /**
     * Return list of apps from the "apks" argument which are already in the database.
     */
    private List<Apk> getKnownApks(List<Apk> apks) {
        final String[] fields = {
                ApkProvider.DataColumns.APK_ID,
                ApkProvider.DataColumns.VERSION,
                ApkProvider.DataColumns.VERSION_CODE
        };
        return ApkProvider.Helper.knownApks(this, apks, fields);
    }

    private void updateOrInsertApks(List<Apk> apksToUpdate, int totalApksAppsCount, int currentCount) {

        List<ContentProviderOperation> operations = new ArrayList<>();

        List<Apk> knownApks = getKnownApks(apksToUpdate);
        for (final Apk apk : apksToUpdate) {
            boolean known = false;
            for (final Apk knownApk : knownApks) {
                if (knownApk.id.equals(apk.id) && knownApk.vercode == apk.vercode) {
                    known = true;
                    break;
                }
            }

            if (known) {
                operations.add(updateExistingApk(apk));
            } else {
                operations.add(insertNewApk(apk));
                knownApks.add(apk); // In case another repo has the same version/id combo for this apk.
            }
        }

        Utils.debugLog(TAG, "Updating/inserting " + operations.size() + " apks.");
        try {
            executeBatchWithStatus(ApkProvider.getAuthority(), operations, currentCount, totalApksAppsCount);
        } catch (RemoteException | OperationApplicationException e) {
            Log.e(TAG, "Could not update/insert apps", e);
        }
    }

    private ContentProviderOperation updateExistingApk(final Apk apk) {
        Uri uri = ApkProvider.getContentUri(apk);
        ContentValues values = apk.toContentValues();
        return ContentProviderOperation.newUpdate(uri).withValues(values).build();
    }

    private ContentProviderOperation insertNewApk(final Apk apk) {
        ContentValues values = apk.toContentValues();
        Uri uri = ApkProvider.getContentUri();
        return ContentProviderOperation.newInsert(uri).withValues(values).build();
    }

    private ContentProviderOperation updateExistingApp(App app) {
        Uri uri = AppProvider.getContentUri(app);
        ContentValues values = app.toContentValues();
        for (final String toIgnore : APP_FIELDS_TO_IGNORE) {
            if (values.containsKey(toIgnore)) {
                values.remove(toIgnore);
            }
        }
        return ContentProviderOperation.newUpdate(uri).withValues(values).build();
    }

    private ContentProviderOperation insertNewApp(App app) {
        ContentValues values = app.toContentValues();
        Uri uri = AppProvider.getContentUri();
        return ContentProviderOperation.newInsert(uri).withValues(values).build();
    }

    /**
     * If a repo was updated (i.e. it is in use, and the index has changed
     * since last time we did an update), then we want to remove any apks that
     * belong to the repo which are not in the current list of apks that were
     * retrieved.
     */
    private void removeApksNoLongerInRepo(List<Apk> apksToUpdate, List<Repo> updatedRepos) {

        long startTime = System.currentTimeMillis();
        List<Apk> toRemove = new ArrayList<>();

        final String[] fields = {
            ApkProvider.DataColumns.APK_ID,
            ApkProvider.DataColumns.VERSION_CODE,
            ApkProvider.DataColumns.VERSION,
        };

        for (final Repo repo : updatedRepos) {
            final List<Apk> existingApks = ApkProvider.Helper.findByRepo(this, repo, fields);
            for (final Apk existingApk : existingApks) {
                if (!isApkToBeUpdated(existingApk, apksToUpdate)) {
                    toRemove.add(existingApk);
                }
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        Utils.debugLog(TAG, "Found " + toRemove.size() + " apks no longer in the updated repos (took " + duration + "ms)");

        if (toRemove.size() > 0) {
            ApkProvider.Helper.deleteApks(this, toRemove);
        }
    }

    private static boolean isApkToBeUpdated(Apk existingApk, List<Apk> apksToUpdate) {
        for (final Apk apkToUpdate : apksToUpdate) {
            if (apkToUpdate.vercode == existingApk.vercode && apkToUpdate.id.equals(existingApk.id)) {
                return true;
            }
        }
        return false;
    }

    private void removeApksFromRepos(List<Repo> repos) {
        for (final Repo repo : repos) {
            Uri uri = ApkProvider.getRepoUri(repo.getId());
            int numDeleted = getContentResolver().delete(uri, null, null);
            Utils.debugLog(TAG, "Removing " + numDeleted + " apks from repo " + repo.address);
        }
    }

    private void removeAppsWithoutApks() {
        int numDeleted = getContentResolver().delete(AppProvider.getNoApksUri(), null, null);
        Utils.debugLog(TAG, "Removing " + numDeleted + " apks that don't have any apks");
    }

    /**
     * Received progress event from the RepoXMLHandler. It could be progress
     * downloading from the repo, or perhaps processing the info from the repo.
     */
    @Override
    public void onProgress(ProgressListener.Event event) {
        String message = "";
        // TODO: Switch to passing through Bundles of data with the event, rather than a repo address. They are
        // now much more general purpose then just repo downloading.
        String repoAddress = event.getData().getString(RepoUpdater.PROGRESS_DATA_REPO_ADDRESS);
        String downloadedSize = Utils.getFriendlySize(event.progress);
        String totalSize = Utils.getFriendlySize(event.total);
        int percent = (int) ((double) event.progress / event.total * 100);
        switch (event.type) {
            case RepoUpdater.PROGRESS_TYPE_PROCESS_XML:
                message = getString(R.string.status_processing_xml_percent, repoAddress, downloadedSize, totalSize, percent);
                break;
        }
        sendStatus(STATUS_INFO, message);
    }
}
