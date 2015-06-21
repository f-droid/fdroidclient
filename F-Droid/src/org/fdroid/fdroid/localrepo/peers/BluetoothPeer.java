package org.fdroid.fdroid.localrepo.peers;

import android.bluetooth.BluetoothDevice;

// TODO: Still to be implemented.
public class BluetoothPeer implements Peer {

    private BluetoothDevice device;

    public BluetoothPeer(BluetoothDevice device) {
        this.device = device;
    }

    @Override
    public String getName() {
        return "Bluetooth: " + device.getName();
    }

    @Override
    public int getIcon() {
        return android.R.drawable.stat_sys_data_bluetooth;
    }

    @Override
    public boolean equals(Peer peer) {
        return peer != null && peer instanceof BluetoothPeer && ((BluetoothPeer)peer).device.getAddress() == device.getAddress();
    }

}
