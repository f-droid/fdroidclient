package org.fdroid.fdroid.net.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;

public class BluetoothClient {

    @SuppressWarnings("unused")
    private static final String TAG = "BluetoothClient";

    private BluetoothDevice device;

    public BluetoothClient(BluetoothDevice device) {
        this.device = device;
    }

    public BluetoothConnection openConnection() throws IOException {
        BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(BluetoothConstants.fdroidUuid());
        BluetoothConnection connection = new BluetoothConnection(socket);
        connection.open();
        return connection;
    }

}
