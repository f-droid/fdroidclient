package org.fdroid.fdroid.net.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;

public class BluetoothClient {

    private static final String TAG = "org.fdroid.fdroid.net.bluetooth.BluetoothClient";

    private BluetoothDevice device;

    public BluetoothClient(BluetoothDevice device) {
        this.device = device;
    }

    public BluetoothConnection openConnection() throws IOException {
        BluetoothSocket socket = device.createRfcommSocketToServiceRecord(BluetoothConstants.fdroidUuid());
        BluetoothConnection connection = new BluetoothConnection(socket);
        connection.open();
        return connection;
    }

}
