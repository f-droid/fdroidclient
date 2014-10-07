package org.fdroid.fdroid.net.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import java.io.IOException;

public class BluetoothClient {

    private static final String TAG = "org.fdroid.fdroid.net.bluetooth.BluetoothClient";

    private final BluetoothAdapter adapter;
    private BluetoothDevice device;

    public BluetoothClient() {
        this.adapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void pairWithDevice() throws IOException {

        if (adapter.getBondedDevices().size() == 0) {
            throw new IOException("No paired Bluetooth devices.");
        }
        
        // TODO: Don't just take a random bluetooth device :)

        device = adapter.getBondedDevices().iterator().next();
        device.createRfcommSocketToServiceRecord(BluetoothConstants.fdroidUuid());

    }

    public BluetoothConnection openConnection() throws IOException {
        return null;
        // return new BluetoothConnection();
    }

}
