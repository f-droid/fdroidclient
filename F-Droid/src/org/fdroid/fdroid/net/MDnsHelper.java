package org.fdroid.fdroid.net;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.fdroid.fdroid.R;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

public class MDnsHelper implements ServiceListener {

    private static final String TAG = "MDnsHelper";
    public static final String HTTP_SERVICE_TYPE = "_http._tcp.local.";
    public static final String HTTPS_SERVICE_TYPE = "_https._tcp.local.";

    final Activity mActivity;
    final RepoScanListAdapter mAdapter;

    private JmDNS mJmdns;
    private final WifiManager wifiManager;
    private final MulticastLock mMulticastLock;

    public MDnsHelper(Activity activity, final RepoScanListAdapter adapter) {
        mActivity = activity;
        mAdapter = adapter;
        wifiManager = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
        mMulticastLock = wifiManager.createMulticastLock(activity.getPackageName());
        mMulticastLock.setReferenceCounted(false);
    }

    @Override
    public void serviceRemoved(ServiceEvent event) {
        // a ListView Adapter can only be updated on the UI thread
        final ServiceInfo serviceInfo = event.getInfo();
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.removeItem(serviceInfo);
            }
        });
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
        // a ListView Adapter can only be updated on the UI thread
        final ServiceInfo serviceInfo = event.getInfo();
        String type = serviceInfo.getPropertyString("type");
        if (type.startsWith("fdroidrepo"))
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.addItem(serviceInfo);
                }
            });
    }

    public void discoverServices() {
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
                    mJmdns = JmDNS.create(InetAddress.getByAddress(byteIp));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (mJmdns != null) {
                    mJmdns.addServiceListener(HTTP_SERVICE_TYPE, MDnsHelper.this);
                    mJmdns.addServiceListener(HTTPS_SERVICE_TYPE, MDnsHelper.this);
                }
            }
        }.execute();
    }

    public void stopDiscovery() {
        mMulticastLock.release();
        if (mJmdns == null)
            return;
        mJmdns.removeServiceListener(HTTP_SERVICE_TYPE, MDnsHelper.this);
        mJmdns.removeServiceListener(HTTPS_SERVICE_TYPE, MDnsHelper.this);
        mJmdns = null;
    }

    public static class RepoScanListAdapter extends BaseAdapter {
        private final Context mContext;
        private final LayoutInflater mLayoutInflater;
        private final List<DiscoveredRepo> mEntries = new ArrayList<>();

        public RepoScanListAdapter(Context context) {
            mContext = context;
            mLayoutInflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mEntries.size();
        }

        @Override
        public Object getItem(int position) {
            return mEntries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean isEnabled(int position) {
            DiscoveredRepo service = mEntries.get(position);
            ServiceInfo serviceInfo = service.getServiceInfo();
            InetAddress[] addresses = serviceInfo.getInetAddresses();
            return (addresses != null && addresses.length > 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            RelativeLayout itemView;
            if (convertView == null) {
                itemView = (RelativeLayout) mLayoutInflater.inflate(
                        R.layout.repodiscoveryitem, parent, false);
            } else {
                itemView = (RelativeLayout) convertView;
            }

            TextView nameLabel = (TextView) itemView.findViewById(R.id.reposcanitemname);
            TextView addressLabel = (TextView) itemView.findViewById(R.id.reposcanitemaddress);

            final DiscoveredRepo service = mEntries.get(position);
            final ServiceInfo serviceInfo = service.getServiceInfo();

            nameLabel.setText(serviceInfo.getName());

            InetAddress[] addresses = serviceInfo.getInetAddresses();
            if (addresses != null && addresses.length > 0) {
                String addressTxt = "Hosted @ " + addresses[0] + ":" + serviceInfo.getPort();
                addressLabel.setText(addressTxt);
            }

            return itemView;
        }

        public void addItem(ServiceInfo item) {
            if (item == null || item.getName() == null)
                return;

            // Construct a DiscoveredRepo wrapper for the service being
            // added in order to use a name based equals().
            DiscoveredRepo newDRepo = new DiscoveredRepo(item);
            // if an unresolved entry with the same name exists, remove it
            for (DiscoveredRepo dr : mEntries)
                if (dr.equals(newDRepo)) {
                    InetAddress[] addresses = dr.mServiceInfo.getInetAddresses();
                    if (addresses == null || addresses.length == 0)
                        mEntries.remove(dr);
                }
            mEntries.add(newDRepo);

            notifyUpdate();
        }

        public void removeItem(ServiceInfo item) {
            if (item == null || item.getName() == null)
                return;

            // Construct a DiscoveredRepo wrapper for the service being
            // removed in order to use a name based equals().
            DiscoveredRepo lostServiceBean = new DiscoveredRepo(item);

            if (mEntries.contains(lostServiceBean)) {
                mEntries.remove(lostServiceBean);
                notifyUpdate();
            }
        }

        private void notifyUpdate() {
            // Need to call notifyDataSetChanged from the UI thread
            // in order for it to update the ListView without error
            Handler refresh = new Handler(Looper.getMainLooper());
            refresh.post(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }
    }

    public static class DiscoveredRepo {
        private final ServiceInfo mServiceInfo;

        public DiscoveredRepo(ServiceInfo serviceInfo) {
            if (serviceInfo == null || serviceInfo.getName() == null)
                throw new IllegalArgumentException(
                        "Parameters \"serviceInfo\" and \"name\" must not be null.");
            mServiceInfo = serviceInfo;
        }

        public ServiceInfo getServiceInfo() {
            return mServiceInfo;
        }

        public String getName() {
            return mServiceInfo.getName();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof DiscoveredRepo))
                return false;

            // Treat two services the same based on name. Eventually
            // there should be a persistent mapping between fingerprint
            // of the repo key and the discovered service such that we
            // could maintain trust across hostnames/ips/networks
            DiscoveredRepo otherRepo = (DiscoveredRepo) other;
            return getName().equals(otherRepo.getName());
        }
    }
}
