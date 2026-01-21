package org.fdroid.fdroid.nearby;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import androidx.annotation.RequiresPermission;

import java.io.IOException;

public class BluetoothClient {
    private static final String TAG = "BluetoothClient";

    private final BluetoothDevice device;

    public BluetoothClient(String macAddress) {
        device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress);
    }

    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    public BluetoothConnection openConnection() throws IOException {

        BluetoothConnection connection = null;
        try {
            BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(BluetoothConstants.fdroidUuid());
            connection = new BluetoothConnection(socket);
            connection.open();
            return connection;
        } finally {
            if (connection != null) {
                connection.closeQuietly();
            }
        }
    }
}
