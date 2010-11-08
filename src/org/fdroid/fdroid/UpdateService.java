/*
 * Copyright (C) 2010  Ciaran Gultnieks, ciaran@ciarang.com
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
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

public class UpdateService extends Service {

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

    // For API levels <5
    @Override
    public void onStart(Intent intent, int startId) {
        handleCommand();
    }

    // For API levels >=5
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleCommand();
        return START_REDELIVER_INTENT;
    }

    private void handleCommand() {

        new Thread() {
            public void run() {

                // See if it's time to actually do anything yet...
                SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(getBaseContext());
                long lastUpdate = prefs.getLong("lastUpdateCheck", 0);
                String sint = prefs.getString("updateInterval", "0");
                int interval = Integer.parseInt(sint);
                if (interval == 0)
                    return;
                if (lastUpdate + (interval * 60 * 60) > System
                        .currentTimeMillis())
                    return;

                // Make sure we have a connection...
                ConnectivityManager netstate = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                if (netstate.getNetworkInfo(1).getState() != NetworkInfo.State.CONNECTED
                        && netstate.getNetworkInfo(0).getState() != NetworkInfo.State.CONNECTED)
                    return;

                // Do the update...
                DB db = null;
                try {
                    db = new DB(getBaseContext());
                    RepoXMLHandler.doUpdates(db);
                } catch(Exception e) {
                    Log.d("FDroid","Exception during handleCommand() - " + e.getMessage());
                } finally {
                    if (db != null)
                        db.close();
                    stopSelf();
                }

            }
        }.start();

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
