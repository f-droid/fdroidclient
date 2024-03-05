package org.fdroid.fdroid.nearby;

import android.bluetooth.BluetoothSocket;

import androidx.annotation.RequiresPermission;

import org.fdroid.fdroid.Utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothConnection {

    private static final String TAG = "BluetoothConnection";

    private InputStream input;
    private OutputStream output;
    private final BluetoothSocket socket;

    BluetoothConnection(BluetoothSocket socket) {
        this.socket = socket;
    }

    public InputStream getInputStream() {
        return input;
    }

    public OutputStream getOutputStream() {
        return output;
    }

    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    public void open() throws IOException {
        if (!socket.isConnected()) {
            // Server sockets will already be connected when they are passed to us,
            // client sockets require us to call connect().
            socket.connect();
        }

        input = new BufferedInputStream(socket.getInputStream());
        output = new BufferedOutputStream(socket.getOutputStream());
        Utils.debugLog(TAG, "Opened connection to Bluetooth device");
    }

    public void closeQuietly() {
        Utils.closeQuietly(input);
        Utils.closeQuietly(output);
        Utils.closeQuietly(socket);
    }

    public void close() {
        closeQuietly();
    }
}
