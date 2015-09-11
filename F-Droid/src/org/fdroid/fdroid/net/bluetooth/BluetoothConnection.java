package org.fdroid.fdroid.net.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.util.Log;

import org.fdroid.fdroid.Utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothConnection {

    private static final String TAG = "BluetoothConnection";

    private InputStream input = null;
    private OutputStream output = null;
    protected final BluetoothSocket socket;

    public BluetoothConnection(BluetoothSocket socket) throws IOException {
        this.socket = socket;
    }

    public InputStream getInputStream() {
        return input;
    }

    public OutputStream getOutputStream() {
        return output;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void open() throws IOException {
        if (!socket.isConnected()) {
            // Server sockets will already be connected when they are passed to us,
            // client sockets require us to call connect().
            socket.connect();
        }

        input  = new BufferedInputStream(socket.getInputStream());
        output = new BufferedOutputStream(socket.getOutputStream());
        Log.d(TAG, "Opened connection to Bluetooth device");
    }

    public void closeQuietly() {
        Utils.closeQuietly(input);
        Utils.closeQuietly(output);
        Utils.closeQuietly(socket);
    }

    public void close() throws IOException {
        closeQuietly();
    }
}