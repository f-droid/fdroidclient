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

        BluetoothSocket socket = null;
        BluetoothConnection connection = null;
        try {
            socket = device.createInsecureRfcommSocketToServiceRecord(BluetoothConstants.fdroidUuid());
            connection = new BluetoothConnection(socket);
            connection.open();
            return connection;
        } catch (IOException e1) {

            if (connection != null) {
                connection.closeQuietly();
            }

            throw e1;

            /*
            Log.e(TAG, "There was an error while establishing Bluetooth connection. Falling back to reflection");
            Class<?> clazz = socket.getRemoteDevice().getClass();
            Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};

            Method method;
            try {
                method = clazz.getMethod("createInsecureRfcommSocket", paramTypes);
                Object[] params = new Object[]{1};
                BluetoothSocket sockFallback = (BluetoothSocket) method.invoke(socket.getRemoteDevice(), params);

                BluetoothConnection connection = new BluetoothConnection(sockFallback);
                connection.open();
                return connection;
            } catch (NoSuchMethodException e) {
                throw e1;
            } catch (IllegalAccessException e) {
                throw e1;
            } catch (InvocationTargetException e) {
                throw e1;
            }*/

            // Don't catch exceptions this time, let it bubble up as we did our best but don't
            // have anythign else to offer in terms of resolving the problem right now.
        }
    }

}
