package org.fdroid.fdroid.localrepo.peers;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.localrepo.type.BluetoothSwap;

public class BluetoothPeer implements Peer {

    private final BluetoothDevice device;

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
        return peer != null
                && peer instanceof BluetoothPeer
                && ((BluetoothPeer) peer).device.getAddress().equals(device.getAddress());
    }

    @Override
    public int hashCode() {
        return device.getAddress().hashCode();
    }

    @Override
    public String getRepoAddress() {
        return "bluetooth://" + device.getAddress().replace(':', '-') + "/fdroid/repo";
    }

    /**
     * Return the fingerprint of the signing key, or {@code null} if it is not set.
     *
     * This is not yet stored for Bluetooth connections. Once a device is connected to a bluetooth
     * socket, if we trust it enough to accept a fingerprint from it somehow, then we may as well
     * trust it enough to receive an index from it that contains a fingerprint we can use.
     */
    @Override
    public String getFingerprint() {
        return null;
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

    private BluetoothPeer(Parcel in) {
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
