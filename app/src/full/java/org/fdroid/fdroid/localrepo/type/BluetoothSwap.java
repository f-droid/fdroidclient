package org.fdroid.fdroid.localrepo.type;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.localrepo.SwapService;
import org.fdroid.fdroid.net.bluetooth.BluetoothServer;

@SuppressWarnings("LineLength")
public final class BluetoothSwap extends SwapType {

    private static final String TAG = "BluetoothSwap";
    public static final String BLUETOOTH_NAME_TAG = "FDroid:";

    private static BluetoothSwap mInstance;

    @NonNull
    private final BluetoothAdapter adapter;
    private boolean isDiscoverable;

    @Nullable
    private BluetoothServer server;

    private String deviceBluetoothName;

    public static SwapType create(@NonNull Context context) {
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
        super(context);
        this.adapter = adapter;
    }

    @Override
    public boolean isDiscoverable() {
        return isDiscoverable;
    }

    @Override
    public boolean isConnected() {
        return server != null && server.isRunning() && super.isConnected();
    }

    @Override
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

    /**
     * Don't try to start BT in the background. you can only start/stop a BT server once, else new connections don't work.
     */
    @Override
    public void stopInBackground() {
        stop();
    }

    @Override
    public void stop() {
        if (server != null && server.isAlive()) {
            server.close();
            setConnected(false);

            /*
            if (receiver != null) {
                context.unregisterReceiver(receiver);
                receiver = null;
            }
            */
        } else {
            Log.i(TAG, "Attempting to stop Bluetooth swap, but it is not currently running.");
        }
    }

    protected void onStopped() {
        Utils.debugLog(TAG, "Resetting bluetooth device name to " + deviceBluetoothName + " after swapping.");
        adapter.setName(deviceBluetoothName);
    }

    @Override
    public String getBroadcastAction() {
        return SwapService.BLUETOOTH_STATE_CHANGE;
    }

    private static class NoBluetoothType extends SwapType {

        NoBluetoothType(@NonNull Context context) {
            super(context);
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
