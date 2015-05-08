package org.fdroid.fdroid.views;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.QrGenAsyncTask;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.net.WifiStateChangeService;

public class QrWizardWifiNetworkActivity extends ActionBarActivity {
    private static final String TAG = "QrWizardWifiNetworkActivity";

    private WifiManager wifiManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ((FDroidApp) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);

        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);
        FDroidApp.startLocalRepoService(this);

        setContentView(R.layout.qr_wizard_activity);
        TextView instructions = (TextView) findViewById(R.id.qrWizardInstructions);
        instructions.setText(R.string.qr_wizard_wifi_network_instructions);
        Button next = (Button) findViewById(R.id.qrNextButton);
        next.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), QrWizardDownloadActivity.class);
                startActivityForResult(intent, 0);
                finish();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        resetNetworkInfo();
        LocalBroadcastManager.getInstance(this).registerReceiver(onWifiChange,
                new IntentFilter(WifiStateChangeService.BROADCAST));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onWifiChange);
    }

    private final BroadcastReceiver onWifiChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
            Log.i(TAG, "onWifiChange.onReceive()");
            resetNetworkInfo();
        }
    };

    private void resetNetworkInfo() {
        int wifiState = wifiManager.getWifiState();
        if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            // http://zxing.appspot.com/generator/
            // WIFI:S:openwireless.org;; // no pw
            // WIFI:S:openwireless.org;T:WPA;;
            // WIFI:S:openwireless.org;T:WEP;;
            // WIFI:S:openwireless.org;H:true;; // hidden
            // WIFI:S:openwireless.org;T:WPA;H:true;; // all
            String qrString = "WIFI:S:";
            qrString += wifiInfo.getSSID();
            // TODO get encryption state (none, WEP, WPA)
            /*
             * WifiConfiguration wc = null; for (WifiConfiguration i :
             * wifiManager.getConfiguredNetworks()) { if (i.status ==
             * WifiConfiguration.Status.CURRENT) { wc = i; break; } } if (wc !=
             * null)
             */
            if (wifiInfo.getHiddenSSID())
                qrString += ";H:true";
            qrString += ";;";
            if (Build.VERSION.SDK_INT >= 8) // zxing requires >= 8
                new QrGenAsyncTask(this, R.id.qrWizardImage).execute(qrString);

            TextView wifiNetworkName = (TextView) findViewById(R.id.qrWifiNetworkName);
            wifiNetworkName.setText(wifiInfo.getSSID());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // this wizard is done, clear this Activity from the history
        if (resultCode == RESULT_OK)
            finish();
    }
}
