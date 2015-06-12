package org.fdroid.fdroid.localrepo.peers;

import android.content.Intent;
import android.util.Log;

/**
 * Searches for other devices in the vicinity, using specific technologies.
 * Once found, alerts a listener through the
 * {@link org.fdroid.fdroid.localrepo.peers.PeerFinder.Listener#onPeerFound(Object)}
 * method. Note that this could have instead been done with {@link android.content.Context#sendBroadcast(Intent)}
 * and {@link android.content.BroadcastReceiver}, but that would require making the {@link Peer}s
 * {@link android.os.Parcelable}, which is difficult. The main reason it is difficult is because
 * they encapsulate information about network connectivity, such as {@link android.bluetooth.BluetoothDevice}
 * and {@link javax.jmdns.ServiceInfo}, which may be difficult to serialize and reconstruct again.
 */
public abstract class PeerFinder<T extends Peer> {

    private static final String TAG = "PeerFinder";

    private Listener<T> listener;

    public abstract void scan();
    public abstract void cancel();

    protected void foundPeer(T peer) {
        Log.i(TAG, "Found peer " + peer.getName());
        if (listener != null) {
            listener.onPeerFound(peer);
        }
    }

    public void setListener(Listener<T> listener) {
        this.listener = listener;
    }

    public interface Listener<T> {
        void onPeerFound(T peer);
        // TODO: What about peers removed, e.g. as with jmdns ServiceListener#serviceRemoved()
    }

}
