package org.fdroid.fdroid.localrepo.peers;

import android.os.Parcelable;
import android.support.annotation.DrawableRes;

public interface Peer extends Parcelable {

    String getName();

    @DrawableRes int getIcon();

    boolean equals(Object peer);

    String getRepoAddress();

    String getFingerprint();

    boolean shouldPromptForSwapBack();
}
