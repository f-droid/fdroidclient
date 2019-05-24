package org.fdroid.fdroid.localrepo;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.localrepo.peers.BonjourPeer;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.util.HashMap;

/**
 * Manage {@link JmDNS} in a {@link HandlerThread}.  The start process is in
 * {@link HandlerThread#onLooperPrepared()} so that it is always started before
 * any messages get delivered from the queue.
 */
public class BonjourManager {
    private static final String TAG = "BonjourManager";

    public static final String ACTION_FOUND = "BonjourNewPeer";
    public static final String ACTION_REMOVED = "BonjourPeerRemoved";
    public static final String EXTRA_BONJOUR_PEER = "extraBonjourPeer";

    public static final String ACTION_STATUS = "BonjourStatus";
    public static final String EXTRA_STATUS = "BonjourStatusExtra";
    public static final int STATUS_STARTING = 0;
    public static final int STATUS_STARTED = 1;
    public static final int STATUS_STOPPING = 2;
    public static final int STATUS_STOPPED = 3;
    public static final int STATUS_VISIBLE = 4;
    public static final int STATUS_NOT_VISIBLE = 5;
    public static final int STATUS_ERROR = 0xffff;

    public static final String HTTP_SERVICE_TYPE = "_http._tcp.local.";
    public static final String HTTPS_SERVICE_TYPE = "_https._tcp.local.";

    private static final int STOP = 5709;
    private static final int VISIBLE = 4151873;
    private static final int NOT_VISIBLE = 144151873;

    private static WeakReference<Context> context;
    private static Handler handler;
    private static volatile HandlerThread handlerThread;
    private static ServiceInfo pairService;
    private static JmDNS jmdns;
    private static WifiManager.MulticastLock multicastLock;

    public static boolean isAlive() {
        return handlerThread != null && handlerThread.isAlive();
    }

    /**
     * Stops the Bonjour/mDNS, triggering a status broadcast via {@link #ACTION_STATUS}.
     * {@link #STATUS_STOPPED} can be broadcast multiple times for the same session,
     * so make sure {@link android.content.BroadcastReceiver}s handle duplicates.
     */
    public static void stop(Context context) {
        BonjourManager.context = new WeakReference<>(context);
        if (handler == null || handlerThread == null || !handlerThread.isAlive()) {
            sendBroadcast(STATUS_STOPPED, null);
            return;
        }
        sendBroadcast(STATUS_STOPPING, null);
        handler.sendEmptyMessage(STOP);
    }

    public static void setVisible(Context context, boolean visible) {
        BonjourManager.context = new WeakReference<>(context);
        if (handler == null || handlerThread == null || !handlerThread.isAlive()) {
            Log.e(TAG, "handlerThread is stopped, not changing visibility!");
            return;
        }
        if (visible) {
            handler.sendEmptyMessage(VISIBLE);
        } else {
            handler.sendEmptyMessage(NOT_VISIBLE);
        }
    }

    /**
     * Starts the service, triggering a status broadcast via {@link #ACTION_STATUS}.
     * {@link #STATUS_STARTED} can be broadcast multiple times for the same session,
     * so make sure {@link android.content.BroadcastReceiver}s handle duplicates.
     */
    public static void start(Context context) {
        start(context,
                Preferences.get().getLocalRepoName(),
                Preferences.get().isLocalRepoHttpsEnabled(),
                httpServiceListener, httpsServiceListener);
    }

    /**
     * Testable version, not for regular use.
     *
     * @see #start(Context)
     */
    static void start(final Context context,
                      final String localRepoName, final boolean useHttps,
                      final ServiceListener httpServiceListener, final ServiceListener httpsServiceListener) {
        BonjourManager.context = new WeakReference<>(context);
        if (handlerThread != null && handlerThread.isAlive()) {
            sendBroadcast(STATUS_STARTED, null);
            return;
        }
        sendBroadcast(STATUS_STARTING, null);

        final WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        handlerThread = new HandlerThread("BonjourManager", Process.THREAD_PRIORITY_LESS_FAVORABLE) {
            @Override
            protected void onLooperPrepared() {
                try {
                    InetAddress address = InetAddress.getByName(FDroidApp.ipAddressString);
                    jmdns = JmDNS.create(address);
                    jmdns.addServiceListener(HTTP_SERVICE_TYPE, httpServiceListener);
                    jmdns.addServiceListener(HTTPS_SERVICE_TYPE, httpsServiceListener);

                    multicastLock = wifiManager.createMulticastLock(context.getPackageName());
                    multicastLock.setReferenceCounted(false);
                    multicastLock.acquire();

                    sendBroadcast(STATUS_STARTED, null);
                } catch (IOException e) {
                    if (handler != null) {
                        handler.removeMessages(VISIBLE);
                        handler.sendMessageAtFrontOfQueue(handler.obtainMessage(STOP));
                    }
                    Log.e(TAG, "Error while registering jmdns service", e);
                    sendBroadcast(STATUS_ERROR, e.getLocalizedMessage());
                }
            }
        };
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper()) {

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case VISIBLE:
                        handleVisible(localRepoName, useHttps);
                        break;
                    case NOT_VISIBLE:
                        handleNotVisible();
                        break;
                    case STOP:
                        handleStop();
                        break;
                }
            }

            private void handleVisible(String localRepoName, boolean useHttps) {
                HashMap<String, String> values = new HashMap<>();
                values.put(BonjourPeer.PATH, "/fdroid/repo");
                values.put(BonjourPeer.NAME, localRepoName);
                values.put(BonjourPeer.FINGERPRINT, FDroidApp.repo.fingerprint);
                String type;
                if (useHttps) {
                    values.put(BonjourPeer.TYPE, "fdroidrepos");
                    type = HTTPS_SERVICE_TYPE;
                } else {
                    values.put(BonjourPeer.TYPE, "fdroidrepo");
                    type = HTTP_SERVICE_TYPE;
                }
                ServiceInfo newPairService = ServiceInfo.create(type, localRepoName, FDroidApp.port, 0, 0, values);
                if (!newPairService.equals(pairService)) try {
                    if (pairService != null) {
                        jmdns.unregisterService(pairService);
                    }
                    jmdns.registerService(newPairService);
                    pairService = newPairService;
                } catch (IOException e) {
                    e.printStackTrace();
                    sendBroadcast(STATUS_ERROR, e.getLocalizedMessage());
                    return;
                }
                sendBroadcast(STATUS_VISIBLE, null);
            }

            private void handleNotVisible() {
                if (pairService != null) {
                    jmdns.unregisterService(pairService);
                    pairService = null;
                }
                sendBroadcast(STATUS_NOT_VISIBLE, null);
            }

            private void handleStop() {
                if (multicastLock != null) {
                    multicastLock.release();
                }
                if (jmdns != null) {
                    jmdns.unregisterAllServices();
                    Utils.closeQuietly(jmdns);
                    pairService = null;
                    jmdns = null;
                }
                handlerThread.quit();
                handlerThread = null;
                sendBroadcast(STATUS_STOPPED, null);
            }

        };
    }

    public static void restart(Context context) {
        restart(context,
                Preferences.get().getLocalRepoName(),
                Preferences.get().isLocalRepoHttpsEnabled(),
                httpServiceListener, httpsServiceListener);
    }

    /**
     * Testable version, not for regular use.
     *
     * @see #restart(Context)
     */
    static void restart(final Context context,
                        final String localRepoName, final boolean useHttps,
                        final ServiceListener httpServiceListener, final ServiceListener httpsServiceListener) {
        stop(context);
        try {
            handlerThread.join(10000);
        } catch (InterruptedException | NullPointerException e) {
            // ignored
        }
        start(context, localRepoName, useHttps, httpServiceListener, httpsServiceListener);
    }

    private static void sendBroadcast(String action, ServiceInfo serviceInfo) {
        BonjourPeer bonjourPeer = BonjourPeer.getInstance(serviceInfo);
        if (bonjourPeer == null) {
            Utils.debugLog(TAG, "IGNORING: " + serviceInfo);
            return;
        }
        Intent intent = new Intent(action);
        intent.putExtra(EXTRA_BONJOUR_PEER, bonjourPeer);
        LocalBroadcastManager.getInstance(context.get()).sendBroadcast(intent);
    }

    private static void sendBroadcast(int status, String message) {

        Intent intent = new Intent(ACTION_STATUS);
        intent.putExtra(EXTRA_STATUS, status);
        if (!TextUtils.isEmpty(message)) {
            intent.putExtra(Intent.EXTRA_TEXT, message);
        }
        LocalBroadcastManager.getInstance(context.get()).sendBroadcast(intent);
    }

    private static final ServiceListener httpServiceListener = new SwapServiceListener();
    private static final ServiceListener httpsServiceListener = new SwapServiceListener();

    private static class SwapServiceListener implements ServiceListener {
        @Override
        public void serviceAdded(ServiceEvent serviceEvent) {
            // ignored, we only need resolved info
        }

        @Override
        public void serviceRemoved(ServiceEvent serviceEvent) {
            sendBroadcast(ACTION_REMOVED, serviceEvent.getInfo());
        }

        @Override
        public void serviceResolved(ServiceEvent serviceEvent) {
            sendBroadcast(ACTION_FOUND, serviceEvent.getInfo());
        }
    }
}
