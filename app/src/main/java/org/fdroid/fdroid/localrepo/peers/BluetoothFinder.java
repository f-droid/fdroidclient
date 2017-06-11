package org.fdroid.fdroid.localrepo.peers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import org.fdroid.fdroid.Utils;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

@SuppressWarnings("LineLength")
final class BluetoothFinder extends PeerFinder {

    public static Observable<Peer> createBluetoothObservable(final Context context) {
        return Observable.create(new Observable.OnSubscribe<Peer>() {
            @Override
            public void call(Subscriber<? super Peer> subscriber) {
                final BluetoothFinder finder = new BluetoothFinder(context, subscriber);

                subscriber.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        finder.cancel();
                    }
                }));

                finder.scan();
            }
        });
    }

    private static final String TAG = "BluetoothFinder";

    private final BluetoothAdapter adapter;

    private BluetoothFinder(Context context, Subscriber<? super Peer> subscriber) {
        super(context, subscriber);
        adapter = BluetoothAdapter.getDefaultAdapter();
    }

    private BroadcastReceiver deviceFoundReceiver;
    private BroadcastReceiver scanCompleteReceiver;

    private void scan() {

        if (adapter == null) {
            Log.i(TAG, "Not scanning for bluetooth peers to swap with, couldn't find a bluetooth adapter on this device.");
            return;
        }

        isScanning = true;

        if (deviceFoundReceiver == null) {
            deviceFoundReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        onDeviceFound(device);
                    }
                }
            };
            context.registerReceiver(deviceFoundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        }

        if (scanCompleteReceiver == null) {
            scanCompleteReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (isScanning) {
                        Utils.debugLog(TAG, "Scan complete, but we haven't been asked to stop scanning yet, so will restart scan.");
                        startDiscovery();
                    }
                }
            };

            // TODO: Unregister this receiver at the appropriate time.
            context.registerReceiver(scanCompleteReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        }

        startDiscovery();
    }

    private void startDiscovery() {

        if (adapter.isDiscovering()) {
            // TODO: Can we reset the discovering timeout, so that it doesn't, e.g. time out in 3
            // seconds because we had already almost completed the previous scan? We could
            // cancelDiscovery(), but then it will probably prompt the user again.
            Utils.debugLog(TAG, "Requested bluetooth scan when already scanning, so will ignore request.");
            return;
        }

        if (!adapter.startDiscovery()) {
            Log.e(TAG, "Couldn't start bluetooth scanning.");
        }

    }

    private void cancel() {
        if (adapter != null) {
            Utils.debugLog(TAG, "Stopping bluetooth discovery.");
            adapter.cancelDiscovery();
        }

        isScanning = false;
    }

    private void onDeviceFound(BluetoothDevice device) {
        if (device != null && device.getName() != null &&
                (device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA ||
                device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA ||
                device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.PHONE_SMART)) {
            subscriber.onNext(new BluetoothPeer(device));
        }
    }

}
