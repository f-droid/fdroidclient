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
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.Preferences.ChangeListener;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.net.LocalHTTPD;
import org.fdroid.fdroid.net.WifiStateChangeService;
import org.fdroid.fdroid.views.swap.SwapActivity;

import java.io.IOException;
import java.net.BindException;
import java.util.HashMap;
import java.util.Random;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

public class LocalRepoService extends Service {
    private static final String TAG = "LocalRepoService";

    public static final String STATE = "org.fdroid.fdroid.action.LOCAL_REPO_STATE";
    public static final String STARTED = "org.fdroid.fdroid.category.LOCAL_REPO_STARTED";
    public static final String STOPPED = "org.fdroid.fdroid.category.LOCAL_REPO_STOPPED";

    private NotificationManager notificationManager;
    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private final int NOTIFICATION = R.string.local_repo_running;

    private Handler webServerThreadHandler = null;
    private LocalHTTPD localHttpd;
    private JmDNS jmdns;
    private ServiceInfo pairService;

    public static final int START = 1111111;
    public static final int STOP = 12345678;
    public static final int RESTART = 87654;

    final Messenger messenger = new Messenger(new StartStopHandler(this));

    /**
     * This is most likely going to be created on the UI thread, hence all of
     * the message handling will take place on a new thread to prevent blocking
     * the UI.
     */
    static class StartStopHandler extends Handler {

        private final LocalRepoService service;

        public StartStopHandler(LocalRepoService service) {
            this.service = service;
        }

        @Override
        public void handleMessage(final Message msg) {
            new Thread() {
                public void run() {
                    switch (msg.arg1) {
                    case START:
                        service.startNetworkServices();
                        break;
                    case STOP:
                        service.stopNetworkServices();
                        break;
                    case RESTART:
                        service.stopNetworkServices();
                        service.startNetworkServices();
                        break;
                    default:
                        Log.e(TAG, "Unsupported msg.arg1 (" + msg.arg1 + "), ignored");
                        break;
                    }
                }
            }.start();
        }
    }

    private final BroadcastReceiver onWifiChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
            stopNetworkServices();
            startNetworkServices();
        }
    };

    private ChangeListener localRepoBonjourChangeListener = new ChangeListener() {
        @Override
        public void onPreferenceChange() {
            if (localHttpd.isAlive())
                if (Preferences.get().isLocalRepoBonjourEnabled())
                    registerMDNSService();
                else
                    unregisterMDNSService();
        }
    };

    private final ChangeListener localRepoHttpsChangeListener = new ChangeListener() {
        @Override
        public void onPreferenceChange() {
            Log.i(TAG, "onPreferenceChange");
            if (localHttpd.isAlive()) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        stopNetworkServices();
                        startNetworkServices();
                        return null;
                    }
                }.execute();
            }
        }
    };

    private void showNotification() {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // launch LocalRepoActivity if the user selects this notification
        Intent intent = new Intent(this, SwapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getText(R.string.local_repo_running))
                .setContentText(getText(R.string.touch_to_configure_local_repo))
                .setSmallIcon(R.drawable.ic_swap)
                .setContentIntent(contentIntent)
                .build();
        startForeground(NOTIFICATION, notification);
    }

    @Override
    public void onCreate() {
        showNotification();
        startNetworkServices();
        Preferences.get().registerLocalRepoBonjourListeners(localRepoBonjourChangeListener);

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
        new Thread() {
            public void run() {
                stopNetworkServices();
            }
        }.start();

        notificationManager.cancel(NOTIFICATION);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onWifiChange);
        Preferences.get().unregisterLocalRepoBonjourListeners(localRepoBonjourChangeListener);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    private void startNetworkServices() {
        Log.d(TAG, "Starting local repo network services");
        startWebServer();
        if (Preferences.get().isLocalRepoBonjourEnabled())
            registerMDNSService();
        Preferences.get().registerLocalRepoHttpsListeners(localRepoHttpsChangeListener);
    }

    private void stopNetworkServices() {
        Log.d(TAG, "Stopping local repo network services");
        Preferences.get().unregisterLocalRepoHttpsListeners(localRepoHttpsChangeListener);

        Log.d(TAG, "Unregistering MDNS service...");
        unregisterMDNSService();

        Log.d(TAG, "Stopping web server...");
        stopWebServer();
    }

    private void startWebServer() {
        Runnable webServer = new Runnable() {
            // Tell Eclipse this is not a leak because of Looper use.
            @SuppressLint("HandlerLeak")
            @Override
            public void run() {
                localHttpd = new LocalHTTPD(
                        LocalRepoService.this,
                        getFilesDir(),
                        Preferences.get().isLocalRepoHttpsEnabled());

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
                    Log.e(TAG, "Could not start local repo HTTP server: " + e);
                    Log.e(TAG, Log.getStackTraceString(e));
                }
                Looper.loop(); // start the message receiving loop
            }
        };
        new Thread(webServer).start();
        Intent intent = new Intent(STATE);
        intent.putExtra(STATE, STARTED);
        LocalBroadcastManager.getInstance(LocalRepoService.this).sendBroadcast(intent);
    }

    private void stopWebServer() {
        if (webServerThreadHandler == null) {
            Log.i(TAG, "null handler in stopWebServer");
            return;
        }
        Message msg = webServerThreadHandler.obtainMessage();
        msg.obj = webServerThreadHandler.getLooper().getThread().getName() + " says stop";
        webServerThreadHandler.sendMessage(msg);
        Intent intent = new Intent(STATE);
        intent.putExtra(STATE, STOPPED);
        LocalBroadcastManager.getInstance(LocalRepoService.this).sendBroadcast(intent);
    }

    private void registerMDNSService() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                /*
                 * a ServiceInfo can only be registered with a single instance
                 * of JmDNS, and there is only ever a single LocalHTTPD port to
                 * advertise anyway.
                 */
                if (pairService != null || jmdns != null)
                    clearCurrentMDNSService();
                String repoName = Preferences.get().getLocalRepoName();
                HashMap<String, String> values = new HashMap<>();
                values.put("path", "/fdroid/repo");
                values.put("name", repoName);
                values.put("fingerprint", FDroidApp.repo.fingerprint);
                String type;
                if (Preferences.get().isLocalRepoHttpsEnabled()) {
                    values.put("type", "fdroidrepos");
                    type = "_https._tcp.local.";
                } else {
                    values.put("type", "fdroidrepo");
                    type = "_http._tcp.local.";
                }
                try {
                    pairService = ServiceInfo.create(type, repoName, FDroidApp.port, 0, 0, values);
                    jmdns = JmDNS.create();
                    jmdns.registerService(pairService);
                } catch (IOException e) {
                    Log.e(TAG, "Error while registering jmdns service: " + e);
                    Log.e(TAG, Log.getStackTraceString(e));
                }
            }
        }).start();
    }

    private void unregisterMDNSService() {
        if (localRepoBonjourChangeListener != null) {
            Preferences.get().unregisterLocalRepoBonjourListeners(localRepoBonjourChangeListener);
            localRepoBonjourChangeListener = null;
        }
        clearCurrentMDNSService();
    }

    private void clearCurrentMDNSService() {
        if (jmdns != null) {
            if (pairService != null) {
                jmdns.unregisterService(pairService);
                pairService = null;
            }
            jmdns.unregisterAllServices();
            Utils.closeQuietly(jmdns);
            jmdns = null;
        }
    }
}
