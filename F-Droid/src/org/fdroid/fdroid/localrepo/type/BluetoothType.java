package org.fdroid.fdroid.localrepo.type;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.util.Log;

import org.fdroid.fdroid.localrepo.SwapService;
import org.fdroid.fdroid.net.bluetooth.BluetoothServer;

public class BluetoothType extends SwapType {

    private static final String TAG = "BluetoothBroadcastType";

    @NonNull
    private final BluetoothAdapter adapter;

    private final BluetoothServer server;

    public static SwapType create(@NonNull Context context) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            return new NoBluetoothType(context);
        } else {
            return new BluetoothType(context, adapter);
        }
    };

    private BluetoothType(@NonNull Context context, @NonNull BluetoothAdapter adapter) {
        super(context);
        this.adapter = adapter;
        this.server = new BluetoothServer(context, context.getFilesDir());

        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1)) {
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        setConnected(false);
                        break;

                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        setConnected(true);
                        break;

                    // Only other is BluetoothAdapter.SCAN_MODE_CONNECTABLE. For now don't handle that.
                }
            }
        }, new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
    }

    @Override
    public void start() {
        if (server.isAlive()) {
            Log.d(TAG, "Attempting to start Bluetooth swap, but it appears to be running already.");
            return;
        }

        sendBroadcast(SwapService.EXTRA_STARTING);

        if (!adapter.isEnabled()) {
            if (!adapter.enable()) {
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
        if (server.isAlive()) {
            server.close();
            setConnected(false);
        } else {
            Log.i(TAG, "Attempting to stop Bluetooth swap, but it is not currently running.");
        }
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
    }
}
