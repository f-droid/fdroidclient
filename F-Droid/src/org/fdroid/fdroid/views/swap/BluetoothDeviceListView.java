package org.fdroid.fdroid.views.swap;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.localrepo.SwapManager;
import org.fdroid.fdroid.net.BluetoothDownloader;
import org.fdroid.fdroid.net.bluetooth.BluetoothClient;
import org.fdroid.fdroid.net.bluetooth.BluetoothConnection;
import org.fdroid.fdroid.net.bluetooth.BluetoothServer;
import org.fdroid.fdroid.views.fragments.ThemeableListFragment;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class BluetoothDeviceListView extends ListView implements
        SwapWorkflowActivity.InnerView,
        ListView.OnItemClickListener {

    private static final String TAG = "BluetoothDeviceListView";

    private Adapter adapter = null;

    private MenuItem scanMenuItem;
    private MenuItem cancelMenuItem;

    private boolean firstScan = true;

    public BluetoothDeviceListView(Context context) {
        super(context);
    }

    public BluetoothDeviceListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BluetoothDeviceListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BluetoothDeviceListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean buildMenu(Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.swap_scan, menu);

        final int flags = MenuItemCompat.SHOW_AS_ACTION_ALWAYS | MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT;

        scanMenuItem = menu.findItem(R.id.action_scan);
        scanMenuItem.setVisible(true);
        MenuItemCompat.setShowAsAction(scanMenuItem, flags);

        scanMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                initiateBluetoothScan();
                return true;
            }
        });

        cancelMenuItem = menu.findItem(R.id.action_cancel);
        cancelMenuItem.setVisible(false);
        MenuItemCompat.setShowAsAction(cancelMenuItem, flags);

        cancelMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                cancelBluetoothScan();
                return true;
            }
        });

        return true;
    }

    @Override
    public int getStep() {
        return SwapManager.STEP_BLUETOOTH;
    }

    @Override
    public int getPreviousStep() {
        return SwapManager.STEP_JOIN_WIFI;
    }

    @Override
    public int getToolbarColour() {
        return R.color.swap_blue;
    }

    @Override
    public String getToolbarTitle() {
        return getContext().getString(R.string.swap_use_bluetooth);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        adapter = new Adapter(
                getContext(),
                R.layout.select_local_apps_list_item
        );

        LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View headerView = inflater.inflate(R.layout.swap_bluetooth_header, this, false);
        addHeaderView(headerView);

        setAdapter(adapter);
        setOnItemClickListener(this);

        final BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();

        final TextView deviceName = (TextView) headerView.findViewById(R.id.device_name);
        deviceName.setText(bluetooth.getName());

        final TextView address = (TextView) headerView.findViewById(R.id.device_address);
        address.setText(bluetooth.getAddress());

        initiateBluetoothScan();

       // populateBondedDevices();

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
        return ((ContentLoadingProgressBar)findViewById(R.id.loading_indicator));
    }

    private void initiateBluetoothScan()
    {
        Log.d(TAG, "Starting bluetooth scan...");

        if (cancelMenuItem != null) {
            cancelMenuItem.setVisible(true);
            scanMenuItem.setVisible(false);
        }

        final ContentLoadingProgressBar loadingBar = getLoadingIndicator();

        loadingBar.show();

        final BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();

        if (firstScan) {
            final BroadcastReceiver deviceFoundReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        Log.d(TAG, "Found bluetooth device: " + device.toString());

                        if (device != null && device.getName() != null)
                            if (device.getName().contains(BluetoothServer.BLUETOOTH_NAME_TAG)) {
                                boolean exists = false;
                                for (int i = 0; i < adapter.getCount(); i++) {
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

            getContext().registerReceiver(deviceFoundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            getContext().registerReceiver(scanCompleteReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

            firstScan = false;
        }
        else
        {
            if (bluetooth.isDiscovering())
            {
                bluetooth.cancelDiscovery();
            }
        }

        if (!bluetooth.startDiscovery()) {
            // TODO: Discovery did not start for some reason :(
            Log.e(TAG, "Could not start bluetooth discovery, but am not sure why :(");
            Toast.makeText(getContext(),"There was a problem looking for Bluetooth devices",Toast.LENGTH_SHORT).show();
        }
    }

    private void populateBondedDevices()
    {
        for (BluetoothDevice device : BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
            adapter.add(device);
        }
    }



    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

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
                LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.simple_list_item_3, null);
            } else {
                view = convertView;
            }

            BluetoothDevice device = getItem(position);
            TextView nameView = (TextView)view.findViewById(android.R.id.text1);
            TextView addressView = (TextView)view.findViewById(android.R.id.text2);
            //TextView descriptionView = (TextView)view.findViewById(R.id.text3);

            nameView.setText(device.getName() == null ? getContext().getString(R.string.unknown) : device.getName());
            addressView.setText(device.getAddress());
            //descriptionView.setText(bondStateToLabel(device.getBondState()));

            return view;
        }

        private String bondStateToLabel(int deviceBondState)
        {
            if (deviceBondState == BluetoothDevice.BOND_BONDED) {
                // TODO: Is the term "Bonded device" common parlance among phone users?
                // It sounds a bit technical to me, maybe something more lay like "Previously connected".
                // Although it is technically not as accurate, it would make sense to more people...
                return getContext().getString(R.string.swap_bluetooth_bonded_device);
            } else if (deviceBondState == BluetoothDevice.BOND_BONDING) {
                return getContext().getString(R.string.swap_bluetooth_bonding_device);
            } else {
                // TODO: Might be a little bit harsh, makes it sound more malicious than it should.
                return getContext().getString(R.string.swap_bluetooth_unknown_device);
            }
        }
    }

}
