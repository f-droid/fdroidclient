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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.updater.RepoUpdater;

public class UpdateService extends IntentService implements ProgressListener {

    public static final String RESULT_MESSAGE = "msg";
    public static final String RESULT_EVENT   = "event";


    public static final int STATUS_COMPLETE_WITH_CHANGES = 0;
    public static final int STATUS_COMPLETE_AND_SAME     = 1;
    public static final int STATUS_ERROR                 = 2;
    public static final int STATUS_INFO                  = 3;

    private ResultReceiver receiver = null;

    public UpdateService() {
        super("UpdateService");
    }

    // For receiving results from the UpdateService when we've told it to
    // update in response to a user request.
    public static class UpdateReceiver extends ResultReceiver {

        private Context context;
        private ProgressDialog dialog;
        private ProgressListener listener;

        public UpdateReceiver(Handler handler) {
            super(handler);
        }

        public UpdateReceiver setContext(Context context) {
            this.context = context;
            return this;
        }

        public UpdateReceiver setDialog(ProgressDialog dialog) {
            this.dialog = dialog;
            return this;
        }

        public UpdateReceiver setListener(ProgressListener listener) {
            this.listener = listener;
            return this;
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            String message = resultData.getString(UpdateService.RESULT_MESSAGE);
            boolean finished = false;
            if (resultCode == UpdateService.STATUS_ERROR) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                finished = true;
            } else if (resultCode == UpdateService.STATUS_COMPLETE_WITH_CHANGES
                    || resultCode == UpdateService.STATUS_COMPLETE_AND_SAME) {
                finished = true;
            } else if (resultCode == UpdateService.STATUS_INFO) {
                dialog.setMessage(message);
            }

            // Forward the progress event on to anybody else who'd like to know.
            if (listener != null) {
                Parcelable event = resultData.getParcelable(UpdateService.RESULT_EVENT);
                if (event != null && event instanceof Event) {
                    listener.onProgress((Event)event);
                }
            }

            if (finished && dialog.isShowing())
                dialog.dismiss();
        }
    }

    public static UpdateReceiver updateNow(Context context) {
        return updateRepoNow(null, context);
    }

    public static UpdateReceiver updateRepoNow(String address, Context context) {
        String title   = context.getString(R.string.process_wait_title);
        String message = context.getString(R.string.process_update_msg);
        ProgressDialog dialog = ProgressDialog.show(context, title, message, true, true);
        dialog.setIcon(android.R.drawable.ic_dialog_info);
        dialog.setCanceledOnTouchOutside(false);

        Intent intent = new Intent(context, UpdateService.class);
        UpdateReceiver receiver = new UpdateReceiver(new Handler());
        receiver.setContext(context).setDialog(dialog);
        intent.putExtra("receiver", receiver);
        if (!TextUtils.isEmpty(address))
            intent.putExtra("address", address);
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
        sendStatus(statusCode, message, null);
    }

    protected void sendStatus(int statusCode, String message, Event event) {
        if (receiver != null) {
            Bundle resultData = new Bundle();
            if (message != null && message.length() > 0)
                resultData.putString(RESULT_MESSAGE, message);
            if (event == null)
                event = new Event(statusCode);
            resultData.putParcelable(RESULT_EVENT, event);
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

    // Get the number of apps that have updates available.
    public int getNumUpdates(List<DB.App> apps) {
        int count = 0;
        for (DB.App app : apps) {
            if (app.toUpdate)
                count++;
        }
        return count;
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        receiver = intent.getParcelableExtra("receiver");
        String address = intent.getStringExtra("address");

        long startTime = System.currentTimeMillis();
        String errmsg = "";
        try {

            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(getBaseContext());

            // See if it's time to actually do anything yet...
            if (isScheduledRun()) {
                long lastUpdate = prefs.getLong(Preferences.PREF_UPD_LAST, 0);
                String sint = prefs.getString(Preferences.PREF_UPD_INTERVAL, "0");
                int interval = Integer.parseInt(sint);
                if (interval == 0) {
                    Log.d("FDroid", "Skipping update - disabled");
                    return;
                }
                long elapsed = System.currentTimeMillis() - lastUpdate;
                if (elapsed < interval * 60 * 60 * 1000) {
                    Log.d("FDroid", "Skipping update - done " + elapsed
                            + "ms ago, interval is " + interval + " hours");
                    return;
                }

                // If we are to update the repos only on wifi, make sure that
                // connection is active
                if (prefs.getBoolean(Preferences.PREF_UPD_WIFI_ONLY, false)) {
                    ConnectivityManager conMan = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo.State wifi = conMan.getNetworkInfo(1).getState();
                    if (wifi != NetworkInfo.State.CONNECTED &&
                            wifi !=  NetworkInfo.State.CONNECTING) {
                        Log.d("FDroid", "Skipping update - wifi not available");
                        return;
                    }
                }
            } else {
                Log.d("FDroid", "Unscheduled (manually requested) update");
            }
            errmsg = updateRepos(address);
            if (TextUtils.isEmpty(errmsg)) {
                Editor e = prefs.edit();
                e.putLong(Preferences.PREF_UPD_LAST, System.currentTimeMillis());
                e.commit();
            }
        } catch (Exception e) {
            Log.e("FDroid",
                    "Exception during update processing:\n"
                            + Log.getStackTraceString(e));
            if (TextUtils.isEmpty(errmsg))
                errmsg = "Unknown error";
            sendStatus(STATUS_ERROR, errmsg);
       } finally {
            Log.d("FDroid", "Update took "
                    + ((System.currentTimeMillis() - startTime) / 1000)
                    + " seconds.");
            receiver = null;
        }
    }

    protected String updateRepos(String address) throws Exception {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());
        boolean notify = prefs.getBoolean(Preferences.PREF_UPD_NOTIFY, false);
        String errmsg = "";
        // Grab some preliminary information, then we can release the
        // database while we do all the downloading, etc...
        int updates = 0;
        List<Repo> repos;
        List<DB.App> apps;
        try {
            DB db = DB.getDB();
            apps = db.getApps(false);
        } finally {
            DB.releaseDB();
        }

        repos = RepoProvider.Helper.all(getContentResolver());

        // Process each repo...
        List<DB.App> updatingApps = new ArrayList<DB.App>();
        Set<Long> keeprepos = new TreeSet<Long>();
        boolean changes = false;
        boolean update;
        for (Repo repo : repos) {
            if (!repo.inuse)
                continue;
            // are we updating all repos, or just one?
            if (TextUtils.isEmpty(address)) {
                update = true;
            } else {
                // if only updating one repo, mark the rest as keepers
                if (address.equals(repo.address)) {
                    update = true;
                } else {
                    keeprepos.add(repo.getId());
                    update = false;
                }
            }
            if (!update)
                continue;
            sendStatus(STATUS_INFO, getString(R.string.status_connecting_to_repo, repo.address));
            RepoUpdater updater = RepoUpdater.createUpdaterFor(getBaseContext(), repo);
            updater.setProgressListener(this);
            try {
                updater.update();
                if (updater.hasChanged()) {
                    updatingApps.addAll(updater.getApps());
                    changes = true;
                } else {
                    keeprepos.add(repo.getId());
                }
            } catch (RepoUpdater.UpdateException e) {
                errmsg += (errmsg.length() == 0 ? "" : "\n") + e.getMessage();
                Log.e("FDroid", "Error updating repository " + repo.address + ": " + e.getMessage());
                Log.e("FDroid", Log.getStackTraceString(e));
            }
        }

        boolean success = true;
        if (!changes) {
            Log.d("FDroid", "Not checking app details or compatibility, " +
                    "because all repos were up to date.");
        } else {
            sendStatus(STATUS_INFO, getString(R.string.status_checking_compatibility));

            DB db = DB.getDB();
            try {

                // Need to flag things we're keeping despite having received
                // no data about during the update. (i.e. stuff from a repo
                // that we know is unchanged due to the etag)
                for (long keep : keeprepos) {
                    for (DB.App app : apps) {
                        boolean keepapp = false;
                        for (DB.Apk apk : app.apks) {
                            if (apk.repo == keep) {
                                keepapp = true;
                                break;
                            }
                        }
                        if (keepapp) {
                            DB.App app_k = null;
                            for (DB.App app2 : apps) {
                                if (app2.id.equals(app.id)) {
                                    app_k = app2;
                                    break;
                                }
                            }
                            if (app_k == null) {
                                updatingApps.add(app);
                                app_k = app;
                            }
                            app_k.updated = true;
                            db.populateDetails(app_k, keep);
                            for (DB.Apk apk : app.apks)
                                if (apk.repo == keep)
                                    apk.updated = true;
                        }
                    }
                }

                db.beginUpdate(apps);
                for (DB.App app : updatingApps) {
                    db.updateApplication(app);
                }
                db.endUpdate();
            } catch (Exception ex) {
                db.cancelUpdate();
                Log.e("FDroid", "Exception during update processing:\n"
                        + Log.getStackTraceString(ex));
                errmsg = "Exception during processing - " + ex.getMessage();
                success = false;
            } finally {
                DB.releaseDB();
            }
        }

        if (success && changes) {
            ((FDroidApp) getApplication()).invalidateAllApps();
            if (notify) {
                apps = ((FDroidApp) getApplication()).getApps();
                updates = getNumUpdates(apps);
            }
            if (notify && updates > 0)
                showAppUpdatesNotification(updates);
        }

        if (success) {
            if (changes) {
                sendStatus(STATUS_COMPLETE_WITH_CHANGES);
            } else {
                sendStatus(STATUS_COMPLETE_AND_SAME);
            }
        } else {
            if (TextUtils.isEmpty(errmsg))
                errmsg = "Unknown error";
            sendStatus(STATUS_ERROR, errmsg);
        }

        return errmsg;
    }

    private void showAppUpdatesNotification(int updates) throws Exception {
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

    /**
     * Received progress event from the RepoXMLHandler. It could be progress
     * downloading from the repo, or perhaps processing the info from the repo.
     */
    @Override
    public void onProgress(ProgressListener.Event event) {
        String message = "";
        if (event.type == RepoUpdater.PROGRESS_TYPE_DOWNLOAD) {
            String repoAddress    = event.data.getString(RepoUpdater.PROGRESS_DATA_REPO);
            String downloadedSize = Utils.getFriendlySize( event.progress );
            String totalSize      = Utils.getFriendlySize( event.total );
            int percent           = (int)((double)event.progress/event.total * 100);
            message = getString(R.string.status_download, repoAddress, downloadedSize, totalSize, percent);
        } else if (event.type == RepoUpdater.PROGRESS_TYPE_PROCESS_XML) {
            String repoAddress    = event.data.getString(RepoUpdater.PROGRESS_DATA_REPO);
            message = getString(R.string.status_processing_xml, repoAddress, event.progress, event.total);
        }
        sendStatus(STATUS_INFO, message);
    }
}
