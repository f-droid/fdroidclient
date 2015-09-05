package org.fdroid.fdroid.localrepo.peers;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.fdroid.fdroid.localrepo.SwapService;

/**
 * Searches for other devices in the vicinity, using specific technologies.
 * Once found, sends an {@link SwapService#ACTION_PEER_FOUND} intent with the {@link SwapService#EXTRA_PEER}
 * extra attribute set to the subclass of {@link Peer} that was found.
 */
public abstract class PeerFinder<T extends Peer> {

    private static final String TAG = "PeerFinder";

    protected boolean isScanning = false;
    protected final Context context;

    public abstract void scan();
    public abstract void cancel();

    public PeerFinder(Context context) {
        this.context = context;
    }

    public boolean isScanning() {
        return isScanning;
    }

    protected void foundPeer(T peer) {
        Log.i(TAG, "Found peer " + peer.getName());
        Intent intent = new Intent(SwapService.ACTION_PEER_FOUND);
        intent.putExtra(SwapService.EXTRA_PEER, peer);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    protected void removePeer(T peer) {
        // TODO: Broadcast messages when peers are removed too.
    }

}
