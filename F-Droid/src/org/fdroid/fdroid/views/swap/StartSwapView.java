package org.fdroid.fdroid.views.swap;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.compat.SwitchCompat;
import org.fdroid.fdroid.localrepo.SwapManager;

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
