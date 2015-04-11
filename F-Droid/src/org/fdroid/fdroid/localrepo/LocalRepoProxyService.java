package org.fdroid.fdroid.localrepo;

import org.fdroid.fdroid.FDroidApp;

/**
 * Starts the local repo service but bound to 127.0.0.1.
 * Also, it does not care about whether wifi is connected or not,
 * and thus doesn't care about Bonjour.
 */
public class LocalRepoProxyService extends LocalRepoService {

    @Override
    protected void onStartNetworkServices() {
        // Do nothing
    }

    @Override
    protected void onStopNetworkServices() {
        // Do nothing
    }

    @Override
    protected boolean useHttps() {
        return false;
    }

    @Override
    protected String getIpAddressToBindTo() {
        return "127.0.0.1";
    }

    @Override
    protected int getPortToBindTo() {
        return FDroidApp.port + 1;
    }
}
