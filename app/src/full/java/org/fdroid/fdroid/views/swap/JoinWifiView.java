package org.fdroid.fdroid.views.swap;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.localrepo.SwapView;
import org.fdroid.fdroid.net.WifiStateChangeService;

public class JoinWifiView extends SwapView {

    public JoinWifiView(Context context) {
        super(context);
    }

    public JoinWifiView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public JoinWifiView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public JoinWifiView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
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

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                onWifiStateChange,
                new IntentFilter(WifiStateChangeService.BROADCAST)
        );
    }

    /**
     * Remove relevant listeners/receivers/etc so that they do not receive and process events
     * when this view is not in use.
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(onWifiStateChange);
    }

    // TODO: Listen for "Connecting..." state and reflect that in the view too.
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

    private final BroadcastReceiver onWifiStateChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshWifiState();
        }
    };
}
