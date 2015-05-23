package org.fdroid.fdroid.views.swap.views;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.localrepo.SwapState;
import org.fdroid.fdroid.net.WifiStateChangeService;
import org.fdroid.fdroid.views.swap.SwapWorkflowActivity;

public class JoinWifiView extends RelativeLayout implements SwapWorkflowActivity.InnerView {

    public JoinWifiView(Context context) {
        super(context);
    }

    public JoinWifiView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public JoinWifiView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public JoinWifiView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private SwapWorkflowActivity getActivity() {
        // TODO: Try and find a better way to get to the SwapActivity, which makes less asumptions.
        return (SwapWorkflowActivity)getContext();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAvailableNetworks();
            }
        });
        refreshWifiState();

        // TODO: Listen for "Connecting..." state and reflect that in the view too.
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        refreshWifiState();
                    }
                },
                new IntentFilter(WifiStateChangeService.BROADCAST)
        );

    }

    private void refreshWifiState() {
        TextView descriptionView = (TextView) findViewById(R.id.text_description);
        ImageView wifiIcon = (ImageView) findViewById(R.id.wifi_icon);
        TextView ssidView = (TextView) findViewById(R.id.wifi_ssid);
        TextView tapView = (TextView) findViewById(R.id.wifi_available_networks_prompt);
        if (TextUtils.isEmpty(FDroidApp.bssid) && !TextUtils.isEmpty(FDroidApp.ipAddressString)) {
            // empty bssid with an ipAddress means hotspot mode
            descriptionView.setText(R.string.swap_join_this_hotspot);
            wifiIcon.setImageDrawable(getResources().getDrawable(R.drawable.hotspot));
            ssidView.setText(R.string.swap_active_hotspot);
            tapView.setText(R.string.swap_switch_to_wifi);
        } else if (TextUtils.isEmpty(FDroidApp.ssid)) {
            // not connected to or setup with any wifi network
            descriptionView.setText(R.string.swap_join_same_wifi);
            wifiIcon.setImageDrawable(getResources().getDrawable(R.drawable.wifi));
            ssidView.setText(R.string.swap_no_wifi_network);
            tapView.setText(R.string.swap_view_available_networks);
        } else {
            // connected to a regular wifi network
            descriptionView.setText(R.string.swap_join_same_wifi);
            wifiIcon.setImageDrawable(getResources().getDrawable(R.drawable.wifi));
            ssidView.setText(FDroidApp.ssid);
            tapView.setText(R.string.swap_view_available_networks);
        }
    }

    private void openAvailableNetworks() {
        getActivity().startActivity(new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK));
    }

    @Override
    public boolean buildMenu(Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.swap_next, menu);
        MenuItem next = menu.findItem(R.id.action_next);
        MenuItemCompat.setShowAsAction(next, MenuItemCompat.SHOW_AS_ACTION_ALWAYS | MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
        next.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                getActivity().onJoinWifiComplete();
                return true;
            }
        });
        return true;
    }

    @Override
    public int getStep() {
        return SwapState.STEP_JOIN_WIFI;
    }
}
