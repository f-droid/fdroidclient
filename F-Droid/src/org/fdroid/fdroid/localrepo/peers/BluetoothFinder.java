package org.fdroid.fdroid.localrepo.peers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

// TODO: Still to be implemented
public class BluetoothFinder extends PeerFinder<BluetoothPeer> {

    private static final String TAG = "BluetoothFinder";

    private final Context context;
    private final BluetoothAdapter adapter;

    public BluetoothFinder(Context context) {
        this.context = context;
        adapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public void scan() {

        if (adapter == null) {
            Log.i(TAG, "Not scanning for bluetooth peers to swap with, couldn't find a bluetooth adapter on this device.");
            return;
        }

        final BroadcastReceiver deviceFoundReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    onDeviceFound(device);
                }
            }
        };

        final BroadcastReceiver scanCompleteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Scan complete: " + intent.getAction());
            }
        };

        context.registerReceiver(deviceFoundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        context.registerReceiver(scanCompleteReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

        if (!adapter.startDiscovery()) {
            Log.e(TAG, "Couldn't start bluetooth scanning.");
        }

    }

    @Override
    public void cancel() {
        if (adapter != null) {
            Log.d(TAG, "Stopping bluetooth discovery.");
            adapter.cancelDiscovery();
        }
    }

    private void onDeviceFound(BluetoothDevice device) {
        foundPeer(new BluetoothPeer(device));
    }

}
