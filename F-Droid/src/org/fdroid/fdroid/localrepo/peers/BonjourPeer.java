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

}
