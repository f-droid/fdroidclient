package org.fdroid.fdroid.nearby.peers;

import android.os.Parcelable;

import androidx.annotation.DrawableRes;

/**
 * TODO This model assumes that "peers" from Bluetooth, Bonjour, and WiFi are
 * different things.  They are not different repos though, they all point to
 * the same repos.  This should really be combined to be a single "RemoteRepo"
 * class that represents a single device's local repo, and can have zero to
 * many ways to connect to it (e.g. Bluetooth, WiFi, USB Thumb Drive, SD Card,
 * WiFi Direct, etc).
 */
public interface Peer extends Parcelable {

    String getName();

    @DrawableRes
    int getIcon();

    boolean equals(Object peer);

    String getRepoAddress();

    String getFingerprint();

    boolean shouldPromptForSwapBack();
}
