package org.fdroid.fdroid.localrepo.peers;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

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

    private final Context context;
    private JmDNS mJmdns;
    private WifiManager wifiManager;
    private WifiManager.MulticastLock mMulticastLock;

    public BonjourFinder(Context context) {
        this.context = context;
    }

    @Override
    public void scan() {

        if (wifiManager == null) {
            wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            mMulticastLock = wifiManager.createMulticastLock(context.getPackageName());
            mMulticastLock.setReferenceCounted(false);
        }

        mMulticastLock.acquire();
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    int ip = wifiManager.getConnectionInfo().getIpAddress();
                    byte[] byteIp = {
                            (byte) (ip & 0xff),
                            (byte) (ip >> 8 & 0xff),
                            (byte) (ip >> 16 & 0xff),
                            (byte) (ip >> 24 & 0xff)
                    };
                    Log.d(TAG, "Searching for mDNS clients...");
                    mJmdns = JmDNS.create(InetAddress.getByAddress(byteIp));
                    Log.d(TAG, "Finished searching for mDNS clients.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                Log.d(TAG, "Cleaning up mDNS service listeners.");
                if (mJmdns != null) {
                    mJmdns.addServiceListener(HTTP_SERVICE_TYPE, BonjourFinder.this);
                    mJmdns.addServiceListener(HTTPS_SERVICE_TYPE, BonjourFinder.this);
                }
            }
        }.execute();

    }

    @Override
    public void serviceRemoved(ServiceEvent event) {
    }

    @Override
    public void serviceAdded(final ServiceEvent event) {
        addFDroidService(event);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mJmdns.requestServiceInfo(event.getType(), event.getName(), true);
                return null;
            }
        }.execute();
    }

    @Override
    public void serviceResolved(ServiceEvent event) {
        addFDroidService(event);
    }

    private void addFDroidService(ServiceEvent event) {
        final ServiceInfo serviceInfo = event.getInfo();
        if (serviceInfo.getPropertyString("type").startsWith("fdroidrepo")) {
            foundPeer(new BonjourPeer(serviceInfo));
        }
    }

    @Override
    public void cancel() {
        mMulticastLock.release();
        if (mJmdns == null)
            return;
        mJmdns.removeServiceListener(HTTP_SERVICE_TYPE, this);
        mJmdns.removeServiceListener(HTTPS_SERVICE_TYPE, this);
        mJmdns = null;

    }

}
