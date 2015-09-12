package org.fdroid.fdroid.localrepo.type;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.localrepo.SwapService;

/**
 * There is lots of common functionality, and a common API among different communication protocols
 * associated with the swap process. This includes Bluetooth visability, Bonjour visability,
 * and the web server which serves info for swapping. This class provides a common API for
 * starting and stopping these services. In addition, it helps with the process of sending broadcast
 * intents in response to the thing starting or stopping.
 */
public abstract class SwapType {

    private final static String TAG = "SwapType";

    private boolean isConnected;

    @NonNull
    protected final Context context;

    public SwapType(@NonNull Context context) {
        this.context = context;
    }

    abstract public void start();

    abstract public void stop();

    abstract protected String getBroadcastAction();

    public boolean isDiscoverable() {
        return isConnected();
    }

    protected final void setConnected(boolean connected) {
        if (connected) {
            isConnected = true;
            sendBroadcast(SwapService.EXTRA_STARTED);
        } else {
            isConnected = false;
            onStopped();
            sendBroadcast(SwapService.EXTRA_STOPPED);
        }
    }

    protected void onStopped() {}

    /**
     * Sends either a {@link org.fdroid.fdroid.localrepo.SwapService#EXTRA_STARTING},
     * {@link org.fdroid.fdroid.localrepo.SwapService#EXTRA_STARTED} or
     * {@link org.fdroid.fdroid.localrepo.SwapService#EXTRA_STOPPED} broadcast.
     */
    protected final void sendBroadcast(String extra) {
        if (getBroadcastAction() != null) {
            Intent intent = new Intent(getBroadcastAction());
            intent.putExtra(extra, true);
            Utils.debugLog(TAG, "Sending broadcast " + extra + " from " + getClass().getSimpleName());
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void startInBackground() {
        new Thread() {
            @Override
            public void run() {
                SwapType.this.start();
            }
        }.start();
    }

    public void ensureRunning() {
        if (!isConnected()) {
            start();
        }
    }

    public void ensureRunningInBackground() {
        new Thread() {
            @Override
            public void run() {
                ensureRunning();
            }
        }.start();
    }

    public void stopInBackground() {
        new Thread() {
            @Override
            public void run() {
                SwapType.this.stop();
            }
        }.run();
    }

}
