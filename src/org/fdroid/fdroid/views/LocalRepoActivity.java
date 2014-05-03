
package org.fdroid.fdroid.views;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.PreferencesActivity;
import org.fdroid.fdroid.QrGenAsyncTask;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.net.WifiStateChangeService;

import java.util.Locale;

public class LocalRepoActivity extends Activity {
    private static final String TAG = "LocalRepoActivity";
    private ProgressDialog repoProgress;

    private WifiManager wifiManager;
    private ToggleButton repoSwitch;

    private int SET_IP_ADDRESS = 7345;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.local_repo_activity);

        repoSwitch = (ToggleButton) findViewById(R.id.repoSwitch);
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
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

    private BroadcastReceiver onWifiChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
            resetNetworkInfo();
        }
    };

    private void resetNetworkInfo() {
        int wifiState = wifiManager.getWifiState();
        if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
            setUIFromWifi();
            wireRepoSwitchToWebServer();
        } else {
            repoSwitch.setChecked(false);
            repoSwitch.setText(R.string.enable_wifi);
            repoSwitch.setTextOn(getString(R.string.enabling_wifi));
            repoSwitch.setTextOff(getString(R.string.enable_wifi));
            repoSwitch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    wifiManager.setWifiEnabled(true);
                    /*
                     * Once the wifi is connected to a network, then
                     * WifiStateChangeReceiver will receive notice, and kick off
                     * the process of getting the info about the wifi
                     * connection.
                     */
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.local_repo_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                startActivityForResult(new Intent(this, PreferencesActivity.class), SET_IP_ADDRESS);
                return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK)
            return;
        if (requestCode == SET_IP_ADDRESS) {
            setUIFromWifi();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case 0:
                repoProgress = new ProgressDialog(this);
                repoProgress.setMessage("Scanning Apps. Please wait...");
                repoProgress.setIndeterminate(false);
                repoProgress.setMax(100);
                repoProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                repoProgress.setCancelable(false);
                repoProgress.show();
                return repoProgress;
            default:
                return null;
        }
    }

    private void wireRepoSwitchToWebServer() {
        repoSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (repoSwitch.isChecked()) {
                    FDroidApp.startLocalRepoService(LocalRepoActivity.this);
                } else {
                    FDroidApp.stopLocalRepoService(LocalRepoActivity.this);
                }
            }
        });
    }

    @TargetApi(14)
    private void setUIFromWifi() {
        if (TextUtils.isEmpty(FDroidApp.repo.address))
            return;
        // the fingerprint is not useful on the button label
        String buttonLabel = FDroidApp.repo.address.replaceAll("\\?.*$", "");
        repoSwitch.setText(buttonLabel);
        repoSwitch.setTextOn(buttonLabel);
        repoSwitch.setTextOff(buttonLabel);
        /*
         * Set URL to UPPER for compact QR Code, FDroid will translate it back.
         * Remove the SSID from the query string since SSIDs are case-sensitive.
         * Instead the receiver will have to rely on the BSSID to find the right
         * wifi AP to join. Lots of QR Scanners are buggy and do not respect
         * custom URI schemes, so we have to use http:// or https:// :-(
         */
        final String qrUriString = Utils.getSharingUri(this, FDroidApp.repo).toString()
                .replaceFirst("fdroidrepo", "http")
                .replaceAll("ssid=[^?]*", "")
                .toUpperCase(Locale.ENGLISH);
        Log.i("QRURI", qrUriString);
        new QrGenAsyncTask(this, R.id.repoQrCode).execute(qrUriString);

        TextView wifiNetworkNameTextView = (TextView) findViewById(R.id.wifiNetworkName);
        wifiNetworkNameTextView.setText(FDroidApp.ssid);

        TextView fingerprintTextView = (TextView) findViewById(R.id.fingerprint);
        if (FDroidApp.repo.fingerprint != null) {
            fingerprintTextView.setVisibility(View.VISIBLE);
            fingerprintTextView.setText(FDroidApp.repo.fingerprint);
        } else {
            fingerprintTextView.setVisibility(View.GONE);
        }

        // the required NFC API was added in 4.0 aka Ice Cream Sandwich
        if (Build.VERSION.SDK_INT >= 14) {
            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
            if (nfcAdapter == null)
                return;
            nfcAdapter.setNdefPushMessage(new NdefMessage(new NdefRecord[] {
                    NdefRecord.createUri(Utils.getSharingUri(this, FDroidApp.repo)),
            }), this);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // ignore orientation/keyboard change
        super.onConfigurationChanged(newConfig);
    }
}
