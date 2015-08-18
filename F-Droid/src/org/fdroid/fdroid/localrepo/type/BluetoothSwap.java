package org.fdroid.fdroid.localrepo.type;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.fdroid.fdroid.localrepo.SwapService;
import org.fdroid.fdroid.net.bluetooth.BluetoothServer;

public class BluetoothSwap extends SwapType {

    private static final String TAG = "BluetoothBroadcastType";
    public final static String BLUETOOTH_NAME_TAG = "FDroid:";

    @NonNull
    private final BluetoothAdapter adapter;

    @Nullable
    private BluetoothServer server;

    private String deviceBluetoothName = null;

    public static SwapType create(@NonNull Context context) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            return new NoBluetoothType(context);
        } else {
            return new BluetoothSwap(context, adapter);
        }
    }

    private BluetoothSwap(@NonNull Context context, @NonNull BluetoothAdapter adapter) {
        super(context);
        this.adapter = adapter;

        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1)) {
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        setConnected(false);
                        break;

                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        if (server != null && server.isRunning()) {
                            setConnected(true);
                        }
                        break;

                    // Only other is BluetoothAdapter.SCAN_MODE_CONNECTABLE. For now don't handle that.
                }
            }
        }, new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
    }

    @Override
    public boolean isConnected() {
        return server != null && server.isRunning() && super.isConnected();
    }

    @Override
    public void start() {
        if (server != null) {
            Log.d(TAG, "Attempting to start Bluetooth swap, but it appears to be running already. Will cancel it so it can be restarted.");
            server.close();
            server = null;
        }

        server = new BluetoothServer(this, context.getFilesDir());

        sendBroadcast(SwapService.EXTRA_STARTING);

        //store the original bluetoothname, and update this one to be unique
        deviceBluetoothName = adapter.getName();

        Log.d(TAG, "Prefixing Bluetooth adapter name with " + BLUETOOTH_NAME_TAG + " to make it identifiable as a swap device.");
        if (!deviceBluetoothName.startsWith(BLUETOOTH_NAME_TAG))
            adapter.setName(BLUETOOTH_NAME_TAG + deviceBluetoothName);

        if (!adapter.getName().startsWith(BLUETOOTH_NAME_TAG)) {
            Log.e(TAG, "Couldn't change the name of the Bluetooth adapter, it will not get recognized by other swap clients.");
            // TODO: Should we bail here?
        }

        if (!adapter.isEnabled()) {
            Log.d(TAG, "Bluetooth adapter is disabled, attempting to enable.");
            if (!adapter.enable()) {
                Log.d(TAG, "Could not enable Bluetooth adapter, so bailing out of Bluetooth swap.");
                setConnected(false);
                return;
            }
        }

        if (adapter.isEnabled()) {
            server.start();
            setConnected(true);
        } else {
            Log.i(TAG, "Didn't start Bluetooth swapping server, because Bluetooth is disabled and couldn't be enabled.");
            setConnected(false);
        }
    }

    @Override
    public void stop() {
        if (server != null && server.isAlive()) {
            server.close();
            setConnected(false);
        } else {
            Log.i(TAG, "Attempting to stop Bluetooth swap, but it is not currently running.");
        }
    }

    protected void onStopped() {
        Log.d(TAG, "Resetting bluetooth device name to " + deviceBluetoothName + " after swapping.");
        adapter.setName(deviceBluetoothName);
    }

    @Override
    public String getBroadcastAction() {
        return SwapService.BLUETOOTH_STATE_CHANGE;
    }

    private static class NoBluetoothType extends SwapType {

        public NoBluetoothType(@NonNull Context context) {
            super(context);
        }

        @Override
        public void start() {}

        @Override
        public void stop() {}

        @Override
        protected String getBroadcastAction() {
            return null;
        }
    }
}
