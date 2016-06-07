package org.fdroid.fdroid.localrepo.peers;

import android.net.Uri;
import android.os.Parcel;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.NewRepoConfig;

public class WifiPeer implements Peer {

    protected String name;
    protected Uri uri;
    protected boolean shouldPromptForSwapBack;

    public WifiPeer() {

    }

    public WifiPeer(NewRepoConfig config) {
        this(config.getRepoUri(), config.getHost(), !config.preventFurtherSwaps());
    }

    private WifiPeer(Uri uri, String name, boolean shouldPromptForSwapBack) {
        this.name = name;
        this.uri = uri;
        this.shouldPromptForSwapBack = shouldPromptForSwapBack;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_network_wifi_white;
    }

    @Override
    public String getRepoAddress() {
        return uri.toString();
    }

    @Override
    public String getFingerprint() {
        return uri.getQueryParameter("fingerprint");
    }

    @Override
    public boolean shouldPromptForSwapBack() {
        return shouldPromptForSwapBack;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(uri.toString());
        dest.writeByte(shouldPromptForSwapBack ? (byte) 1 : (byte) 0);
    }

    private WifiPeer(Parcel in) {
        this(Uri.parse(in.readString()), in.readString(), in.readByte() == 1);
    }

    public static final Creator<WifiPeer> CREATOR = new Creator<WifiPeer>() {
        public WifiPeer createFromParcel(Parcel source) {
            return new WifiPeer(source);
        }

        public WifiPeer[] newArray(int size) {
            return new WifiPeer[size];
        }
    };
}
