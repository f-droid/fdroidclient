package org.fdroid.fdroid.localrepo.type;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.util.Log;

import org.fdroid.fdroid.localrepo.SwapService;

public class BluetoothType extends SwapType {

    private static final String TAG = "BluetoothBroadcastType";

    @NonNull
    private final BluetoothAdapter adapter;

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
        sendBroadcast(SwapService.EXTRA_STARTING);

        if (!adapter.isEnabled()) {
            if (!adapter.enable()) {
                setConnected(false);
                return;
            }
        }

        if (adapter.isEnabled()) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);

            // TODO: Hmm, don't like the idea of a background service being able to do this :(
            discoverableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Can't get notified if they cancel this, because we are not an activity and thus
            // can't start an activity for a result :(
            context.startActivity(discoverableIntent);

            // Don't setConnected(true) yet, wait for the broadcast to come from the BluetoothAdapter
            // saying its state has changed.
        }
    }

    @Override
    public void stop() {
        Log.d(TAG, "Making bluetooth non-discoverable.");
        adapter.cancelDiscovery();
        setConnected(false);
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
