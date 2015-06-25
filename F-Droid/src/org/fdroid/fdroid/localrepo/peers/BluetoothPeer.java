package org.fdroid.fdroid.localrepo.peers;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;

// TODO: Still to be implemented.
public class BluetoothPeer implements Peer {

    private BluetoothDevice device;

    public BluetoothPeer(BluetoothDevice device) {
        this.device = device;
    }

    @Override
    public String toString() {
        return getName();
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
        return peer != null && peer instanceof BluetoothPeer && ((BluetoothPeer)peer).device.getAddress().equals(device.getAddress());
    }

    @Override
    public String getRepoAddress() {
        return "bluetooth://" + device.getAddress() + "/fdroid/repo";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.device, 0);
    }

    protected BluetoothPeer(Parcel in) {
        this.device = in.readParcelable(BluetoothDevice.class.getClassLoader());
    }

    public static final Creator<BluetoothPeer> CREATOR = new Creator<BluetoothPeer>() {
        public BluetoothPeer createFromParcel(Parcel source) {
            return new BluetoothPeer(source);
        }

        public BluetoothPeer[] newArray(int size) {
            return new BluetoothPeer[size];
        }
    };

}
