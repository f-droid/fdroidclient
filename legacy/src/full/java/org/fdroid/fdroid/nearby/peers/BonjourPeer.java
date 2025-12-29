package org.fdroid.fdroid.nearby.peers;

import android.net.Uri;
import android.os.Parcel;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import org.fdroid.fdroid.FDroidApp;

import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.FDroidServiceInfo;

public class BonjourPeer extends WifiPeer {
    private static final String TAG = "BonjourPeer";

    public static final String FINGERPRINT = "fingerprint";
    public static final String NAME = "name";
    public static final String PATH = "path";
    public static final String TYPE = "type";

    private final FDroidServiceInfo serviceInfo;

    /**
     * Return a instance if the {@link ServiceInfo} is fully resolved and does
     * not represent this device, but something else on the network.
     */
    @Nullable
    public static BonjourPeer getInstance(ServiceInfo serviceInfo) {
        String type = serviceInfo.getPropertyString(TYPE);
        String fingerprint = serviceInfo.getPropertyString(FINGERPRINT);
        if (type == null || !type.startsWith("fdroidrepo") || FDroidApp.repo == null
                || TextUtils.equals(FDroidApp.repo.getFingerprint(), fingerprint)) {
            return null;
        }
        return new BonjourPeer(serviceInfo);
    }

    private BonjourPeer(ServiceInfo serviceInfo) {
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
    public int hashCode() {
        String fingerprint = getFingerprint();
        if (fingerprint == null) {
            return 0;
        }
        return fingerprint.hashCode();
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
