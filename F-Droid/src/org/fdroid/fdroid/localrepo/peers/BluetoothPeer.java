package org.fdroid.fdroid.localrepo.peers;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.localrepo.type.BluetoothSwap;

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
        return device.getName().replaceAll("^" + BluetoothSwap.BLUETOOTH_NAME_TAG, "");
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_bluetooth_white;
    }

    @Override
    public boolean equals(Object peer) {
        return peer != null && peer instanceof BluetoothPeer && ((BluetoothPeer)peer).device.getAddress().equals(device.getAddress());
    }

    @Override
    public String getRepoAddress() {
        return "bluetooth://" + device.getAddress().replace(':', '-') + "/fdroid/repo";
    }

    @Override
    public String getFingerprint() {
        // TODO: Get fingerprint somehow over bluetooth.
        return "";
    }

    @Override
    public boolean shouldPromptForSwapBack() {
        return false;
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
