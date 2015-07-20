package org.fdroid.fdroid.net.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;

public class BluetoothClient {

    @SuppressWarnings("unused")
    private static final String TAG = "BluetoothClient";

    private final BluetoothDevice device;

    public BluetoothClient(BluetoothDevice device) {
        this.device = device;
    }

    public BluetoothClient(String macAddress) {
        device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress);
    }

    public BluetoothConnection openConnection() throws IOException {
        BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(BluetoothConstants.fdroidUuid());
        BluetoothConnection connection = new BluetoothConnection(socket);
        connection.open();
        return connection;
    }

}
