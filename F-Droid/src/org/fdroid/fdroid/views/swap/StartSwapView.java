package org.fdroid.fdroid.views.swap;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.localrepo.SwapManager;
import org.fdroid.fdroid.localrepo.peers.Peer;
import org.fdroid.fdroid.localrepo.peers.PeerFinder;

public class StartSwapView extends LinearLayout implements SwapWorkflowActivity.InnerView {

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
            super(context, 0, SwapManager.load(context).getPeers());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.two_line_list_item, parent, false);
            }

            Peer peer = getItem(position);
            ((TextView)convertView.findViewById(android.R.id.text1)).setText(peer.getName());

            return convertView;
        }


    }

    private SwapWorkflowActivity getActivity() {
        // TODO: Try and find a better way to get to the SwapActivity, which makes less asumptions.
        return (SwapWorkflowActivity)getContext();
    }

    private SwapManager getManager() {
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

        uiInitPeers();
        uiInitBluetooth();
        uiInitWifi();
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

        SwapManager.load(getActivity()).setPeerListener(new PeerFinder.Listener<Peer>() {
            @Override
            public void onPeerFound(Peer peer) {
                adapter.notifyDataSetChanged();
                uiUpdatePeersInfo();
            }
        });

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

            Switch bluetoothSwitch = ((Switch) findViewById(R.id.switch_bluetooth));
            bluetoothSwitch.setChecked(getManager().isBluetoothDiscoverable());
            bluetoothSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        getManager().ensureBluetoothDiscoverable();
                        getManager().scanForPeers();
                        textBluetoothVisible.setText(getContext().getString(R.string.swap_visible_bluetooth));
                        uiUpdatePeersInfo();
                        // TODO: When they deny the request for enabling bluetooth, we need to disable this switch...
                    } else {
                        getManager().cancelScanningForPeers();
                        getManager().makeBluetoothNonDiscoverable();
                        textBluetoothVisible.setText(getContext().getString(R.string.swap_not_visible_bluetooth));
                        uiUpdatePeersInfo();
                    }
                }
            });
        } else {
            findViewById(R.id.bluetooth_info).setVisibility(View.GONE);
        }
    }

    private void uiInitWifi() {

        final TextView textBluetoothVisible = (TextView)findViewById(R.id.bluetooth_visible);

        viewWifiId = (TextView)findViewById(R.id.device_id_wifi);
        viewWifiNetwork = (TextView)findViewById(R.id.wifi_network);

        Switch wifiSwitch = (Switch)findViewById(R.id.switch_wifi);
        wifiSwitch.setChecked(getManager().isBonjourDiscoverable());
        wifiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    textBluetoothVisible.setText(getContext().getString(R.string.swap_visible_wifi));
                    uiUpdatePeersInfo();
                } else {
                    textBluetoothVisible.setText(getContext().getString(R.string.swap_not_visible_wifi));
                    uiUpdatePeersInfo();
                }
            }
        });

        uiUpdateWifi();
    }

    private void uiUpdateWifi() {
        viewWifiId.setText(FDroidApp.ipAddressString);

        if (TextUtils.isEmpty(FDroidApp.bssid) && !TextUtils.isEmpty(FDroidApp.ipAddressString)) {
            // empty bssid with an ipAddress means hotspot mode
            viewWifiNetwork.setText(getContext().getString(R.string.swap_active_hotspot));
        } else if (TextUtils.isEmpty(FDroidApp.ssid)) {
            // not connected to or setup with any wifi network
            viewWifiNetwork.setText(getContext().getString(R.string.swap_no_wifi_network));
        } else {
            // connected to a regular wifi network
            viewWifiNetwork.setText(FDroidApp.ssid);
        }
    }

    @Override
    public boolean buildMenu(Menu menu, @NonNull MenuInflater inflater) {
        return false;
    }

    @Override
    public int getStep() {
        return SwapManager.STEP_INTRO;
    }

    @Override
    public int getPreviousStep() {
        // TODO: Currently this is handleed by the SwapWorkflowActivity as a special case, where
        // if getStep is STEP_INTRO, don't even bother asking for getPreviousStep. But that is a
        // bit messy. It would be nicer if this was handled using the same mechanism as everything
        // else.
        return SwapManager.STEP_INTRO;
    }

    @Override
    @ColorRes
    public int getToolbarColour() {
        return getResources().getColor(R.color.swap_bright_blue);
    }

    @Override
    public String getToolbarTitle() {
        return getResources().getString(R.string.swap_nearby);
    }

}
