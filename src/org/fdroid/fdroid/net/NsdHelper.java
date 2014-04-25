package org.fdroid.fdroid.net;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.net.nsd.NsdManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.fdroid.fdroid.R;

import java.util.ArrayList;
import java.util.List;

@TargetApi(16) // AKA Android 4.1 AKA Jelly Bean
public class NsdHelper {

    public static final String TAG = "NsdHelper";
    public static final String HTTP_SERVICE_TYPE =  "_fdroidrepo._tcp.";
    public static final String HTTPS_SERVICE_TYPE = "_fdroidrepos._tcp.";

    final Context mContext;
    final NsdManager mNsdManager;
    final RepoScanListAdapter mAdapter;

    NsdManager.ResolveListener mResolveListener;
    NsdManager.DiscoveryListener mDiscoveryListener;

    public NsdHelper(Context context, final RepoScanListAdapter adapter) {
        mContext = context;
        mAdapter = adapter;
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);

        initializeResolveListener();
        initializeDiscoveryListener();
    }

    public void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
                Log.i(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service)
            {
                Log.d(TAG, "Discovered service: "+ service.getServiceName() +
                           " Type: "+ service.getServiceType());

                if (service.getServiceType().equals(HTTP_SERVICE_TYPE) ||
                    service.getServiceType().equals(HTTPS_SERVICE_TYPE))
                {
                  Log.d(TAG, "Resolving FDroid service");
                  mNsdManager.resolveService(service, mResolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "service lost" + service);
                mAdapter.removeItem(service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Resolve failed: Error code: " + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Resolve Succeeded. " + serviceInfo);
                mAdapter.addItem(serviceInfo);
            }
        };
    }

    public void discoverServices() {
        mNsdManager.discoverServices(
                HTTP_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
        mNsdManager.discoverServices(
                HTTPS_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    public void stopDiscovery() {
        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
    }

    public static class RepoScanListAdapter extends BaseAdapter {
        private Context mContext;
        private LayoutInflater mLayoutInflater;
        private List<DiscoveredRepo> mEntries = new ArrayList<DiscoveredRepo>();

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
        public View getView(int position, View convertView, ViewGroup parent) {
           RelativeLayout itemView;
           if (convertView == null)
           {
              itemView = (RelativeLayout) mLayoutInflater.inflate(
                          R.layout.repodiscoveryitem, parent, false);
           } else {
              itemView = (RelativeLayout) convertView;
           }

           TextView nameLabel = (TextView) itemView.findViewById(R.id.reposcanitemname);
           TextView addressLabel = (TextView) itemView.findViewById(R.id.reposcanitemaddress);

           final DiscoveredRepo service = mEntries.get(position);
           final NsdServiceInfo serviceInfo = service.getServiceInfo();

           String addressTxt = "Hosted @ "+
                   serviceInfo.getHost().getHostAddress() + ":"+ serviceInfo.getPort();

           nameLabel.setText(serviceInfo.getServiceName());
           addressLabel.setText(addressTxt);

           return itemView;
        }

        public void addItem(NsdServiceInfo item)
        {
            if(item == null || item.getServiceName() == null)
                return;

            //Construct a DiscoveredRepo wrapper for the service being
            //added in order to use a name based equals().
            DiscoveredRepo repoBean = new DiscoveredRepo(item);
            mEntries.add(repoBean);

            notifyUpdate();
        }

        public void removeItem(NsdServiceInfo item)
        {
            if(item == null || item.getServiceName() == null)
                return;

            //Construct a DiscoveredRepo wrapper for the service being
            //removed in order to use a name based equals().
            DiscoveredRepo lostServiceBean = new DiscoveredRepo(item);

            if(mEntries.contains(lostServiceBean))
            {
                mEntries.remove(lostServiceBean);
                notifyUpdate();
            }
        }

        private void notifyUpdate()
        {
            //Need to call notifyDataSetChanged from the UI thread
            //in order for it to update the ListView without error
            Handler refresh = new Handler(Looper.getMainLooper());
            refresh.post(new Runnable() {
                @Override
                public void run()
                {
                    notifyDataSetChanged();
                }
            });
        }
    }

    public static class DiscoveredRepo {
        private final NsdServiceInfo mServiceInfo;

        public DiscoveredRepo(NsdServiceInfo serviceInfo)
        {
            if(serviceInfo == null || serviceInfo.getServiceName() == null)
                throw new IllegalArgumentException(
                        "Parameters \"serviceInfo\" and \"name\" must not be null.");
            mServiceInfo = serviceInfo;
        }

        public NsdServiceInfo getServiceInfo()
        {
            return mServiceInfo;
        }

        public String getName()
        {
            return mServiceInfo.getServiceName();
        }

        @Override
        public boolean equals(Object other)
        {
            if(!(other instanceof DiscoveredRepo))
                return false;

            //Treat two services the same based on name. Eventually
            //there should be a persistent mapping between fingerprint
            //of the repo key and the discovered service such that we
            //could maintain trust across hostnames/ips/networks
            DiscoveredRepo otherRepo = (DiscoveredRepo) other;
            return getName().equals(otherRepo.getName());
        }
    }
}



