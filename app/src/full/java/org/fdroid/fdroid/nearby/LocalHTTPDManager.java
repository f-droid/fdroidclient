package org.fdroid.fdroid.nearby;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;

import java.io.IOException;
import java.net.BindException;

/**
 * Manage {@link LocalHTTPD} in a {@link HandlerThread};
 */
public class LocalHTTPDManager {
    private static final String TAG = "LocalHTTPDManager";

    public static final String ACTION_STARTED = "LocalHTTPDStarted";
    public static final String ACTION_STOPPED = "LocalHTTPDStopped";
    public static final String ACTION_ERROR = "LocalHTTPDError";

    private static final int STOP = 5709;

    private static Handler handler;
    private static volatile HandlerThread handlerThread;
    private static LocalHTTPD localHttpd;

    public static void start(Context context) {
        start(context, Preferences.get().isLocalRepoHttpsEnabled());
    }

    /**
     * Testable version, not for regular use.
     *
     * @see #start(Context)
     */
    static void start(final Context context, final boolean useHttps) {
        if (handlerThread != null && handlerThread.isAlive()) {
            Log.w(TAG, "handlerThread is already running, doing nothing!");
            return;
        }

        handlerThread = new HandlerThread("LocalHTTPD", Process.THREAD_PRIORITY_LESS_FAVORABLE) {
            @Override
            protected void onLooperPrepared() {
                localHttpd = new LocalHTTPD(
                        context,
                        FDroidApp.ipAddressString,
                        FDroidApp.port,
                        context.getFilesDir(),
                        useHttps);
                try {
                    localHttpd.start();
                    Intent intent = new Intent(ACTION_STARTED);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                } catch (BindException e) {
                    FDroidApp.generateNewPort = true;
                    WifiStateChangeService.start(context, null);
                    Intent intent = new Intent(ACTION_ERROR);
                    intent.putExtra(Intent.EXTRA_TEXT,
                            "port " + FDroidApp.port + " occupied, trying new port: ("
                                    + e.getLocalizedMessage() + ")");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                } catch (IOException e) {
                    e.printStackTrace();
                    Intent intent = new Intent(ACTION_ERROR);
                    intent.putExtra(Intent.EXTRA_TEXT, e.getLocalizedMessage());
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                }
            }
        };
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                localHttpd.stop();
                handlerThread.quit();
                handlerThread = null;
            }
        };
    }

    public static void stop(Context context) {
        if (handler == null || handlerThread == null || !handlerThread.isAlive()) {
            Log.w(TAG, "handlerThread is already stopped, doing nothing!");
            handlerThread = null;
            return;
        }
        handler.sendEmptyMessage(STOP);
        Intent stoppedIntent = new Intent(ACTION_STOPPED);
        LocalBroadcastManager.getInstance(context).sendBroadcast(stoppedIntent);
    }

    /**
     * Run {@link #stop(Context)}, wait for it to actually stop, then run
     * {@link #start(Context)}.
     */
    public static void restart(Context context) {
        restart(context, Preferences.get().isLocalRepoHttpsEnabled());
    }

    /**
     * Testable version, not for regular use.
     *
     * @see #restart(Context)
     */
    static void restart(Context context, boolean useHttps) {
        stop(context);
        try {
            handlerThread.join(10000);
        } catch (InterruptedException | NullPointerException e) {
            // ignored
        }
        start(context, useHttps);
    }

    public static boolean isAlive() {
        return handlerThread != null && handlerThread.isAlive();
    }
}
