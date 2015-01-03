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
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
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
import org.fdroid.fdroid.updater.RepoUpdater;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpdateService extends IntentService implements ProgressListener {

    public static final String RESULT_MESSAGE     = "msg";
    public static final String RESULT_EVENT       = "event";
    public static final String RESULT_REPO_ERRORS = "repoErrors";

    public static final int STATUS_COMPLETE_WITH_CHANGES = 0;
    public static final int STATUS_COMPLETE_AND_SAME     = 1;
    public static final int STATUS_ERROR_GLOBAL          = 2;
    public static final int STATUS_ERROR_LOCAL           = 3;
    public static final int STATUS_ERROR_LOCAL_SMALL     = 4;
    public static final int STATUS_INFO                  = 5;

    // I don't like that I've had to dupliacte the statuses above with strings here, however
    // one method of communication/notification is using ResultReceiver (int status codes)
    // while the other uses progress events (string event types).
    public static final String EVENT_COMPLETE_WITH_CHANGES = "repoUpdateComplete (changed)";
    public static final String EVENT_COMPLETE_AND_SAME     = "repoUpdateComplete (not changed)";
    public static final String EVENT_FINISHED              = "repoUpdateFinished";
    public static final String EVENT_ERROR                 = "repoUpdateError";
    public static final String EVENT_INFO                  = "repoUpdateInfo";

    public static final String EXTRA_RECEIVER = "receiver";
    public static final String EXTRA_ADDRESS = "address";

    private ResultReceiver receiver = null;

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

    // For receiving results from the UpdateService when we've told it to
    // update in response to a user request.
    public static class UpdateReceiver extends ResultReceiver {

        private Context context;
        private ProgressDialog dialog;
        private ProgressListener listener;
        private String lastShownMessage = null;

        public UpdateReceiver(Handler handler) {
            super(handler);
        }

        public UpdateReceiver setDialog(ProgressDialog dialog) {
            this.dialog = dialog;
            return this;
        }

        public UpdateReceiver setListener(ProgressListener listener) {
            this.listener = listener;
            return this;
        }

        private void forwardEvent(String type) {
            if (listener != null) {
                listener.onProgress(new Event(type));
            }
        }

        private void ensureDialog() {
            if (dialog == null) {
                String title = context.getString(R.string.process_wait_title);
                String message = lastShownMessage == null ? context.getString(R.string.process_update_msg) : lastShownMessage;
                dialog = ProgressDialog.show(context, title, message, true, true);
                dialog.setIcon(android.R.drawable.ic_dialog_info);
                dialog.setCanceledOnTouchOutside(false);
            }
        }

        public UpdateReceiver showDialog(Context context) {
            this.context = context;
            ensureDialog();
            dialog.show();
            return this;
        }

        public void hideDialog() {
            dialog.hide();
            dialog.dismiss();
            dialog = null;
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            final String message = resultData.getString(UpdateService.RESULT_MESSAGE);
            boolean finished = false;
            if (resultCode == UpdateService.STATUS_ERROR_GLOBAL) {
                forwardEvent(EVENT_ERROR);
                Toast.makeText(context, context.getString(R.string.global_error_updating_repos) + " " + message, Toast.LENGTH_LONG).show();
                finished = true;
            } else if (resultCode == UpdateService.STATUS_ERROR_LOCAL || resultCode == UpdateService.STATUS_ERROR_LOCAL_SMALL) {
                StringBuilder msgB = new StringBuilder();
                ArrayList<CharSequence> repoErrors = resultData.getCharSequenceArrayList(UpdateService.RESULT_REPO_ERRORS);
                for (CharSequence error : repoErrors) {
                    if (msgB.length() > 0) msgB.append('\n');
                    msgB.append(error);
                }
                if (resultCode == UpdateService.STATUS_ERROR_LOCAL_SMALL) {
                    msgB.append("\n").append(context.getString(R.string.all_other_repos_fine));
                }
                Toast.makeText(context, msgB.toString(), Toast.LENGTH_LONG).show();
                finished = true;
            } else if (resultCode == UpdateService.STATUS_COMPLETE_WITH_CHANGES) {
                forwardEvent(EVENT_COMPLETE_WITH_CHANGES);
                finished = true;
            } else if (resultCode == UpdateService.STATUS_COMPLETE_AND_SAME) {
                forwardEvent(EVENT_COMPLETE_AND_SAME);
                Toast.makeText(context, context.getString(R.string.repos_unchanged), Toast.LENGTH_LONG).show();
                finished = true;
            } else if (resultCode == UpdateService.STATUS_INFO) {
                forwardEvent(EVENT_INFO);
                if (dialog != null) {
                    lastShownMessage = message;
                    dialog.setMessage(message);
                }
            }

            if (finished) {
                forwardEvent(EVENT_FINISHED);
                if (dialog != null && dialog.isShowing()) {
                    try {
                        dialog.dismiss();
                    } catch (IllegalArgumentException e) {
                        // sometimes dialog.isShowing() doesn't work :(
                        // https://stackoverflow.com/questions/19538282/view-not-attached-to-window-manager-dialog-dismiss
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static UpdateReceiver updateNow(Context context) {
        return updateRepoNow(null, context);
    }

    public static UpdateReceiver updateRepoNow(String address, Context context) {
        Intent intent = new Intent(context, UpdateService.class);
        UpdateReceiver receiver = new UpdateReceiver(new Handler());
        receiver.showDialog(context);
        intent.putExtra(EXTRA_RECEIVER, receiver);
        if (!TextUtils.isEmpty(address)) {
            intent.putExtra(EXTRA_ADDRESS, address);
        }
        context.startService(intent);

        return receiver;
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
            Log.d("FDroid", "Update scheduler alarm set");
        } else {
            Log.d("FDroid", "Update scheduler alarm not set");
        }

    }

    protected void sendStatus(int statusCode) {
        sendStatus(statusCode, null);
    }

    protected void sendStatus(int statusCode, String message) {
        if (receiver != null) {
            Bundle resultData = new Bundle();
            if (message != null && message.length() > 0)
                resultData.putString(RESULT_MESSAGE, message);
            receiver.send(statusCode, resultData);
        }
    }

    protected void sendRepoErrorStatus(int statusCode, ArrayList<CharSequence> repoErrors) {
        if (receiver != null) {
            Bundle resultData = new Bundle();
            resultData.putCharSequenceArrayList(RESULT_REPO_ERRORS, repoErrors);
            receiver.send(statusCode, resultData);
        }
    }

    /**
     * We might be doing a scheduled run, or we might have been launched by the
     * app in response to a user's request. If we have a receiver, it's the
     * latter...
     */
    private boolean isScheduledRun() {
        return receiver == null;
    }

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
        long lastUpdate = prefs.getLong(Preferences.PREF_UPD_LAST, 0);
        String sint = prefs.getString(Preferences.PREF_UPD_INTERVAL, "0");
        int interval = Integer.parseInt(sint);
        if (interval == 0) {
            Log.d("FDroid", "Skipping update - disabled");
            return false;
        }
        long elapsed = System.currentTimeMillis() - lastUpdate;
        if (elapsed < interval * 60 * 60 * 1000) {
            Log.d("FDroid", "Skipping update - done " + elapsed
                    + "ms ago, interval is " + interval + " hours");
            return false;
        }

        // If we are to update the repos only on wifi, make sure that
        // connection is active
        if (prefs.getBoolean(Preferences.PREF_UPD_WIFI_ONLY, false)) {
            ConnectivityManager conMan = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo.State wifi = conMan.getNetworkInfo(1).getState();
            if (wifi != NetworkInfo.State.CONNECTED &&
                    wifi !=  NetworkInfo.State.CONNECTING) {
                Log.d("FDroid", "Skipping update - wifi not available");
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        receiver = intent.getParcelableExtra(EXTRA_RECEIVER);
        String address = intent.getStringExtra(EXTRA_ADDRESS);

        long startTime = System.currentTimeMillis();
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

            // See if it's time to actually do anything yet...
            if (!isScheduledRun()) {
                Log.d("FDroid", "Unscheduled (manually requested) update");
            } else if (!verifyIsTimeForScheduledRun()) {
                return;
            }

            // Grab some preliminary information, then we can release the
            // database while we do all the downloading, etc...
            List<Repo> repos = RepoProvider.Helper.all(this);

            // Process each repo...
            Map<String, App> appsToUpdate = new HashMap<String, App>();
            List<Apk> apksToUpdate = new ArrayList<Apk>();
            List<Repo> unchangedRepos = new ArrayList<Repo>();
            List<Repo> updatedRepos = new ArrayList<Repo>();
            List<Repo> disabledRepos = new ArrayList<Repo>();
            ArrayList<CharSequence> errorRepos = new ArrayList<CharSequence>();
            ArrayList<CharSequence> repoErrors = new ArrayList<CharSequence>();
            List<RepoUpdater.RepoUpdateRememberer> repoUpdateRememberers = new ArrayList<RepoUpdater.RepoUpdateRememberer>();
            boolean changes = false;
            for (final Repo repo : repos) {

                if (!repo.inuse) {
                    disabledRepos.add(repo);
                    continue;
                } else if (!TextUtils.isEmpty(address) && !repo.address.equals(address)) {
                    unchangedRepos.add(repo);
                    continue;
                }

                sendStatus(STATUS_INFO, getString(R.string.status_connecting_to_repo, repo.address));
                RepoUpdater updater = RepoUpdater.createUpdaterFor(getBaseContext(), repo);
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
                    Log.e("FDroid", "Error updating repository " + repo.address + ": " + e.getMessage());
                    Log.e("FDroid", Log.getStackTraceString(e));
                }
            }

            if (!changes) {
                Log.d("FDroid", "Not checking app details or compatibility, because all repos were up to date.");
            } else {
                sendStatus(STATUS_INFO, getString(R.string.status_checking_compatibility));

                List<App> listOfAppsToUpdate = new ArrayList<App>();
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
                    performUpdateNotification(appsToUpdate.values());
                }
            }

            Editor e = prefs.edit();
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
            Log.e("FDroid",
                    "Exception during update processing:\n"
                            + Log.getStackTraceString(e));
            sendStatus(STATUS_ERROR_GLOBAL, e.getMessage());
        } finally {
            Log.d("FDroid", "Update took "
                    + ((System.currentTimeMillis() - startTime) / 1000)
                    + " seconds.");
            receiver = null;
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
        CompatibilityChecker checker = new CompatibilityChecker(context);
        for (Apk apk : apks) {
            List<String> reasons = checker.getIncompatibleReasons(apk);
            if (reasons.size() > 0) {
                apk.compatible = false;
                apk.incompatible_reasons = Utils.CommaSeparatedList.make(reasons);
            } else {
                apk.compatible = true;
                apk.incompatible_reasons = null;
            }
        }
    }

    private void performUpdateNotification(Collection<App> apps) {
        int count = AppProvider.Helper.count(this, AppProvider.getCanUpdateUri());
        if (count > 0) {
            showAppUpdatesNotification(count);
        }
    }

    private void showAppUpdatesNotification(int updates) {
        Log.d("FDroid", "Notifying " + updates + " updates.");
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setAutoCancel(true)
                        .setContentTitle(getString(R.string.fdroid_updates_available));
        if (Build.VERSION.SDK_INT >= 11) {
            builder.setSmallIcon(R.drawable.ic_stat_notify_updates);
        } else {
            builder.setSmallIcon(R.drawable.ic_launcher);
        }
        Intent notifyIntent = new Intent(this, FDroid.class)
                .putExtra(FDroid.EXTRA_TAB_UPDATE, true);
        if (updates > 1) {
            builder.setContentText(getString(R.string.many_updates_available, updates));
        } else {
            builder.setContentText(getString(R.string.one_update_available));
        }
        TaskStackBuilder stackBuilder = TaskStackBuilder
                .create(this).addParentStack(FDroid.class)
                .addNextIntent(notifyIntent);
        PendingIntent pi = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pi);
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(1, builder.build());
    }

    private List<String> getKnownAppIds(List<App> apps) {
        List<String> knownAppIds = new ArrayList<String>();
        if (apps.size() == 0) {
            // Do nothing
        } else if (apps.size() > AppProvider.MAX_APPS_TO_QUERY) {
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
        String[] fields = new String[] { AppProvider.DataColumns.APP_ID };
        Cursor cursor = getContentResolver().query(uri, fields, null, null, null);

        int knownIdCount = cursor != null ? cursor.getCount() : 0;
        List<String> knownIds = new ArrayList<String>(knownIdCount);
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

    /**
     * If you call this with too many apks, then it will likely hit limit of
     * parameters allowed for sqlite3 query. Rather, you should use
     * {@link org.fdroid.fdroid.UpdateService#getKnownApks(java.util.List)}
     * instead, which will only call this with the right number of apks at
     * a time.
     * @see org.fdroid.fdroid.UpdateService#getKnownAppIds(java.util.List)
     */
    private List<Apk> getKnownApksFromProvider(List<Apk> apks) {
        String[] fields = {
            ApkProvider.DataColumns.APK_ID,
            ApkProvider.DataColumns.VERSION,
            ApkProvider.DataColumns.VERSION_CODE
        };
        return ApkProvider.Helper.knownApks(this, apks, fields);
    }

    private void updateOrInsertApps(List<App> appsToUpdate, int totalUpdateCount, int currentCount) {

        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        List<String> knownAppIds = getKnownAppIds(appsToUpdate);
        for (final App a : appsToUpdate) {
            boolean known = false;
            for (String knownId : knownAppIds) {
                if (knownId.equals(a.id)) {
                    known = true;
                    break;
                }
            }

            if (known) {
                operations.add(updateExistingApp(a));
            } else {
                operations.add(insertNewApp(a));
            }
        }

        Log.d("FDroid", "Updating/inserting " + operations.size() + " apps.");
        try {
            executeBatchWithStatus(AppProvider.getAuthority(), operations, currentCount, totalUpdateCount);
        } catch (RemoteException e) {
            Log.e("FDroid", e.getMessage());
        } catch (OperationApplicationException e) {
            Log.e("FDroid", e.getMessage());
        }
    }

    private void executeBatchWithStatus(String providerAuthority,
                                        ArrayList<ContentProviderOperation> operations,
                                        int currentCount,
                                        int totalUpdateCount)
            throws RemoteException, OperationApplicationException {
        int i = 0;
        while (i < operations.size()) {
            int count = Math.min(operations.size() - i, 100);
            ArrayList<ContentProviderOperation> o = new ArrayList<ContentProviderOperation>(operations.subList(i, i + count));
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
        List<Apk> knownApks = new ArrayList<Apk>();
        if (apks.size() > ApkProvider.MAX_APKS_TO_QUERY) {
            int middle = apks.size() / 2;
            List<Apk> apks1 = apks.subList(0, middle);
            List<Apk> apks2 = apks.subList(middle, apks.size());
            knownApks.addAll(getKnownApks(apks1));
            knownApks.addAll(getKnownApks(apks2));
        } else {
            knownApks.addAll(getKnownApksFromProvider(apks));
        }
        return knownApks;
    }

    private void updateOrInsertApks(List<Apk> apksToUpdate, int totalApksAppsCount, int currentCount) {

        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

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

        Log.d("FDroid", "Updating/inserting " + operations.size() + " apks.");
        try {
            executeBatchWithStatus(ApkProvider.getAuthority(), operations, currentCount, totalApksAppsCount);
        } catch (RemoteException e) {
            Log.e("FDroid", e.getMessage());
        } catch (OperationApplicationException e) {
            Log.e("FDroid", e.getMessage());
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
        for (String toIgnore : APP_FIELDS_TO_IGNORE) {
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
        List<Apk> toRemove = new ArrayList<Apk>();

        String[] fields = {
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
        Log.d("FDroid", "Found " + toRemove.size() + " apks no longer in the updated repos (took " + duration + "ms)");

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
            Log.d("FDroid", "Removing " + numDeleted + " apks from repo " + repo.address);
        }
    }

    private void removeAppsWithoutApks() {
        int numDeleted = getContentResolver().delete(AppProvider.getNoApksUri(), null, null);
        Log.d("FDroid", "Removing " + numDeleted + " apks that don't have any apks");
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
        if (event.type.equals(Downloader.EVENT_PROGRESS)) {
            String downloadedSize = Utils.getFriendlySize(event.progress);
            String totalSize      = Utils.getFriendlySize(event.total);
            int percent           = (int)((double)event.progress/event.total * 100);
            message = getString(R.string.status_download, repoAddress, downloadedSize, totalSize, percent);
        } else if (event.type.equals(RepoUpdater.PROGRESS_TYPE_PROCESS_XML)) {
            message = getString(R.string.status_processing_xml, repoAddress, event.progress, event.total);
        }
        sendStatus(STATUS_INFO, message);
    }
}
