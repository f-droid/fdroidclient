package org.fdroid.fdroid.views.swap;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import cc.mvdan.accesspoint.WifiApControl;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.localrepo.SwapService;
import org.fdroid.fdroid.localrepo.peers.Peer;
import org.fdroid.fdroid.net.WifiStateChangeService;
import rx.Subscriber;
import rx.Subscription;

import java.util.ArrayList;

@SuppressWarnings("LineLength")
public class StartSwapView extends RelativeLayout implements SwapWorkflowActivity.InnerView {

    private static final String TAG = "StartSwapView";

    // TODO: Is there a way to guarantee which of these constructors the inflater will call?
    // Especially on different API levels? It would be nice to only have the one which accepts
    // a Context, but I'm not sure if that is correct or not. As it stands, this class provides
    // constructors which match each of the ones available in the parent class.
    // The same is true for the other views in the swap process too.

    public StartSwapView(Context context) {
        super(context);
    }

    public StartSwapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(11)
    public StartSwapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public StartSwapView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private class PeopleNearbyAdapter extends ArrayAdapter<Peer> {

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
                    .setImageDrawable(getResources().getDrawable(peer.getIcon()));

            return convertView;
        }

    }

    private SwapWorkflowActivity getActivity() {
        return (SwapWorkflowActivity) getContext();
    }

    private SwapService getManager() {
        return getActivity().getState();
    }

    @Nullable /* Emulators typically don't have bluetooth adapters */
    private final BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();

    private SwitchCompat wifiSwitch;
    private SwitchCompat bluetoothSwitch;
    private TextView textWifiVisible;
    private TextView viewBluetoothId;
    private TextView textBluetoothVisible;
    private TextView viewWifiId;
    private TextView viewWifiNetwork;
    private TextView peopleNearbyText;
    private ListView peopleNearbyList;
    private ProgressBar peopleNearbyProgress;

    private PeopleNearbyAdapter peopleNearbyAdapter;

    /**
     * When peers are emitted by the peer finder, add them to the adapter
     * so that they will show up in the list of peers.
     */
    private final Subscriber<Peer> onPeerFound = new Subscriber<Peer>() {

        @Override
        public void onCompleted() {
            uiShowNotSearchingForPeers();
        }

        @Override
        public void onError(Throwable e) {
            uiShowNotSearchingForPeers();
        }

        @Override
        public void onNext(Peer peer) {
            Utils.debugLog(TAG, "Found peer: " + peer + ", adding to list of peers in UI.");
            peopleNearbyAdapter.add(peer);
        }
    };

    private Subscription peerFinderSubscription;

    /**
     * Remove relevant listeners/subscriptions/etc so that they do not receive and process events
     * when this view is not in use.
     * <p>
     * TODO: Not sure if this is the best place to handle being removed from the view.
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (peerFinderSubscription != null) {
            peerFinderSubscription.unsubscribe();
            peerFinderSubscription = null;
        }

        if (wifiSwitch != null) {
            wifiSwitch.setOnCheckedChangeListener(null);
        }

        if (bluetoothSwitch != null) {
            bluetoothSwitch.setOnCheckedChangeListener(null);
        }

        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(onWifiSwapStateChanged);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(onBluetoothSwapStateChanged);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(onWifiNetworkChanged);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (peerFinderSubscription == null) {
            peerFinderSubscription = getManager().scanForPeers().subscribe(onPeerFound);
        }

        uiInitPeers();
        uiInitBluetooth();
        uiInitWifi();
        uiInitButtons();
        uiShowSearchingForPeers();

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

        peopleNearbyText = (TextView) findViewById(R.id.text_people_nearby);
        peopleNearbyList = (ListView) findViewById(R.id.list_people_nearby);
        peopleNearbyProgress = (ProgressBar) findViewById(R.id.searching_people_nearby);

        peopleNearbyAdapter = new PeopleNearbyAdapter(getContext());
        peopleNearbyList.setAdapter(peopleNearbyAdapter);

        peopleNearbyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Peer peer = peopleNearbyAdapter.getItem(position);
                onPeerSelected(peer);
            }
        });
    }

    private void uiShowSearchingForPeers() {
        peopleNearbyText.setText(getContext().getString(R.string.swap_scanning_for_peers));
        peopleNearbyProgress.setVisibility(View.VISIBLE);
    }

    private void uiShowNotSearchingForPeers() {
        peopleNearbyProgress.setVisibility(View.GONE);
        if (peopleNearbyList.getAdapter().getCount() > 0) {
            peopleNearbyText.setText(getContext().getString(R.string.swap_people_nearby));
        } else {
            peopleNearbyText.setText(getContext().getString(R.string.swap_no_peers_nearby));
        }
    }

    private void uiInitBluetooth() {
        if (bluetooth != null) {

            textBluetoothVisible = (TextView) findViewById(R.id.bluetooth_visible);

            viewBluetoothId = (TextView) findViewById(R.id.device_id_bluetooth);
            viewBluetoothId.setText(bluetooth.getName());
            viewBluetoothId.setVisibility(bluetooth.isEnabled() ? View.VISIBLE : View.GONE);

            int textResource = getManager().isBluetoothDiscoverable() ? R.string.swap_visible_bluetooth : R.string.swap_not_visible_bluetooth;
            textBluetoothVisible.setText(textResource);

            bluetoothSwitch = (SwitchCompat) findViewById(R.id.switch_bluetooth);
            Utils.debugLog(TAG, getManager().isBluetoothDiscoverable() ? "Initially marking switch as checked, because Bluetooth is discoverable." : "Initially marking switch as not-checked, because Bluetooth is not discoverable.");
            bluetoothSwitch.setOnCheckedChangeListener(onBluetoothSwitchToggled);
            setBluetoothSwitchState(getManager().isBluetoothDiscoverable(), true);

            LocalBroadcastManager.getInstance(getContext()).registerReceiver(onBluetoothSwapStateChanged, new IntentFilter(SwapService.BLUETOOTH_STATE_CHANGE));

        } else {
            findViewById(R.id.bluetooth_info).setVisibility(View.GONE);
        }
    }

    /**
     * @see StartSwapView#onWifiSwapStateChanged
     */
    private final BroadcastReceiver onBluetoothSwapStateChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(SwapService.EXTRA_STARTING)) {
                Utils.debugLog(TAG, "Bluetooth service is starting (setting toggle to disabled, not checking because we will wait for an intent that bluetooth is actually enabled)");
                bluetoothSwitch.setEnabled(false);
                textBluetoothVisible.setText(R.string.swap_setting_up_bluetooth);
                // bluetoothSwitch.setChecked(true);
            } else {
                if (intent.hasExtra(SwapService.EXTRA_STARTED)) {
                    Utils.debugLog(TAG, "Bluetooth service has started (updating text to visible, but not marking as checked).");
                    textBluetoothVisible.setText(R.string.swap_visible_bluetooth);
                    bluetoothSwitch.setEnabled(true);
                    // bluetoothSwitch.setChecked(true);
                } else {
                    Utils.debugLog(TAG, "Bluetooth service has stopped (setting switch to not-visible).");
                    textBluetoothVisible.setText(R.string.swap_not_visible_bluetooth);
                    setBluetoothSwitchState(false, true);
                }
            }
        }
    };

    /**
     * @see StartSwapView#setWifiSwitchState(boolean, boolean)
     */
    private void setBluetoothSwitchState(boolean isChecked, boolean isEnabled) {
        bluetoothSwitch.setOnCheckedChangeListener(null);
        bluetoothSwitch.setChecked(isChecked);
        bluetoothSwitch.setEnabled(isEnabled);
        bluetoothSwitch.setOnCheckedChangeListener(onBluetoothSwitchToggled);
    }

    /**
     * @see StartSwapView#onWifiSwitchToggled
     */
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
                getManager().getBluetoothSwap().stop();
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

        wifiSwitch = (SwitchCompat) findViewById(R.id.switch_wifi);
        wifiSwitch.setOnCheckedChangeListener(onWifiSwitchToggled);
        setWifiSwitchState(getManager().isBonjourDiscoverable(), true);

        textWifiVisible = (TextView) findViewById(R.id.wifi_visible);
        int textResource = getManager().isBonjourDiscoverable() ? R.string.swap_visible_wifi : R.string.swap_not_visible_wifi;
        textWifiVisible.setText(textResource);

        // Note that this is only listening for the WifiSwap, whereas we start both the WifiSwap
        // and the Bonjour service at the same time. Technically swap will work fine without
        // Bonjour, and that is more of a convenience. Thus, we should show feedback once wifi
        // is ready, even if Bonjour is not yet.
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(onWifiSwapStateChanged,
                new IntentFilter(SwapService.WIFI_STATE_CHANGE));

        viewWifiNetwork.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().promptToSelectWifiNetwork();
            }
        });

        uiUpdateWifiNetwork();
    }

    /**
     * When the WiFi swap service is started or stopped, update the UI appropriately.
     * This includes both the in-transit states of "Starting" and "Stopping". In these two cases,
     * the UI should be disabled to prevent the user quickly switching back and forth - causing
     * multiple start/stop actions to be sent to the swap service.
     */
    private final BroadcastReceiver onWifiSwapStateChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(SwapService.EXTRA_STARTING)) {
                Utils.debugLog(TAG, "WiFi service is starting (setting toggle to checked, but disabled).");
                textWifiVisible.setText(R.string.swap_setting_up_wifi);
                setWifiSwitchState(true, false);
            } else if (intent.hasExtra(SwapService.EXTRA_STOPPING)) {
                Utils.debugLog(TAG, "WiFi service is stopping (setting toggle to unchecked and disabled).");
                textWifiVisible.setText(R.string.swap_stopping_wifi);
                setWifiSwitchState(false, false);
            } else {
                if (intent.hasExtra(SwapService.EXTRA_STARTED)) {
                    Utils.debugLog(TAG, "WiFi service has started (setting toggle to visible).");
                    textWifiVisible.setText(R.string.swap_visible_wifi);
                    setWifiSwitchState(true, true);
                } else {
                    Utils.debugLog(TAG, "WiFi service has stopped (setting toggle to not-visible).");
                    textWifiVisible.setText(R.string.swap_not_visible_wifi);
                    setWifiSwitchState(false, true);
                }
            }
            uiUpdateWifiNetwork();
        }
    };

    /**
     * Helper function to set the "enable wifi" switch, but prevents the listeners from
     * being notified. This enables the UI to be updated without triggering further enable/disable
     * events being queued.
     * <p>
     * This is required because the SwitchCompat and its parent classes will always try to notify
     * their listeners if there is one (e.g. http://stackoverflow.com/a/15523518).
     * <p>
     * The fact that this method also deals with enabling/disabling the switch is more of a convenience
     * Nigh on all times this UI wants to change the state of the switch, it is also interested in
     * ensuring the enabled state of the switch.
     */
    private void setWifiSwitchState(boolean isChecked, boolean isEnabled) {
        wifiSwitch.setOnCheckedChangeListener(null);
        wifiSwitch.setChecked(isChecked);
        wifiSwitch.setEnabled(isEnabled);
        wifiSwitch.setOnCheckedChangeListener(onWifiSwitchToggled);
    }

    /**
     * When the wifi switch is:
     * <p>
     * Toggled on: Ask the swap service to ensure wifi swap is running.
     * Toggled off: Ask the swap service to prevent the wifi swap service from running.
     * <p>
     * Both of these actions will be performed in a background thread which will send broadcast
     * intents when they are completed.
     */
    private final CompoundButton.OnCheckedChangeListener onWifiSwitchToggled = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                Utils.debugLog(TAG, "Received onCheckChanged(true) for WiFi swap, asking in background thread to ensure WiFi swap is running.");
                getManager().getWifiSwap().ensureRunningInBackground();
            } else {
                Utils.debugLog(TAG, "Received onCheckChanged(false) for WiFi swap, disabling WiFi swap in background thread.");
                getManager().getWifiSwap().stopInBackground();
            }
            SwapService.putWifiVisibleUserPreference(isChecked);
            uiUpdateWifiNetwork();
        }
    };

    private void uiUpdateWifiNetwork() {

        viewWifiId.setText(FDroidApp.ipAddressString);
        viewWifiId.setVisibility(TextUtils.isEmpty(FDroidApp.ipAddressString) ? View.GONE : View.VISIBLE);

        WifiApControl wifiAp = WifiApControl.getInstance(getActivity());
        if (wifiAp != null && wifiAp.isWifiApEnabled()) {
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
