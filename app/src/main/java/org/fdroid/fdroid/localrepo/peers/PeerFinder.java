package org.fdroid.fdroid.localrepo.peers;

import android.content.Context;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Searches for other devices in the vicinity, using specific technologies.
 * Once found, emits a {@link Peer} to interested {@link Subscriber}s.
 */
public abstract class PeerFinder {

    protected boolean isScanning;
    protected final Context context;
    protected final Subscriber<? super Peer> subscriber;

    protected PeerFinder(Context context, Subscriber<? super Peer> subscriber) {
        this.context = context;
        this.subscriber = subscriber;
    }

    public static Observable<Peer> createObservable(final Context context) {
        return Observable.merge(
            BluetoothFinder.createBluetoothObservable(context).subscribeOn(Schedulers.newThread()),
            BonjourFinder.createBonjourObservable(context).subscribeOn(Schedulers.newThread())
        );
    }

}
