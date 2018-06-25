package org.fdroid.fdroid.localrepo.peers;

import android.content.Context;
import android.net.wifi.WifiManager;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Utils;

import java.io.IOException;
import java.net.InetAddress;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

@SuppressWarnings("LineLength")
final class BonjourFinder extends PeerFinder implements ServiceListener {

    public static Observable<Peer> createBonjourObservable(final Context context) {
        return Observable.create(new Observable.OnSubscribe<Peer>() {
            @Override
            public void call(Subscriber<? super Peer> subscriber) {
                final BonjourFinder finder = new BonjourFinder(context, subscriber);

                subscriber.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        finder.cancel();
                    }
                }));

                finder.scan();
            }
        });
    }

    private static final String TAG = "BonjourFinder";

    private static final String HTTP_SERVICE_TYPE = "_http._tcp.local.";
    private static final String HTTPS_SERVICE_TYPE = "_https._tcp.local.";

    private JmDNS jmdns;
    private WifiManager wifiManager;
    private WifiManager.MulticastLock multicastLock;

    private BonjourFinder(Context context, Subscriber<? super Peer> subscriber) {
        super(context, subscriber);
    }

    private void scan() {

        Utils.debugLog(TAG, "Requested Bonjour (mDNS) scan for peers.");

        if (wifiManager == null) {
            wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            multicastLock = wifiManager.createMulticastLock(context.getPackageName());
            multicastLock.setReferenceCounted(false);
        }

        if (isScanning) {
            Utils.debugLog(TAG, "Requested Bonjour scan, but already scanning. But we will still try to explicitly scan for services.");
            return;
        }

        isScanning = true;
        multicastLock.acquire();

        try {
            Utils.debugLog(TAG, "Searching for Bonjour (mDNS) clients...");
            jmdns = JmDNS.create(InetAddress.getByName(FDroidApp.ipAddressString));
        } catch (IOException e) {
            subscriber.onError(e);
            return;
        }

        Utils.debugLog(TAG, "Adding mDNS service listeners for " + HTTP_SERVICE_TYPE + " and " + HTTPS_SERVICE_TYPE);
        jmdns.addServiceListener(HTTP_SERVICE_TYPE, this);
        jmdns.addServiceListener(HTTPS_SERVICE_TYPE, this);
        listServices();
    }

    private void listServices() {
        Utils.debugLog(TAG, "Explicitly querying for services, in addition to waiting for notifications.");
        addFDroidServices(jmdns.list(HTTP_SERVICE_TYPE));
        addFDroidServices(jmdns.list(HTTPS_SERVICE_TYPE));
    }

    @Override
    public void serviceRemoved(ServiceEvent event) {
    }

    @Override
    public void serviceAdded(final ServiceEvent event) {
        // TODO: Get clarification, but it looks like this is:
        //   1) Identifying that there is _a_ bonjour service available
        //   2) Adding it to the list to give some sort of feedback to the user
        //   3) Requesting more detailed info in an async manner
        //   4) If that is in fact an fdroid repo (after requesting info), then add it again
        //      so that more detailed info can be shown to the user.
        //
        //    If so, when is the old one removed?
        addFDroidService(event.getInfo());

        Utils.debugLog(TAG, "Found JmDNS service, now requesting further details of service");
        jmdns.requestServiceInfo(event.getType(), event.getName(), true);
    }

    @Override
    public void serviceResolved(ServiceEvent event) {
        addFDroidService(event.getInfo());
    }

    private void addFDroidServices(ServiceInfo[] services) {
        for (ServiceInfo info : services) {
            addFDroidService(info);
        }
    }

    /**
     * Broadcasts the fact that a Bonjour peer was found to swap with.
     * Checks that the service is an F-Droid service, and also that it is not the F-Droid service
     * for this device (by comparing its signing fingerprint to our signing fingerprint).
     */
    private void addFDroidService(ServiceInfo serviceInfo) {
        final String type = serviceInfo.getPropertyString("type");
        final String fingerprint = serviceInfo.getPropertyString("fingerprint");
        final boolean isFDroid = type != null && type.startsWith("fdroidrepo");
        final boolean isSelf = FDroidApp.repo != null && fingerprint != null && fingerprint.equalsIgnoreCase(FDroidApp.repo.fingerprint);
        if (isFDroid && !isSelf) {
            Utils.debugLog(TAG, "Found F-Droid swap Bonjour service:\n" + serviceInfo);
            subscriber.onNext(new BonjourPeer(serviceInfo));
        } else {
            if (isSelf) {
                Utils.debugLog(TAG, "Ignoring Bonjour service because it belongs to this device:\n" + serviceInfo);
            } else {
                Utils.debugLog(TAG, "Ignoring Bonjour service because it doesn't look like an F-Droid swap repo:\n" + serviceInfo);
            }
        }
    }

    private void cancel() {
        Utils.debugLog(TAG, "Cancelling BonjourFinder, releasing multicast lock, removing jmdns service listeners");

        if (multicastLock != null) {
            multicastLock.release();
        }

        isScanning = false;

        if (jmdns == null) {
            return;
        }
        jmdns.removeServiceListener(HTTP_SERVICE_TYPE, this);
        jmdns.removeServiceListener(HTTPS_SERVICE_TYPE, this);
        jmdns = null;

    }

}
