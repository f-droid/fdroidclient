package org.fdroid.fdroid.localrepo.peers;

import android.os.Parcel;

import org.fdroid.fdroid.R;

import java.net.Inet4Address;
import java.net.Inet6Address;

import javax.jmdns.impl.FDroidServiceInfo;
import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.ServiceInfoImpl;

public class BonjourPeer implements Peer {

    private FDroidServiceInfo serviceInfo;

    public BonjourPeer(ServiceInfo serviceInfo) {
        this.serviceInfo = new FDroidServiceInfo(serviceInfo);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public String getName() {
        return serviceInfo.getName();
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_network_wifi_white;
    }

    @Override
    public boolean equals(Object peer) {
        if (peer != null && peer instanceof BonjourPeer) {
            BonjourPeer that = (BonjourPeer)peer;
            return this.getFingerprint().equals(that.getFingerprint());
        }
        return false;
    }

    @Override
    public String getRepoAddress() {
        return serviceInfo.getURL(); // Automatically appends the "path" property if present, so no need to do it ourselves.
    }

    @Override
    public String getFingerprint() {
        return serviceInfo.getPropertyString("fingerprint");
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(serviceInfo, flags);
    }

    protected BonjourPeer(Parcel in) {
        this.serviceInfo = in.readParcelable(FDroidServiceInfo.class.getClassLoader());
    }

    public static final Creator<BonjourPeer> CREATOR = new Creator<BonjourPeer>() {
        public BonjourPeer createFromParcel(Parcel source) {
            return new BonjourPeer(source);
        }

        public BonjourPeer[] newArray(int size) {
            return new BonjourPeer[size];
        }
    };
}
