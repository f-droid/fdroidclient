package org.fdroid.fdroid.nearby;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiConfiguration;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.nearby.peers.Peer;

import java.util.ArrayList;

import cc.mvdan.accesspoint.WifiApControl;

@SuppressWarnings("LineLength")
public class StartSwapView extends SwapView {
    private static final String TAG = "StartSwapView";

    public StartSwapView(Context context) {
        super(context);
    }

    public StartSwapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StartSwapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public StartSwapView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    class PeopleNearbyAdapter extends ArrayAdapter<Peer> {

        PeopleNearbyAdapter(Context context) {
            super(context, 0, new ArrayList<Peer>());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.swap_peer_list_item, parent, false);
            }

            Peer peer = getItem(position);
            ((TextView) convertView.findViewById(R.id.peer_name)).setText(peer.getName());
            ((ImageView) convertView.findViewById(R.id.icon))
                    .setImageDrawable(ContextCompat.getDrawable(getContext(), peer.getIcon()));

            return convertView;
        }
    }

    @Nullable /* Emulators typically don't have bluetooth adapters */
    private final BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();

    private SwitchMaterial bluetoothSwitch;
    private TextView viewBluetoothId;
    private TextView textBluetoothVisible;
    private TextView viewWifiId;
    private TextView viewWifiNetwork;
    private TextView peopleNearbyText;
    private ListView peopleNearbyList;
    private ProgressBar peopleNearbyProgress;

    private PeopleNearbyAdapter peopleNearbyAdapter;

    /**
     * Remove relevant listeners/subscriptions/etc so that they do not receive and process events
     * when this view is not in use.
     * <p>
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (bluetoothSwitch != null) {
            bluetoothSwitch.setOnCheckedChangeListener(null);
        }

        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(onWifiNetworkChanged);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        uiInitPeers();
        uiInitBluetooth();
        uiInitWifi();
        uiInitButtons();

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                onWifiNetworkChanged, new IntentFilter(WifiStateChangeService.BROADCAST));
    }

    private final BroadcastReceiver onWifiNetworkChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            uiUpdateWifiNetwork();
        }
    };

    private void uiInitButtons() {
        MaterialButton sendFDroidButton = findViewById(R.id.btn_send_fdroid);
        sendFDroidButton.setEllipsize(TextUtils.TruncateAt.END);
        findViewById(R.id.btn_send_fdroid).setOnClickListener(v -> getActivity().sendFDroid());
    }

    /**
     * Setup the list of nearby peers with an adapter, and hide or show it and the associated
     * message for when no peers are nearby depending on what is happening.
     *
     * @see SwapWorkflowActivity#bonjourFound
     * @see SwapWorkflowActivity#bluetoothFound
     */
    private void uiInitPeers() {

        peopleNearbyText = (TextView) findViewById(R.id.text_people_nearby);
        peopleNearbyList = (ListView) findViewById(R.id.list_people_nearby);
        peopleNearbyProgress = (ProgressBar) findViewById(R.id.searching_people_nearby);

        peopleNearbyAdapter = new PeopleNearbyAdapter(getContext());
        peopleNearbyList.setAdapter(peopleNearbyAdapter);
        for (Peer peer : getActivity().getSwapService().getActivePeers()) {
            if (peopleNearbyAdapter.getPosition(peer) == -1) {
                peopleNearbyAdapter.add(peer);
            }
        }

        peopleNearbyList.setOnItemClickListener((parent, view, position, id) -> {
            Peer peer = peopleNearbyAdapter.getItem(position);
            onPeerSelected(peer);
        });
    }

    private void uiInitBluetooth() {
        if (bluetooth != null && ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {

            viewBluetoothId = (TextView) findViewById(R.id.device_id_bluetooth);
            viewBluetoothId.setText(bluetooth.getName());
            viewBluetoothId.setVisibility(bluetooth.isEnabled() ? View.VISIBLE : View.GONE);

            textBluetoothVisible = findViewById(R.id.bluetooth_visible);

            bluetoothSwitch = (SwitchMaterial) findViewById(R.id.switch_bluetooth);
            bluetoothSwitch.setOnCheckedChangeListener(onBluetoothSwitchToggled);
            bluetoothSwitch.setChecked(SwapService.getBluetoothVisibleUserPreference());
            bluetoothSwitch.setEnabled(true);
            bluetoothSwitch.setOnCheckedChangeListener(onBluetoothSwitchToggled);
        } else {
            findViewById(R.id.bluetooth_info).setVisibility(View.GONE);
        }
    }

    private final CompoundButton.OnCheckedChangeListener onBluetoothSwitchToggled = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                Utils.debugLog(TAG, "Received onCheckChanged(true) for Bluetooth swap, prompting user as to whether they want to enable Bluetooth.");
                getActivity().startBluetoothSwap();
                textBluetoothVisible.setText(R.string.swap_visible_bluetooth);
                viewBluetoothId.setVisibility(View.VISIBLE);
                Utils.debugLog(TAG, "Received onCheckChanged(true) for Bluetooth swap (prompting user or setup Bluetooth complete)");
                // TODO: When they deny the request for enabling bluetooth, we need to disable this switch...
            } else {
                Utils.debugLog(TAG, "Received onCheckChanged(false) for Bluetooth swap, disabling Bluetooth swap.");
                BluetoothManager.stop(getContext());
                textBluetoothVisible.setText(R.string.swap_not_visible_bluetooth);
                viewBluetoothId.setVisibility(View.GONE);
                Utils.debugLog(TAG, "Received onCheckChanged(false) for Bluetooth swap, Bluetooth swap disabled successfully.");
            }
            SwapService.putBluetoothVisibleUserPreference(isChecked);
        }
    };

    private void uiInitWifi() {

        viewWifiId = (TextView) findViewById(R.id.device_id_wifi);
        viewWifiNetwork = (TextView) findViewById(R.id.wifi_network);

        uiUpdateWifiNetwork();
    }

    private void uiUpdateWifiNetwork() {

        viewWifiId.setText(FDroidApp.ipAddressString);
        viewWifiId.setVisibility(TextUtils.isEmpty(FDroidApp.ipAddressString) ? View.GONE : View.VISIBLE);

        WifiApControl wifiAp = WifiApControl.getInstance(getActivity());
        if (wifiAp != null && wifiAp.isWifiApEnabled()) {
            WifiConfiguration config = wifiAp.getConfiguration();
            TextView textWifiVisible = findViewById(R.id.wifi_visible);
            if (textWifiVisible != null) {
                textWifiVisible.setText(R.string.swap_visible_hotspot);
            }
            Context context = getContext();
            if (config == null) {
                viewWifiNetwork.setText(context.getString(R.string.swap_active_hotspot,
                        context.getString(R.string.swap_blank_wifi_ssid)));
            } else {
                viewWifiNetwork.setText(context.getString(R.string.swap_active_hotspot, config.SSID));
            }
        } else if (TextUtils.isEmpty(FDroidApp.ssid)) {
            // not connected to or setup with any wifi network
            viewWifiNetwork.setText(R.string.swap_no_wifi_network);
        } else {
            // connected to a regular wifi network
            viewWifiNetwork.setText(FDroidApp.ssid);
        }
    }

    private void onPeerSelected(Peer peer) {
        getActivity().swapWith(peer);
    }
}
