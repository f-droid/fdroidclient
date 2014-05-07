
package org.fdroid.fdroid.localrepo;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.net.LocalHTTPD;
import org.fdroid.fdroid.net.WifiStateChangeService;
import org.fdroid.fdroid.views.LocalRepoActivity;

import java.io.IOException;
import java.net.BindException;
import java.util.Random;

public class LocalRepoService extends Service {
    private static final String TAG = "LocalRepoService";

    private NotificationManager notificationManager;
    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = R.string.local_repo_running;

    private Handler webServerThreadHandler = null;

    public static int START = 1111111;
    public static int STOP = 12345678;
    public static int RESTART = 87654;

    final Messenger messenger = new Messenger(new StartStopHandler(this));

    static class StartStopHandler extends Handler {
        private static LocalRepoService service;

        public StartStopHandler(LocalRepoService service) {
            StartStopHandler.service = service;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.arg1 == START) {
                service.startWebServer();
            } else if (msg.arg1 == STOP) {
                service.stopWebServer();
            } else if (msg.arg1 == RESTART) {
                service.stopWebServer();
                service.startWebServer();
            } else {
                Log.e(TAG, "unsupported msg.arg1, ignored");
            }
        }
    }

    private BroadcastReceiver onWifiChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
            stopWebServer();
            startWebServer();
        }
    };

    @Override
    public void onCreate() {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // launch LocalRepoActivity if the user selects this notification
        Intent intent = new Intent(this, LocalRepoActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getText(R.string.local_repo_running))
                .setContentText(getText(R.string.touch_to_configure_local_repo))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(contentIntent)
                .build();
        startForeground(NOTIFICATION, notification);
        startWebServer();
        LocalBroadcastManager.getInstance(this).registerReceiver(onWifiChange,
                new IntentFilter(WifiStateChangeService.BROADCAST));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopWebServer();
        notificationManager.cancel(NOTIFICATION);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onWifiChange);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    private void startWebServer() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        Runnable webServer = new Runnable() {
            // Tell Eclipse this is not a leak because of Looper use.
            @SuppressLint("HandlerLeak")
            @Override
            public void run() {
                final LocalHTTPD localHttpd = new LocalHTTPD(getFilesDir(),
                        prefs.getBoolean("use_https", false));

                Looper.prepare(); // must be run before creating a Handler
                webServerThreadHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        Log.i(TAG, "we've been asked to stop the webserver: " + msg.obj);
                        localHttpd.stop();
                    }
                };
                try {
                    localHttpd.start();
                } catch (BindException e) {
                    int prev = FDroidApp.port;
                    FDroidApp.port = FDroidApp.port + new Random().nextInt(1111);
                    Log.w(TAG, "port " + prev + " occupied, trying on " + FDroidApp.port + "!");
                    startService(new Intent(LocalRepoService.this, WifiStateChangeService.class));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Looper.loop(); // start the message receiving loop
            }
        };
        new Thread(webServer).start();
    }

    private void stopWebServer() {
        if (webServerThreadHandler == null) {
            Log.i(TAG, "null handler in stopWebServer");
            return;
        }
        Message msg = webServerThreadHandler.obtainMessage();
        msg.obj = webServerThreadHandler.getLooper().getThread().getName() + " says stop";
        webServerThreadHandler.sendMessage(msg);
    }
}
