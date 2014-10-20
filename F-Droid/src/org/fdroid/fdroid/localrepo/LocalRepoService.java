package org.fdroid.fdroid.localrepo;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.net.LocalHTTPD;
import org.fdroid.fdroid.net.WifiStateChangeService;
import org.fdroid.fdroid.views.swap.SwapActivity;

import java.io.IOException;
import java.net.BindException;
import java.util.Random;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

public abstract class LocalRepoService extends Service {

    private static final String TAG = "LocalRepoService";

    public static final String STATE = "org.fdroid.fdroid.action.LOCAL_REPO_STATE";
    public static final String STARTED = "org.fdroid.fdroid.category.LOCAL_REPO_STARTED";
    public static final String STOPPED = "org.fdroid.fdroid.category.LOCAL_REPO_STOPPED";

    private NotificationManager notificationManager;
    private Notification notification;
    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private final int NOTIFICATION = R.string.local_repo_running;

    private Handler webServerThreadHandler = null;
    protected LocalHTTPD localHttpd;

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

    private void showNotification() {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // launch LocalRepoActivity if the user selects this notification
        Intent intent = new Intent(this, SwapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        notification = new NotificationCompat.Builder(this)
                .setContentTitle(getText(R.string.local_repo_running))
                .setContentText(getText(R.string.touch_to_configure_local_repo))
                .setSmallIcon(R.drawable.ic_swap)
                .setContentIntent(contentIntent)
                .build();
        startForeground(NOTIFICATION, notification);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        showNotification();
        startNetworkServices();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        new Thread() {
            public void run() {
                stopNetworkServices();
            }
        }.start();

        notificationManager.cancel(NOTIFICATION);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    /**
     * Called immediately _after_ the webserver is started.
     */
    protected abstract void onStartNetworkServices();

    /**
     * Called immediately _before_ the webserver is stopped.
     */
    protected abstract void onStopNetworkServices();

    /**
     * Whether or not this particular version of LocalRepoService requires a HTTPS
     * connection. In the local proxy instance, it will not require it, but in the
     * wifi setting, it should use whatever preference the user selected.
     */
    protected abstract boolean useHttps();

    protected void startNetworkServices() {
        Log.d(TAG, "Starting local repo network services");
        startWebServer();

        onStartNetworkServices();
    }

    protected void stopNetworkServices() {
        onStopNetworkServices();

        Log.d(TAG, "Stopping web server...");
        stopWebServer();
    }

    protected abstract String getIpAddressToBindTo();
    protected abstract int getPortToBindTo();

    protected void startWebServer() {
        Runnable webServer = new Runnable() {
            // Tell Eclipse this is not a leak because of Looper use.
            @SuppressLint("HandlerLeak")
            @Override
            public void run() {
                localHttpd = new LocalHTTPD(
                        LocalRepoService.this,
                        getIpAddressToBindTo(),
                        getPortToBindTo(),
                        getFilesDir(),
                        useHttps());

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

}

