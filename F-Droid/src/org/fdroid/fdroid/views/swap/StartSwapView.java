package org.fdroid.fdroid.views.swap;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.localrepo.SwapService;
import org.fdroid.fdroid.localrepo.peers.Peer;
import org.fdroid.fdroid.net.WifiStateChangeService;

import java.util.ArrayList;

import cc.mvdan.accesspoint.WifiApControl;

public class StartSwapView extends ScrollView implements SwapWorkflowActivity.InnerView {

    private static final String TAG = "StartSwapView";

    // TODO: Is there a way to guarangee which of these constructors the inflater will call?
    // Especially on different API levels? It would be nice to only have the one which accepts
    // a Context, but I'm not sure if that is correct or not. As it stands, this class provides
    // constructurs which match each of the ones available in the parent class.
    // The same is true for the other views in the swap process too.

    public StartSwapView(Context context) {
        super(context);
    }

    public StartSwapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public StartSwapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public StartSwapView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private class PeopleNearbyAdapter extends ArrayAdapter<Peer> {

        public PeopleNearbyAdapter(Context context) {
            super(context, 0, new ArrayList<Peer>());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.swap_peer_list_item, parent, false);
            }

            Peer peer = getItem(position);
            ((TextView)convertView.findViewById(R.id.peer_name)).setText(peer.getName());
            ((ImageView)convertView.findViewById(R.id.icon)).setImageDrawable(getResources().getDrawable(peer.getIcon()));

            return convertView;
        }


    }

    private SwapWorkflowActivity getActivity() {
        // TODO: Try and find a better way to get to the SwapActivity, which makes less asumptions.
        return (SwapWorkflowActivity)getContext();
    }

    private SwapService getManager() {
        return getActivity().getState();
    }

    @Nullable /* Emulators typically don't have bluetooth adapters */
    private final BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();

    private TextView viewBluetoothId;
    private TextView viewWifiId;
    private TextView viewWifiNetwork;
    private TextView peopleNearbyText;
    private ListView peopleNearbyList;
    private ProgressBar peopleNearbyProgress;

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        getManager().scanForPeers();

        uiInitPeers();
        uiInitBluetooth();
        uiInitWifi();
        uiInitButtons();
        uiUpdatePeersInfo();

        // TODO: Unregister this receiver at some point.
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        uiUpdateWifiNetwork();
                    }
                },
                new IntentFilter(WifiStateChangeService.BROADCAST)
        );
    }

    private void uiInitButtons() {
        findViewById(R.id.btn_send_fdroid).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().sendFDroid();
            }
        });

        findViewById(R.id.btn_qr_scanner).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().startQrWorkflow();
            }
        });
    }

    /**
     * Setup the list of nearby peers with an adapter, and hide or show it and the associated
     * message for when no peers are nearby depending on what is happening.
     */
    private void uiInitPeers() {

        peopleNearbyText = (TextView)findViewById(R.id.text_people_nearby);
        peopleNearbyList = (ListView)findViewById(R.id.list_people_nearby);
        peopleNearbyProgress = (ProgressBar)findViewById(R.id.searching_people_nearby);

        final PeopleNearbyAdapter adapter = new PeopleNearbyAdapter(getContext());
        peopleNearbyList.setAdapter(adapter);
        uiUpdatePeersInfo();

        peopleNearbyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Peer peer = adapter.getItem(position);
                onPeerSelected(peer);
            }
        });

        // TODO: Unregister this receiver at the right time.
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Peer peer = intent.getParcelableExtra(SwapService.EXTRA_PEER);
                if (adapter.getPosition(peer) >= 0) {
                    Log.d(TAG, "Found peer: " + peer + ", ignoring though, because it is already in our list.");
                } else {
                    Log.d(TAG, "Found peer: " + peer + ", adding to list of peers in UI.");
                    adapter.add(peer);
                    uiUpdatePeersInfo();
                }
            }
        }, new IntentFilter(SwapService.ACTION_PEER_FOUND));

    }

    private void uiUpdatePeersInfo() {
        if (getManager().isScanningForPeers()) {
            peopleNearbyText.setText(getContext().getString(R.string.swap_scanning_for_peers));
            peopleNearbyProgress.setVisibility(View.VISIBLE);
        } else {
            peopleNearbyProgress.setVisibility(View.GONE);
            if (peopleNearbyList.getAdapter().getCount() > 0) {
                peopleNearbyText.setText(getContext().getString(R.string.swap_people_nearby));
            } else {
                peopleNearbyText.setText(getContext().getString(R.string.swap_no_peers_nearby));
            }
        }

    }

    private void uiInitBluetooth() {
        if (bluetooth != null) {

            final TextView textBluetoothVisible = (TextView)findViewById(R.id.bluetooth_visible);

            viewBluetoothId = (TextView)findViewById(R.id.device_id_bluetooth);
            viewBluetoothId.setText(bluetooth.getName());
            viewBluetoothId.setVisibility(bluetooth.isEnabled() ? View.VISIBLE : View.GONE);

            int textResource = getManager().isBluetoothDiscoverable() ? R.string.swap_visible_bluetooth : R.string.swap_not_visible_bluetooth;
            textBluetoothVisible.setText(textResource);

            final SwitchCompat bluetoothSwitch = ((SwitchCompat) findViewById(R.id.switch_bluetooth));
            bluetoothSwitch.setChecked(getManager().isBluetoothDiscoverable());
            bluetoothSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        getActivity().startBluetoothSwap();
                        textBluetoothVisible.setText(R.string.swap_visible_bluetooth);
                        viewBluetoothId.setVisibility(View.VISIBLE);
                        uiUpdatePeersInfo();
                        // TODO: When they deny the request for enabling bluetooth, we need to disable this switch...
                    } else {
                        getManager().getBluetoothSwap().stop();
                        textBluetoothVisible.setText(R.string.swap_not_visible_bluetooth);
                        viewBluetoothId.setVisibility(View.GONE);
                        uiUpdatePeersInfo();
                    }
                }
            });

            // TODO: Unregister receiver correctly...
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.hasExtra(SwapService.EXTRA_STARTING)) {
                        Log.d(TAG, "Bluetooth service is starting...");
                        bluetoothSwitch.setEnabled(false);
                        textBluetoothVisible.setText(R.string.swap_setting_up_bluetooth);
                        // bluetoothSwitch.setChecked(true);
                    } else {
                        bluetoothSwitch.setEnabled(true);
                        if (intent.hasExtra(SwapService.EXTRA_STARTED)) {
                            Log.d(TAG, "Bluetooth service has started.");
                            textBluetoothVisible.setText(R.string.swap_visible_bluetooth);
                            // bluetoothSwitch.setChecked(true);
                        } else {
                            Log.d(TAG, "Bluetooth service has stopped.");
                            textBluetoothVisible.setText(R.string.swap_not_visible_bluetooth);
                            bluetoothSwitch.setChecked(false);
                        }
                    }
                }
            }, new IntentFilter(SwapService.BLUETOOTH_STATE_CHANGE));

        } else {
            findViewById(R.id.bluetooth_info).setVisibility(View.GONE);
        }
    }

    private void uiInitWifi() {

        viewWifiId = (TextView)findViewById(R.id.device_id_wifi);
        viewWifiNetwork = (TextView)findViewById(R.id.wifi_network);

        final SwitchCompat wifiSwitch = (SwitchCompat)findViewById(R.id.switch_wifi);
        wifiSwitch.setChecked(getManager().isBonjourDiscoverable());
        wifiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    getManager().getWifiSwap().ensureRunningInBackground();
                } else {
                    getManager().getWifiSwap().stop();
                }
                uiUpdatePeersInfo();
                uiUpdateWifiNetwork();
            }
        });

        final TextView textWifiVisible = (TextView)findViewById(R.id.wifi_visible);
        int textResource = getManager().isBonjourDiscoverable() ? R.string.swap_visible_wifi : R.string.swap_not_visible_wifi;
        textWifiVisible.setText(textResource);

        // TODO: Unregister receiver correctly...
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasExtra(SwapService.EXTRA_STARTING)) {
                    Log.d(TAG, "Bonjour/WiFi service is starting...");
                    textWifiVisible.setText(R.string.swap_setting_up_wifi);
                    wifiSwitch.setEnabled(false);
                    wifiSwitch.setChecked(true);
                } else {
                    wifiSwitch.setEnabled(true);
                    if (intent.hasExtra(SwapService.EXTRA_STARTED)) {
                        Log.d(TAG, "Bonjour/WiFi service has started.");
                        textWifiVisible.setText(R.string.swap_visible_wifi);
                        wifiSwitch.setChecked(true);
                    } else {
                        Log.d(TAG, "Bonjour/WiFi service has stopped.");
                        textWifiVisible.setText(R.string.swap_not_visible_wifi);
                        wifiSwitch.setChecked(false);
                    }
                }
                uiUpdateWifiNetwork();
            }
        }, new IntentFilter(SwapService.BONJOUR_STATE_CHANGE));

        viewWifiNetwork.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().promptToSelectWifiNetwork();
            }
        });

        uiUpdateWifiNetwork();
    }

    private void uiUpdateWifiNetwork() {

        viewWifiId.setText(FDroidApp.ipAddressString);
        viewWifiId.setVisibility(TextUtils.isEmpty(FDroidApp.ipAddressString) ? View.GONE : View.VISIBLE);

        WifiApControl wifiAp = WifiApControl.getInstance(getActivity());
        if (wifiAp.isWifiApEnabled()) {
            WifiConfiguration config = wifiAp.getConfiguration();
            viewWifiNetwork.setText(getContext().getString(R.string.swap_active_hotspot, config.SSID));
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

    @Override
    public boolean buildMenu(Menu menu, @NonNull MenuInflater inflater) {
        return false;
    }

    @Override
    public int getStep() {
        return SwapService.STEP_INTRO;
    }

    @Override
    public int getPreviousStep() {
        // TODO: Currently this is handleed by the SwapWorkflowActivity as a special case, where
        // if getStep is STEP_INTRO, don't even bother asking for getPreviousStep. But that is a
        // bit messy. It would be nicer if this was handled using the same mechanism as everything
        // else.
        return SwapService.STEP_INTRO;
    }

    @Override
    @ColorRes
    public int getToolbarColour() {
        return R.color.swap_bright_blue;
    }

    @Override
    public String getToolbarTitle() {
        return getResources().getString(R.string.swap_nearby);
    }

}
