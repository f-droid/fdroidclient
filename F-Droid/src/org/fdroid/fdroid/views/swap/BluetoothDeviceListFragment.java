package org.fdroid.fdroid.views.swap;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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

    private static final String TAG = "fdroid.BluetoothList";

    private Adapter adapter = null;

    private MenuItem scanMenuItem;
    private MenuItem cancelMenuItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.swap_scan, menu);

        final int flags = MenuItemCompat.SHOW_AS_ACTION_ALWAYS | MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT;

        scanMenuItem = menu.findItem(R.id.action_scan);
        scanMenuItem.setVisible(true);
        MenuItemCompat.setShowAsAction(scanMenuItem, flags);

        cancelMenuItem = menu.findItem(R.id.action_cancel);
        cancelMenuItem.setVisible(false);
        MenuItemCompat.setShowAsAction(cancelMenuItem, flags);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_scan) {
            initiateBluetoothScan();
        } else if (item.getItemId() == R.id.action_cancel) {
            cancelBluetoothScan();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText("No bluetooth devices found. Is the other device \"discoverable\"?");

        adapter = new Adapter(
            new ContextThemeWrapper(getActivity(), R.style.SwapTheme_BluetoothDeviceList_ListItem),
            R.layout.select_local_apps_list_item
        );

        populateBondedDevices();

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
            final BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();

            final TextView deviceName = (TextView)headerView.findViewById(R.id.device_name);
            deviceName.setText(bluetooth.getName());

            final TextView address = (TextView)headerView.findViewById(R.id.device_address);
            address.setText(bluetooth.getAddress());
        }
        return view;
    }

    private void cancelBluetoothScan() {

        Log.d(TAG, "Cancelling bluetooth scan.");

        cancelMenuItem.setVisible(false);
        scanMenuItem.setVisible(true);

        final BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        bluetooth.cancelDiscovery();

        getLoadingIndicator().hide();

    }

    private ContentLoadingProgressBar getLoadingIndicator() {
        return ((ContentLoadingProgressBar)getView().findViewById(R.id.loading_indicator));
    }

    private void initiateBluetoothScan()
    {
        Log.d(TAG, "Starting bluetooth scan...");

        cancelMenuItem.setVisible(true);
        scanMenuItem.setVisible(false);

        final ContentLoadingProgressBar loadingBar = getLoadingIndicator();

        loadingBar.show();


        final BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        final BroadcastReceiver deviceFoundReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.d(TAG, "Found bluetooth device: " + device.toString());
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

        final BroadcastReceiver scanCompleteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Scan complete: " + intent.getAction());
                loadingBar.hide();
                cancelMenuItem.setVisible(false);
                scanMenuItem.setVisible(true);
            }
        };

        getActivity().registerReceiver(deviceFoundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        getActivity().registerReceiver(scanCompleteReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

        if (!bluetooth.startDiscovery()) {
            // TODO: Discovery did not start for some reason :(
            Log.e(TAG, "Could not start bluetooth discovery, but am not sure why :(");
        }
    }

    private void populateBondedDevices()
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

            nameView.setText(device.getName() == null ? getString(R.string.unknown) : device.getName());
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
