package org.fdroid.fdroid.localrepo.peers;

import android.support.annotation.DrawableRes;

import java.io.Serializable;

public interface Peer extends Serializable {

    String getName();

    @DrawableRes int getIcon();

    boolean equals(Peer peer);

}
