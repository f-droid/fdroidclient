package org.fdroid.fdroid.localrepo.type;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.localrepo.SwapService;
import org.fdroid.fdroid.net.bluetooth.BluetoothServer;

@SuppressWarnings("LineLength")
public class BluetoothSwap {

    private static final String TAG = "BluetoothSwap";
    public static final String BLUETOOTH_NAME_TAG = "FDroid:";

    private static BluetoothSwap mInstance;

    @NonNull
    private final BluetoothAdapter adapter;
    private boolean isDiscoverable;

    private boolean isConnected;

    @NonNull
    protected final Context context;

    @Nullable
    private BluetoothServer server;

    private String deviceBluetoothName;

    public static BluetoothSwap create(@NonNull Context context) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            return new NoBluetoothType(context);
        }
        if (mInstance == null) {
            mInstance = new BluetoothSwap(context, adapter);
        }

        return mInstance;
    }

    private BluetoothSwap(@NonNull Context context, @NonNull BluetoothAdapter adapter) {
        this.context = context;
        this.adapter = adapter;
    }

    public boolean isDiscoverable() {
        return isDiscoverable;
    }

    public boolean isConnected() {
        return server != null && server.isRunning() && isConnected;
    }

    public synchronized void start() {
        if (isConnected()) {
            Utils.debugLog(TAG, "already running, quitting start()");
            return;
        }

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1)) {
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        setConnected(false);
                        break;

                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        isDiscoverable = true;
                        if (server != null && server.isRunning()) {
                            setConnected(true);
                        }
                        break;

                    // Only other is BluetoothAdapter.SCAN_MODE_CONNECTABLE. For now don't handle that.
                }
            }
        };
        context.registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));

        /*
        if (server != null) {
            Utils.debugLog(TAG, "Attempting to start Bluetooth swap, but it appears to be running already. Will cancel it so it can be restarted.");
            server.close();
            server = null;
        }*/

        if (server == null) {
            server = new BluetoothServer(this, context.getFilesDir());
        }

        sendBroadcast(SwapService.EXTRA_STARTING);

        //store the original bluetoothname, and update this one to be unique
        deviceBluetoothName = adapter.getName();

        /*
        Utils.debugLog(TAG, "Prefixing Bluetooth adapter name with " + BLUETOOTH_NAME_TAG + " to make it identifiable as a swap device.");
        if (!deviceBluetoothName.startsWith(BLUETOOTH_NAME_TAG)) {
            adapter.setName(BLUETOOTH_NAME_TAG + deviceBluetoothName);
        }

        if (!adapter.getName().startsWith(BLUETOOTH_NAME_TAG)) {
            Log.e(TAG, "Couldn't change the name of the Bluetooth adapter, it will not get recognized by other swap clients.");
            // TODO: Should we bail here?
        }*/

        if (!adapter.isEnabled()) {
            Utils.debugLog(TAG, "Bluetooth adapter is disabled, attempting to enable.");
            if (!adapter.enable()) {
                Utils.debugLog(TAG, "Could not enable Bluetooth adapter, so bailing out of Bluetooth swap.");
                setConnected(false);
                return;
            }
        }

        if (adapter.isEnabled()) {
            setConnected(true);
        } else {
            Log.i(TAG, "Didn't start Bluetooth swapping server, because Bluetooth is disabled and couldn't be enabled.");
            setConnected(false);
        }
    }

    public void stop() {
        if (server != null && server.isAlive()) {
            server.close();
            setConnected(false);
        } else {
            Log.i(TAG, "Attempting to stop Bluetooth swap, but it is not currently running.");
        }
    }

    protected void onStopped() {
        Utils.debugLog(TAG, "Resetting bluetooth device name to " + deviceBluetoothName + " after swapping.");
        adapter.setName(deviceBluetoothName);
    }

    protected String getBroadcastAction() {
        return SwapService.BLUETOOTH_STATE_CHANGE;
    }

    protected final void setConnected(boolean connected) {
        if (connected) {
            isConnected = true;
            sendBroadcast(SwapService.EXTRA_STARTED);
        } else {
            isConnected = false;
            sendBroadcast(SwapService.EXTRA_STOPPED);
        }
    }

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

    public void startInBackground() {
        // TODO switch to thread which is killed if still running, like WiFiStateChangeService
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                start();
                return null;
            }
        }.execute();
    }


    private static class NoBluetoothType extends BluetoothSwap {

        NoBluetoothType(@NonNull Context context) {
            super(context, null);
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

        @Override
        protected String getBroadcastAction() {
            return null;
        }

    }
}
