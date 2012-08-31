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

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
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
        // the app in response to a user's request. If we get this receiver, it's
        // the latter...
        ResultReceiver receiver = intent.getParcelableExtra("receiver");
        
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());

        // See if it's time to actually do anything yet...
        if(receiver == null) {
            long lastUpdate = prefs.getLong("lastUpdateCheck", 0);
            String sint = prefs.getString("updateInterval", "0");
            int interval = Integer.parseInt(sint);
            if (interval == 0)
                return;
            if (lastUpdate + (interval * 60 * 60) > System
                    .currentTimeMillis())
                return;
        }

        // Do the update...
        DB db = null;
        try {
            db = new DB(getBaseContext());
            boolean notify = prefs.getBoolean("updateNotify", false);

            // Get the number of updates available before we
            // start, so we can notify if there are new ones.
            // (But avoid doing it if the user doesn't want
            // notifications, since it may be time consuming)
            int prevUpdates = 0;
            if (notify)
                prevUpdates = db.getNumUpdates();

            boolean success = RepoXMLHandler.doUpdates(
                    getBaseContext(), db);

            if (success && notify) {
                int newUpdates = db.getNumUpdates();
                Log.d("FDroid", "Updates before:" + prevUpdates + ", after: " +newUpdates);                        
                if (newUpdates > prevUpdates) {
                    NotificationManager n = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    Notification notification = new Notification(
                            R.drawable.icon,
                            "FDroid Updates Available", System
                                    .currentTimeMillis());
                    Context context = getApplicationContext();
                    CharSequence contentTitle = "FDroid";
                    CharSequence contentText = "Updates are available.";
                    Intent notificationIntent = new Intent(
                            UpdateService.this, FDroid.class);
                    PendingIntent contentIntent = PendingIntent
                            .getActivity(UpdateService.this, 0,
                                    notificationIntent, 0);
                    notification.setLatestEventInfo(context,
                            contentTitle, contentText, contentIntent);
                    notification.flags |= Notification.FLAG_AUTO_CANCEL;
                    n.notify(1, notification);
                }
            }
            
            if(receiver != null) {
                Bundle resultData = new Bundle();
                receiver.send(0, resultData);
            }

        } catch (Exception e) {
            Log.e("FDroid", "Exception during handleCommand():\n"
                    + Log.getStackTraceString(e));
            if(receiver != null) {
                Bundle resultData = new Bundle();
                receiver.send(1, resultData);
            }
        } finally {
            if (db != null)
                db.close();
        }

    }

}
