package org.fdroid.fdroid.localrepo.peers;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.FDroidApp;

import java.io.IOException;
import java.net.InetAddress;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

public class BonjourFinder extends PeerFinder<BonjourPeer> implements ServiceListener {

    private static final String TAG = "BonjourFinder";

    public static final String HTTP_SERVICE_TYPE = "_http._tcp.local.";
    public static final String HTTPS_SERVICE_TYPE = "_https._tcp.local.";

    private JmDNS jmdns;
    private WifiManager wifiManager;
    private WifiManager.MulticastLock mMulticastLock;

    public BonjourFinder(Context context) {
        super(context);
    }

    @Override
    public void scan() {

        Log.d(TAG, "Requested Bonjour (mDNS) scan for peers.");

        if (wifiManager == null) {
            wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            mMulticastLock = wifiManager.createMulticastLock(context.getPackageName());
            mMulticastLock.setReferenceCounted(false);
        }

        if (isScanning) {
            Log.d(TAG, "Requested Bonjour scan, but already scanning. But we will still try to explicitly scan for services.");
            // listServices();
            return;
        }

        isScanning = true;
        mMulticastLock.acquire();
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Log.d(TAG, "Searching for Bonjour (mDNS) clients...");
                    jmdns = JmDNS.create(InetAddress.getByName(FDroidApp.ipAddressString));
                } catch (IOException e) {
                    Log.e(TAG, "", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                // TODO: This is not threadsafe - cancelling the discovery will make jmdns null, but it could happen after this check and before call to addServiceListener().
                if (jmdns != null) {
                    Log.d(TAG, "Adding mDNS service listeners for " + HTTP_SERVICE_TYPE + " and " + HTTPS_SERVICE_TYPE);
                    jmdns.addServiceListener(HTTP_SERVICE_TYPE, BonjourFinder.this);
                    jmdns.addServiceListener(HTTPS_SERVICE_TYPE, BonjourFinder.this);
                    listServices();
                }
            }
        }.execute();

    }

    private void listServices() {

        // The member variable is likely to get set to null if a swap process starts, thus we hold
        // a reference for the benefit of the background task so it doesn't have to synchronoize on it.
        final JmDNS mdns = jmdns;

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                Log.d(TAG, "Explicitly querying for services, in addition to waiting for notifications.");
                addFDroidServices(mdns.list(HTTP_SERVICE_TYPE));
                addFDroidServices(mdns.list(HTTPS_SERVICE_TYPE));
                return null;
            }
        }.execute();
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

        // The member variable is likely to get set to null if a swap process starts, thus we hold
        // a reference for the benefit of the background task so it doesn't have to synchronoize on it.
        final JmDNS mdns = jmdns;
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mdns.requestServiceInfo(event.getType(), event.getName(), true);
                return null;
            }
        }.execute();
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
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Found F-Droid swap Bonjour service:\n" + serviceInfo);
            }
            foundPeer(new BonjourPeer(serviceInfo));
        } else {
            if (BuildConfig.DEBUG) {
                if (isSelf) {
                    Log.d(TAG, "Ignoring Bonjour service because it belongs to this device:\n" + serviceInfo);
                } else {
                    Log.d(TAG, "Ignoring Bonjour service because it doesn't look like an F-Droid swap repo:\n" + serviceInfo);
                }
            }
        }
    }

    @Override
    public void cancel() {
        if (mMulticastLock != null) {
            mMulticastLock.release();
        }

        isScanning = false;

        if (jmdns == null)
            return;
        jmdns.removeServiceListener(HTTP_SERVICE_TYPE, this);
        jmdns.removeServiceListener(HTTPS_SERVICE_TYPE, this);
        jmdns = null;

    }

}
