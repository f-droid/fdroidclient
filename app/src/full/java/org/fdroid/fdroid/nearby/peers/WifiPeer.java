package org.fdroid.fdroid.nearby.peers;

import android.net.Uri;
import android.os.Parcel;
import android.text.TextUtils;

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

    /**
     * Return if this instance points to the same device as that instance, even
     * if some of the configuration details are not the same, like whether one
     * instance supplies the fingerprint and the other does not, then use IP
     * address and port number.
     */
    @Override
    public boolean equals(Object peer) {
        if (peer instanceof BluetoothPeer) {
            return false;
        }
        String fingerprint = getFingerprint();
        if (this instanceof BonjourPeer && peer instanceof BonjourPeer) {
            BonjourPeer that = (BonjourPeer) peer;
            return TextUtils.equals(this.getFingerprint(), that.getFingerprint());
        } else {
            WifiPeer that = (WifiPeer) peer;
            if (!TextUtils.isEmpty(fingerprint) && TextUtils.equals(this.getFingerprint(), that.getFingerprint())) {
                return true;
            }
            return TextUtils.equals(this.getRepoAddress(), that.getRepoAddress());
        }
    }

    @Override
    public int hashCode() {
        return (uri.getHost() + uri.getPort()).hashCode();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_wifi;
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
