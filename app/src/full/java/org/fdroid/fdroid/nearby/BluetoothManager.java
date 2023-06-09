package org.fdroid.fdroid.nearby;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.nearby.peers.BluetoothPeer;

import java.lang.ref.WeakReference;

/**
 * Manage the {@link android.bluetooth.BluetoothAdapter}in a {@link HandlerThread}.
 * The start process is in {@link HandlerThread#onLooperPrepared()} so that it is
 * always started before any messages get delivered from the queue.
 *
 * @see BonjourManager
 * @see LocalRepoManager
 */
public class BluetoothManager {
    private static final String TAG = "BluetoothManager";

    public static final String ACTION_FOUND = "BluetoothNewPeer";
    public static final String EXTRA_PEER = "extraBluetoothPeer";

    public static final String ACTION_STATUS = "BluetoothStatus";
    public static final String EXTRA_STATUS = "BluetoothStatusExtra";
    public static final int STATUS_STARTING = 0;
    public static final int STATUS_STARTED = 1;
    public static final int STATUS_STOPPING = 2;
    public static final int STATUS_STOPPED = 3;
    public static final int STATUS_ERROR = 0xffff;

    private static final int STOP = 5709;

    private static WeakReference<Context> context;
    private static Handler handler;
    private static volatile HandlerThread handlerThread;
    private static BluetoothAdapter bluetoothAdapter;

    /**
     * Stops the Bluetooth adapter, triggering a status broadcast via {@link #ACTION_STATUS}.
     * {@link #STATUS_STOPPED} can be broadcast multiple times for the same session,
     * so make sure {@link android.content.BroadcastReceiver}s handle duplicates.
     */
    public static void stop(Context context) {
        BluetoothManager.context = new WeakReference<>(context);
        if (handler == null || handlerThread == null || !handlerThread.isAlive()) {
            Log.w(TAG, "handlerThread is already stopped, doing nothing!");
            sendBroadcast(STATUS_STOPPED, null);
            return;
        }
        sendBroadcast(STATUS_STOPPING, null);
        handler.sendEmptyMessage(STOP);
    }

    /**
     * Starts the service, triggering a status broadcast via {@link #ACTION_STATUS}.
     * {@link #STATUS_STARTED} can be broadcast multiple times for the same session,
     * so make sure {@link android.content.BroadcastReceiver}s handle duplicates.
     */
    public static void start(final Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) !=
                PackageManager.PERMISSION_GRANTED) {
            // TODO we either throw away that Bluetooth code or properly request permissions here
            return;
        }
        BluetoothManager.context = new WeakReference<>(context);
        if (handlerThread != null && handlerThread.isAlive()) {
            sendBroadcast(STATUS_STARTED, null);
            return;
        }
        sendBroadcast(STATUS_STARTING, null);

        final BluetoothServer bluetoothServer = new BluetoothServer(context.getFilesDir());
        handlerThread = new HandlerThread("BluetoothManager", Process.THREAD_PRIORITY_LESS_FAVORABLE) {
            @Override
            protected void onLooperPrepared() {
                LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
                localBroadcastManager.registerReceiver(bluetoothDeviceFound,
                        new IntentFilter(BluetoothDevice.ACTION_FOUND));
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                String name = bluetoothAdapter.getName();
                if (name != null) {
                    SwapService.putBluetoothNameBeforeSwap(name);
                }
                if (!bluetoothAdapter.enable()) {
                    sendBroadcast(STATUS_ERROR, context.getString(R.string.swap_error_cannot_start_bluetooth));
                    return;
                }
                bluetoothServer.start();
                if (bluetoothAdapter.startDiscovery()) {
                    sendBroadcast(STATUS_STARTED, null);
                } else {
                    sendBroadcast(STATUS_ERROR, context.getString(R.string.swap_error_cannot_start_bluetooth));
                }
                for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
                    sendFoundBroadcast(context, device);
                }
            }
        };
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
                localBroadcastManager.unregisterReceiver(bluetoothDeviceFound);
                bluetoothServer.close();
                if (bluetoothAdapter != null) {
                    bluetoothAdapter.cancelDiscovery();
                    if (!SwapService.wasBluetoothEnabledBeforeSwap()) {
                        bluetoothAdapter.disable();
                    }
                    String name = SwapService.getBluetoothNameBeforeSwap();
                    if (name != null) {
                        bluetoothAdapter.setName(name);
                    }
                }
                handlerThread.quit();
                handlerThread = null;
                sendBroadcast(STATUS_STOPPED, null);
            }
        };
    }

    public static void restart(Context context) {
        stop(context);
        try {
            handlerThread.join(10000);
        } catch (InterruptedException | NullPointerException e) {
            // ignored
        }
        start(context);
    }

    public static void setName(Context context, String name) {
        // TODO
    }

    public static boolean isAlive() {
        return handlerThread != null && handlerThread.isAlive();
    }

    private static void sendBroadcast(int status, String message) {

        Intent intent = new Intent(ACTION_STATUS);
        intent.putExtra(EXTRA_STATUS, status);
        if (!TextUtils.isEmpty(message)) {
            intent.putExtra(Intent.EXTRA_TEXT, message);
        }
        LocalBroadcastManager.getInstance(context.get()).sendBroadcast(intent);
    }

    private static final BroadcastReceiver bluetoothDeviceFound = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            sendFoundBroadcast(context, (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
        }
    };

    private static void sendFoundBroadcast(Context context, BluetoothDevice device) {
        BluetoothPeer bluetoothPeer = BluetoothPeer.getInstance(device);
        if (bluetoothPeer == null) {
            Utils.debugLog(TAG, "IGNORING: " + device);
            return;
        }
        Intent intent = new Intent(ACTION_FOUND);
        intent.putExtra(EXTRA_PEER, bluetoothPeer);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
