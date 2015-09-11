package org.fdroid.fdroid.localrepo.peers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class BluetoothFinder extends PeerFinder<BluetoothPeer> {

    private static final String TAG = "BluetoothFinder";

    public final static int DISCOVERABLE_TIMEOUT = 3600;

    private final BluetoothAdapter adapter;

    public BluetoothFinder(Context context) {
        super(context);
        adapter = BluetoothAdapter.getDefaultAdapter();
    }

    private BroadcastReceiver deviceFoundReceiver;
    private BroadcastReceiver scanCompleteReceiver;

    @Override
    public void scan() {

        if (adapter == null) {
            Log.i(TAG, "Not scanning for bluetooth peers to swap with, couldn't find a bluetooth adapter on this device.");
            return;
        }

        isScanning = true;

        if (deviceFoundReceiver == null) {
            deviceFoundReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        onDeviceFound(device);
                    }
                }
            };
            context.registerReceiver(deviceFoundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        }

        if (scanCompleteReceiver == null) {
            scanCompleteReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (isScanning) {
                        Log.d(TAG, "Scan complete, but we haven't been asked to stop scanning yet, so will restart scan.");
                        startDiscovery();
                    }
                }
            };

            // TODO: Unregister this receiver at the appropriate time.
            context.registerReceiver(scanCompleteReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        }

        startDiscovery();
    }

    private void startDiscovery() {

        if (adapter.isDiscovering()) {
            // TODO: Can we reset the discovering timeout, so that it doesn't, e.g. time out in 3
            // seconds because we had already almost completed the previous scan? We could
            // cancelDiscovery(), but then it will probably prompt the user again.
            Log.d(TAG, "Requested bluetooth scan when already scanning, so will ignore request.");
            return;
        }

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

        isScanning = false;
    }

    private void onDeviceFound(BluetoothDevice device) {

        if (device != null && device.getName() != null &&
                (device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA||
                device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA||
                device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.PHONE_SMART)) {
            foundPeer(new BluetoothPeer(device));
        }
    }

}
