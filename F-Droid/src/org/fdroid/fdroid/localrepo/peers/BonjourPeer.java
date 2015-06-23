package org.fdroid.fdroid.localrepo.peers;

import android.os.Parcel;

import org.fdroid.fdroid.R;

import javax.jmdns.ServiceInfo;

public class BonjourPeer implements Peer {

    private ServiceInfo serviceInfo;

    public BonjourPeer(ServiceInfo serviceInfo) {
        this.serviceInfo = serviceInfo;
    }

    @Override
    public String getName() {
        return "Bonjour: " + serviceInfo.getName();
    }

    @Override
    public int getIcon() {
        return R.drawable.wifi;
    }

    @Override
    public boolean equals(Peer peer) {
        if (peer != null && peer instanceof BonjourPeer) {
            BonjourPeer that = (BonjourPeer)peer;
            // TODO: Don't use "name" for comparing, but rather fingerprint of the swap repo.
            return that.serviceInfo.getName().equals(this.serviceInfo.getName());
        }
        return false;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(serviceInfo.getType());
        dest.writeString(serviceInfo.getName());
        dest.writeString(serviceInfo.getSubtype());
        dest.writeInt(serviceInfo.getPort());
        dest.writeInt(serviceInfo.getWeight());
        dest.writeInt(serviceInfo.getPriority());
        dest.writeByte(serviceInfo.isPersistent() ? (byte) 1 : (byte) 0);
        dest.writeString(serviceInfo.getTextString());
    }

    protected BonjourPeer(Parcel in) {
        String type = in.readString();
        String name = in.readString();
        String subtype = in.readString();
        int port = in.readInt();
        int weight = in.readInt();
        int priority = in.readInt();
        boolean persistent = in.readByte() != 0;
        String text = in.readString();

        this.serviceInfo = ServiceInfo.create(type, name, subtype, port, weight, priority, persistent, text);
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
