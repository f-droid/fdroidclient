package org.fdroid.fdroid.net.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.net.bluetooth.httpish.Request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Act as a layer on top of LocalHTTPD server, by forwarding requests served
 * over bluetooth to that server.
 */
public class BluetoothServer extends Thread {

    private static final String TAG = "org.fdroid.fdroid.net.bluetooth.BluetoothServer";

    private BluetoothServerSocket serverSocket;

    private List<Connection> clients = new ArrayList<Connection>();

    public void close() {

        for (Connection connection : clients) {
            connection.interrupt();
        }

        if (serverSocket != null) {
            Utils.closeQuietly(serverSocket);
        }

    }

    @Override
    public void run() {

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        try {
            serverSocket = adapter.listenUsingRfcommWithServiceRecord("FDroid App Swap", BluetoothConstants.fdroidUuid());
        } catch (IOException e) {
            Log.e(TAG, "Error starting Bluetooth server socket, will stop the server now - " + e.getMessage());
            return;
        }

        while (true) {
            try {
                BluetoothSocket clientSocket = serverSocket.accept();
                if (clientSocket != null && !isInterrupted()) {
                    Connection client = new Connection(clientSocket);
                    client.start();
                    clients.add(client);
                } else {
                    break;
                }
            } catch (IOException e) {
                Log.e(TAG, "Error receiving client connection over Bluetooth server socket, will continue listening for other clients - " + e.getMessage());
            }
        }

    }

    private static class Connection extends Thread
    {

        private static final String TAG = "org.fdroid.fdroid.net.bluetooth.BluetoothServer.Connection";
        private BluetoothSocket socket;

        public Connection(BluetoothSocket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {

            Log.d(TAG, "Listening for incoming Bluetooth requests from client");

            BluetoothConnection connection;
            try {
                connection = new BluetoothConnection(socket);
            } catch (IOException e) {
                Log.e(TAG, "Error listening for incoming connections over bluetooth - " + e.getMessage());
                return;
            }

            while (true) {

                try {
                    Log.d(TAG, "Listening for new Bluetooth request from client.");
                    Request incomingRequest = Request.listenForRequest(connection);
                    handleRequest(incomingRequest);
                } catch (IOException e) {
                    Log.e(TAG, "Error receiving incoming connection over bluetooth - " + e.getMessage());
                }

                if (isInterrupted())
                    break;

            }

        }

        private void handleRequest(Request request) {

            Log.d(TAG, "Received Bluetooth request from client, will process it now.");

        }
    }
}
