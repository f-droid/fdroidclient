package org.fdroid.fdroid.views.swap;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.LinearLayout;

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
    private View noPeopleNearby;
    private ListView peopleNearby;

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        noPeopleNearby = findViewById(R.id.no_people_nearby);
        peopleNearby = (ListView)findViewById(R.id.list_people_nearby);
        peopleNearby.setVisibility(View.GONE);

        if (bluetooth != null) {

            viewBluetoothId = (TextView)findViewById(R.id.device_id_bluetooth);
            viewBluetoothId.setText(bluetooth.getName());

            Switch bluetoothSwitch = ((Switch) findViewById(R.id.switch_bluetooth));
            bluetoothSwitch.setChecked(getManager().isBluetoothDiscoverable());
            bluetoothSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        getManager().ensureBluetoothDiscoverable();
                    } else {
                        // disableBluetooth();
                    }
                }
            });
        } else {
            findViewById(R.id.bluetooth_info).setVisibility(View.GONE);
        }

        ((Switch)findViewById(R.id.switch_wifi)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    enableWifi();
                } else {
                    disableWifi();
                }
            }
        });

        final PeopleNearbyAdapter adapter = new PeopleNearbyAdapter(getContext());

        peopleNearbyList = (ListView)findViewById(R.id.people_nearby);
        peopleNearbyList.setAdapter(adapter);

        SwapManager.load(getActivity()).setPeerListener(new PeerFinder.Listener<Peer>() {
            @Override
            public void onPeerFound(Peer peer) {
                adapter.notifyDataSetChanged();
            }
        });


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


    // ========================================================================
    //                            Wifi stuff
    // ========================================================================

    private void enableWifi() {

    }

    private void disableWifi() {

    }
}
