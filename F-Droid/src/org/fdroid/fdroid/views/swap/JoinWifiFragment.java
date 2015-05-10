package org.fdroid.fdroid.views.swap;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.net.WifiStateChangeService;

public class JoinWifiFragment extends Fragment {

    private final BroadcastReceiver onWifiChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshWifiState();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.swap_next, menu);
        MenuItem nextMenuItem = menu.findItem(R.id.action_next);
        int flags = MenuItemCompat.SHOW_AS_ACTION_ALWAYS | MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT;
        MenuItemCompat.setShowAsAction(nextMenuItem, flags);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View joinWifiView = inflater.inflate(R.layout.swap_join_wifi, container, false);
        joinWifiView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAvailableNetworks();
            }
        });
        return joinWifiView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // TODO: Listen for "Connecting..." state and reflect that in the view too.
        LocalBroadcastManager.getInstance(activity).registerReceiver(
                onWifiChange,
                new IntentFilter(WifiStateChangeService.BROADCAST));
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshWifiState();
    }

    private void refreshWifiState() {
        View view = getView();
        if (view != null) {
            TextView descriptionView = (TextView) view.findViewById(R.id.text_description);
            ImageView wifiIcon = (ImageView) view.findViewById(R.id.wifi_icon);
            TextView ssidView = (TextView) view.findViewById(R.id.wifi_ssid);
            TextView tapView = (TextView) view.findViewById(R.id.wifi_available_networks_prompt);
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
    }

    private void openAvailableNetworks() {
        startActivity(new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK));
    }
}
