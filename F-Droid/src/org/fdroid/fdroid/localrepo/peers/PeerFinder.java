package org.fdroid.fdroid.localrepo.peers;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import org.fdroid.fdroid.localrepo.SwapManager;

/**
 * Searches for other devices in the vicinity, using specific technologies.
 * Once found, sends an {@link SwapManager#ACTION_PEER_FOUND} intent with the {@link SwapManager#EXTRA_PEER}
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
        Intent intent = new Intent(SwapManager.ACTION_PEER_FOUND);
        intent.putExtra(SwapManager.EXTRA_PEER, peer);
        context.sendBroadcast(intent);
    }

}
