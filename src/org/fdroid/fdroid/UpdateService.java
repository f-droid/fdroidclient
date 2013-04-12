/*
 * Copyright (C) 2010-12  Ciaran Gultnieks, ciaran@ciarang.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

public class UpdateService extends IntentService {

    public UpdateService() {
        super("UpdateService");
    }

    // Schedule (or cancel schedule for) this service, according to the
    // current preferences. Should be called a) at boot, or b) if the preference
    // is changed.
    // TODO: What if we get upgraded?
    public static void schedule(Context ctx) {

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(ctx);
        String sint = prefs.getString("updateInterval", "0");
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
        }
    }

    protected void onHandleIntent(Intent intent) {

        // We might be doing a scheduled run, or we might have been launched by
        // the app in response to a user's request. If we get this receiver,
        // it's
        // the latter...
        ResultReceiver receiver = intent.getParcelableExtra("receiver");

        long startTime = System.currentTimeMillis();
        String errmsg = "";

        try {

            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(getBaseContext());

            // See if it's time to actually do anything yet...
            if (receiver == null) {
                long lastUpdate = prefs.getLong("lastUpdateCheck", 0);
                String sint = prefs.getString("updateInterval", "0");
                int interval = Integer.parseInt(sint);
                if (interval == 0) {
                    Log.d("FDroid", "Skipping update - disabled");
                    return;
                }
                long elapsed = System.currentTimeMillis() - lastUpdate;
                if (elapsed < interval * 60 * 60) {
                    Log.d("FDroid", "Skipping update - done " + elapsed
                            + "ms ago, interval is " + interval + " hours");
                    return;
                }
            }

            boolean notify = prefs.getBoolean("updateNotify", false);

            // Grab some preliminary information, then we can release the
            // database while we do all the downloading, etc...
            int prevUpdates = 0;
            int newUpdates = 0;
            Vector<DB.Repo> repos;
            try {
                DB db = DB.getDB();
                repos = db.getRepos();
            } finally {
                DB.releaseDB();
            }

            // Process each repo...
            Vector<DB.App> apps = new Vector<DB.App>();
            Vector<Integer> keeprepos = new Vector<Integer>();
            boolean success = true;
            for (DB.Repo repo : repos) {
                if (repo.inuse) {
                    StringBuilder newetag = new StringBuilder();
                    String err = RepoXMLHandler.doUpdate(getBaseContext(),
                            repo, apps, newetag, keeprepos);
                    if (err == null) {
                        repo.lastetag = newetag.toString();
                    } else {
                        success = false;
                        err = "Update failed for " + repo.address + " - " + err;
                        Log.d("FDroid", err);
                        if (errmsg.length() == 0)
                            errmsg = err;
                        else
                            errmsg += "\n" + err;
                    }
                }
            }

            if (success) {
                Vector<DB.App> acceptedapps = new Vector<DB.App>();
                Vector<DB.App> prevapps = ((FDroidApp) getApplication())
                        .getApps();

                DB db = DB.getDB();
                try {

                    // Need to flag things we're keeping despite having received
                    // no data about during the update. (i.e. stuff from a repo
                    // that we know is unchanged due to the etag)
                    for (int keep : keeprepos) {
                        for (DB.App app : prevapps) {
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
                                    apps.add(app);
                                    app_k = app;
                                }
                                app_k.updated = true;
                                if (!app_k.detail_Populated) {
                                    db.populateDetails(app_k, keep);
                                }
                                for (DB.Apk apk : app.apks)
                                    if (apk.repo == keep)
                                        apk.updated = true;
                            }
                        }
                    }

                    prevUpdates = db.beginUpdate(prevapps);
                    for (DB.App app : apps) {
                        if (db.updateApplication(app))
                            acceptedapps.add(app);
                    }
                    db.endUpdate();
                    if (notify)
                        newUpdates = db.getNumUpdates();
                    for (DB.Repo repo : repos)
                        db.writeLastEtag(repo);
                } catch (Exception ex) {
                    db.cancelUpdate();
                    Log.e("FDroid", "Exception during update processing:\n"
                            + Log.getStackTraceString(ex));
                    errmsg = "Exception during processing - " + ex.getMessage();
                    success = false;
                } finally {
                    DB.releaseDB();
                }
                if (success) {
                    for (DB.App app : acceptedapps)
                        getIcon(app, repos);
                    ((FDroidApp) getApplication()).invalidateApps();
                }

            }

            if (success && notify) {
                Log.d("FDroid", "Updates before:" + prevUpdates + ", after: "
                        + newUpdates);
                if (newUpdates > prevUpdates) {
                    NotificationManager n = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    Notification notification = new Notification(
                            R.drawable.icon, "F-Droid Updates Available",
                            System.currentTimeMillis());
                    Context context = getApplicationContext();
                    CharSequence contentTitle = "F-Droid";
                    CharSequence contentText = "Updates are available.";
                    Intent notificationIntent = new Intent(UpdateService.this,
                            FDroid.class);
                    notificationIntent.putExtra(FDroid.EXTRA_TAB_UPDATE, true);
                    PendingIntent contentIntent = PendingIntent.getActivity(
                            UpdateService.this, 0, notificationIntent, 0);
                    notification.setLatestEventInfo(context, contentTitle,
                            contentText, contentIntent);
                    notification.flags |= Notification.FLAG_AUTO_CANCEL;
                    n.notify(1, notification);
                }
            }

            if (receiver != null) {
                Bundle resultData = new Bundle();
                if (!success) {
                    if (errmsg.length() == 0)
                        errmsg = "Unknown error";
                    resultData.putString("errmsg", errmsg);
                    receiver.send(1, resultData);
                } else {
                    receiver.send(0, resultData);
                }
                
            }

            if(success) {
                Editor e = prefs.edit();
                e.putLong("lastUpdateCheck", System.currentTimeMillis());
                e.commit();
            }

        } catch (Exception e) {
            Log.e("FDroid",
                    "Exception during update processing:\n"
                            + Log.getStackTraceString(e));
            if (receiver != null) {
                Bundle resultData = new Bundle();
                if (errmsg.length() == 0)
                    errmsg = "Unknown error";
                resultData.putString("errmsg", errmsg);
                receiver.send(1, resultData);
            }
        } finally {
            Log.d("FDroid", "Update took "
                    + ((System.currentTimeMillis() - startTime) / 1000)
                    + " seconds.");
        }

    }

    private void getIcon(DB.App app, Vector<DB.Repo> repos) {
        try {

            File f = new File(DB.getIconsPath(), app.icon);
            if (f.exists())
                return;

            if (app.apks.size() == 0)
                return;
            String server = null;
            for (DB.Repo repo : repos)
                if (repo.id == app.apks.get(0).repo)
                    server = repo.address;
            if (server == null)
                return;
            URL u = new URL(server + "/icons/" + app.icon);
            HttpURLConnection uc = (HttpURLConnection) u.openConnection();
            if (uc.getResponseCode() == 200) {
                InputStream input = null;
                OutputStream output = null;
                try {
                    input = uc.getInputStream();
                    output = new FileOutputStream(f);
                    Utils.copy(input, output);
                } finally {
                    Utils.closeQuietly(output);
                    Utils.closeQuietly(input);
                }
            }
        } catch (Exception e) {

        }
    }

}
