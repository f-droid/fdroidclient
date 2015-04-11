package org.fdroid.fdroid.views.swap;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.net.BluetoothDownloader;
import org.fdroid.fdroid.net.bluetooth.BluetoothClient;
import org.fdroid.fdroid.net.bluetooth.BluetoothConnection;
import org.fdroid.fdroid.views.fragments.ThemeableListFragment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class BluetoothDeviceListFragment extends ThemeableListFragment {

    private static final String TAG = "org.fdroid.fdroid.views.swap.BluetoothDeviceListFragment";

    private Adapter adapter = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText("No bluetooth devices found. Is the other device \"discoverable\"?");

        adapter = new Adapter(
            new ContextThemeWrapper(getActivity(), R.style.SwapTheme_BluetoothDeviceList_ListItem),
            R.layout.select_local_apps_list_item
        );

        populateDeviceList();

        setListAdapter(adapter);
        setListShown(false); // start out with a progress indicator
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        View headerView = getHeaderView();
        if (headerView == null) {
            Log.e(TAG, "Could not find header view, although we expected one to exist.");
        } else {
            headerView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    initiateBluetoothScan();
                    return true;
                }
            });
        }
        return view;
    }

    private void initiateBluetoothScan()
    {
        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        BroadcastReceiver deviceFoundReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    boolean exists = false;
                    for (int i = 0; i < adapter.getCount(); i ++) {
                        if (adapter.getItem(i).getAddress().equals(device.getAddress())) {
                            exists = true;
                            break;
                        }
                    }

                    if (!exists) {
                        adapter.add(device);
                    }
                }
            }
        };

        ((ContentLoadingProgressBar)getView().findViewById(R.id.loading_indicator)).show();
        getActivity().registerReceiver(deviceFoundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        if (!bluetooth.startDiscovery()) {
            // TODO: Discovery did not start for some reason :(
            Log.e(TAG, "Could not start bluetooth discovery, but am not sure why :(");
        }
    }

    private void populateDeviceList()
    {
        for (BluetoothDevice device : BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
            adapter.add(device);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {

        // "position" includes the header view, so ignore that.
        if (position == 0) {
            return;
        }

        BluetoothDevice device = adapter.getItem(position - 1);

        // TODO: I think that I can connect regardless of the bond state.
        // It sounds like when I attempt to connect to a non-bonded peer, then
        // Android initiates the pairing dialog on our behalf.

        BluetoothClient client = new BluetoothClient(device);

        try {
            Log.d(TAG, "Testing bluetooth connection (opening connection first).");
            BluetoothConnection connection = client.openConnection();

            ByteArrayOutputStream stream = new ByteArrayOutputStream(4096);
            BluetoothDownloader downloader = new BluetoothDownloader(connection, "/", stream);
            downloader.downloadUninterrupted();
            String result = stream.toString();
            Log.d(TAG, "Download complete.");
            Log.d(TAG, result);

            Log.d(TAG, "Downloading again...");
            downloader = new BluetoothDownloader(connection, "/fdroid/repo/index.xml", stream);
            downloader.downloadUninterrupted();
            result = stream.toString();
            Log.d(TAG, "Download complete.");
            Log.d(TAG, result);

            /*Log.d(TAG, "Creating HEAD request for resource at \"/\"...");
            Request head = Request.createGET("/", connection);
            Log.d(TAG, "Sending request...");
            Response response = head.send();
            Log.d(TAG, "Response from bluetooth: " + response.getStatusCode());
            String contents = response.readContents();
            Log.d(TAG, contents);*/
        } catch (IOException e) {
            Log.e(TAG, "Error: " + e.getMessage());
        }

        /*if (device.getBondState() == BluetoothDevice.BOND_NONE) {
            // attempt to bond

        } else if (device.getBondState() == BluetoothDevice.BOND_BONDING) {
            // wait for bonding to finish

        } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
            // connect
            BluetoothClient client = new BluetoothClient(device);
        }*/
    }

    @Override
    protected int getThemeStyle() {
        return R.style.SwapTheme_BluetoothDeviceList;
    }

    @Override
    protected int getHeaderLayout() {
        return R.layout.swap_bluetooth_header;
    }

    private class Adapter extends ArrayAdapter<BluetoothDevice> {

        public Adapter(Context context, int resource) {
            super(context, resource);
        }

        public Adapter(Context context, int resource, int textViewResourceId) {
            super(context, resource, textViewResourceId);
        }

        public Adapter(Context context, int resource, BluetoothDevice[] objects) {
            super(context, resource, objects);
        }

        public Adapter(Context context, int resource, int textViewResourceId, BluetoothDevice[] objects) {
            super(context, resource, textViewResourceId, objects);
        }

        public Adapter(Context context, int resource, List<BluetoothDevice> objects) {
            super(context, resource, objects);
        }

        public Adapter(Context context, int resource, int textViewResourceId, List<BluetoothDevice> objects) {
            super(context, resource, textViewResourceId, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = getActivity().getLayoutInflater().inflate(R.layout.simple_list_item_3, null);
            } else {
                view = convertView;
            }

            BluetoothDevice device = getItem(position);
            TextView nameView = (TextView)view.findViewById(android.R.id.text1);
            TextView addressView = (TextView)view.findViewById(android.R.id.text2);
            TextView descriptionView = (TextView)view.findViewById(R.id.text3);

            nameView.setText(device.getName());
            addressView.setText(device.getAddress());
            descriptionView.setText(bondStateToLabel(device.getBondState()));

            return view;
        }

        private String bondStateToLabel(int deviceBondState)
        {
            if (deviceBondState == BluetoothDevice.BOND_BONDED) {
                // TODO: Is the term "Bonded device" common parlance among phone users?
                // It sounds a bit technical to me, maybe something more lay like "Previously connected".
                // Although it is technically not as accurate, it would make sense to more people...
                return "Bonded";
            } else if (deviceBondState == BluetoothDevice.BOND_BONDING) {
                return "Currently bonding...";
            } else {
                // TODO: Might be a little bit harsh, makes it sound more malicious than it should.
                return "Unknown device";
            }
        }
    }

}
