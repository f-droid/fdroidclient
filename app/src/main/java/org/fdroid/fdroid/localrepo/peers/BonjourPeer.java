package org.fdroid.fdroid.localrepo.peers;

import android.net.Uri;
import android.os.Parcel;

import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.FDroidServiceInfo;

public class BonjourPeer extends WifiPeer {

    private final FDroidServiceInfo serviceInfo;

    public BonjourPeer(ServiceInfo serviceInfo) {
        this.serviceInfo = new FDroidServiceInfo(serviceInfo);
        this.name = serviceInfo.getDomain();
        this.uri = Uri.parse(this.serviceInfo.getRepoAddress());
        this.shouldPromptForSwapBack = true;
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
    public boolean equals(Object peer) {
        if (peer != null && peer instanceof BonjourPeer) {
            BonjourPeer that = (BonjourPeer) peer;
            return this.getFingerprint().equals(that.getFingerprint());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getFingerprint().hashCode();
    }

    @Override
    public String getRepoAddress() {
        return serviceInfo.getRepoAddress();
    }

    /**
     * Return the fingerprint of the signing key, or {@code null} if it is not set.
     */
    @Override
    public String getFingerprint() {
        return serviceInfo.getFingerprint();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(serviceInfo, flags);
    }

    private BonjourPeer(Parcel in) {
        this((ServiceInfo) in.readParcelable(FDroidServiceInfo.class.getClassLoader()));
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
