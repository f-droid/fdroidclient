package org.fdroid.fdroid.localrepo.peers;

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
            // TODO: Don't us "name" for comparing, but rather fingerprint of the swap repo.
            return that.serviceInfo.getName().equals(this.serviceInfo.getName());
        }
        return false;
    }

}
