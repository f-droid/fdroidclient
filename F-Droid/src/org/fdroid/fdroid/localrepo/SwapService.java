package org.fdroid.fdroid.localrepo;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.localrepo.type.BonjourType;
import org.fdroid.fdroid.localrepo.type.NfcType;
import org.fdroid.fdroid.localrepo.type.WebServerType;
import org.fdroid.fdroid.net.WifiStateChangeService;
import org.fdroid.fdroid.views.swap.SwapWorkflowActivity;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Central service which manages all of the different moving parts of swap which are required
 * to enable p2p swapping of apps. Currently manages WiFi and NFC. Will manage Bluetooth in
 * the future.
 *
 * TODO: Manage threading correctly.
 */
public class SwapService extends Service {

    private static final String TAG = "SwapService";

    private static final int NOTIFICATION = 1;

    private final Binder binder = new Binder();
    private final BonjourType bonjourType;
    private final WebServerType webServerType;

    // TODO: The NFC type can't really be managed by the service, because it is intrinsically tied
    // to a specific _Activity_, and will only be active while that activity is shown. This service
    // knows nothing about activities.
    private final NfcType nfcType;

    private final static int TIMEOUT = 900000; // 15 mins

    /**
     * Used to automatically turn of swapping after a defined amount of time (15 mins).
     */
    @Nullable
    private Timer timer;

    public class Binder extends android.os.Binder {
        public SwapService getService() {
            return SwapService.this;
        }
    }

    public SwapService() {
        nfcType = new NfcType(this);
        bonjourType = new BonjourType(this);
        webServerType = new WebServerType(this);
    }

    public void onCreate() {
        super.onCreate();

        Preferences.get().unregisterLocalRepoBonjourListeners(bonjourEnabledListener);
        Preferences.get().registerLocalRepoHttpsListeners(httpsEnabledListener);

        LocalBroadcastManager.getInstance(this).registerReceiver(onWifiChange,
                new IntentFilter(WifiStateChangeService.BROADCAST));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disableSwapping();
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, SwapWorkflowActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        return new NotificationCompat.Builder(this)
                .setContentTitle(getText(R.string.local_repo_running))
                .setContentText(getText(R.string.touch_to_configure_local_repo))
                .setSmallIcon(R.drawable.ic_swap)
                .setContentIntent(contentIntent)
                .build();
    }

    private boolean enabled = false;

    /**
     * Ensures that the webserver is running, as are the other services which make swap work.
     * Will only do all this if it is not already running, and will run on a background thread.'
     * TODO: What about an "enabling" status? Not sure if it will be useful or not.
     */
    public void enableSwapping() {
        if (!enabled) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    enableSwappingSynchronous();
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    enabled = true;
                }
            }.execute();
        }

        // Regardless of whether it was previously enabled, start the timer again. This ensures that
        // if, e.g. a person views the swap activity again, it will attempt to enable swapping if
        // appropriate, and thus restart this timer.
        initTimer();
    }

    /**
     * The guts of this class - responsible for enabling the relevant services for swapping.
     *  * Doesn't know anything about enabled/disabled.
     *  * Runs synchronously on the thread it was called.
     */
    private void enableSwappingSynchronous() {
        nfcType.start();
        webServerType.start();
        bonjourType.start();
        startForeground(NOTIFICATION, createNotification());
    }

    public void disableSwapping() {
        if (enabled) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    disableSwappingSynchronous();
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    // TODO: Does this  need to be run before the background task, so that the timer
                    // can't kick in while we are shutting down everything?
                    if (timer != null) {
                        timer.cancel();
                    }

                    enabled = false;
                }
            }.execute();
        }
    }

    /**
     * @see SwapService#enableSwappingSynchronous()
     */
    private void disableSwappingSynchronous() {
        bonjourType.stop();
        webServerType.stop();
        nfcType.stop();
        stopForeground(true);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void restartIfEnabled() {
        if (enabled) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    disableSwappingSynchronous();
                    enableSwappingSynchronous();
                    return null;
                }
            }.execute();
        }
    }

    private void initTimer() {
        if (timer != null)
            timer.cancel();

        // automatically turn off after 15 minutes
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                disableSwapping();
            }
        }, TIMEOUT);
    }

    @SuppressWarnings("FieldCanBeLocal") // The constructor will get bloated if these are all local...
    private final Preferences.ChangeListener bonjourEnabledListener = new Preferences.ChangeListener() {
        @Override
        public void onPreferenceChange() {
            Log.i(TAG, "Use Bonjour while swapping preference changed.");
            if (enabled)
                if (Preferences.get().isLocalRepoBonjourEnabled())
                    bonjourType.start();
                else
                    bonjourType.stop();
        }
    };

    @SuppressWarnings("FieldCanBeLocal") // The constructor will get bloated if these are all local...
    private final Preferences.ChangeListener httpsEnabledListener = new Preferences.ChangeListener() {
        @Override
        public void onPreferenceChange() {
            Log.i(TAG, "Swap over HTTPS preference changed.");
            restartIfEnabled();
        }
    };

    @SuppressWarnings("FieldCanBeLocal") // The constructor will get bloated if these are all local...
    private final BroadcastReceiver onWifiChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
            restartIfEnabled();
        }
    };

}
