package org.fdroid.fdroid.net.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import org.fdroid.fdroid.Utils;

import java.io.*;
import java.util.UUID;

public class BluetoothClient {

    private static final String TAG = "org.fdroid.fdroid.net.bluetooth.BluetoothClient";

    private BluetoothAdapter adapter;
    private BluetoothDevice device;

    public BluetoothClient(BluetoothAdapter adapter) {
        this.adapter = adapter;
    }

    public void pairWithDevice() throws IOException {

        if (adapter.getBondedDevices().size() == 0) {
            throw new IOException("No paired Bluetooth devices.");
        }
        
        // TODO: Don't just take a random bluetooth device :)
        device = adapter.getBondedDevices().iterator().next();

    }

    public Connection openConnection() throws IOException {
        return new Connection();
    }

    public class Connection {

        private InputStream input = null;
        private OutputStream output = null;

        private BluetoothSocket socket;

        private Connection() throws IOException {
            Log.d(TAG, "Attempting to create connection to Bluetooth device '" + device.getName() + "'...");
            socket = device.createRfcommSocketToServiceRecord(UUID.fromString(BluetoothConstants.fdroidUuid()));
        }

        public InputStream getInputStream() {
            return input;
        }

        public OutputStream getOutputStream() {
            return output;
        }

        public void open() throws IOException {
            socket.connect();
            input  = socket.getInputStream();
            output = socket.getOutputStream();
            Log.d(TAG, "Opened connection to Bluetooth device '" + device.getName() + "'");
        }

        public void closeQuietly() {
            Utils.closeQuietly(input);
            Utils.closeQuietly(output);
            Utils.closeQuietly(socket);
        }

        public void close() throws IOException {
            if (input == null || output == null) {
                throw new RuntimeException("Cannot close() a BluetoothConnection before calling open()" );
            }

            input.close();
            output.close();
            socket.close();
        }
    }
}
