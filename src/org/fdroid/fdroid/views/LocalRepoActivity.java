
package org.fdroid.fdroid.views;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.*;
import android.content.res.Configuration;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.fdroid.fdroid.*;
import org.fdroid.fdroid.net.WifiStateChangeService;

import java.util.Locale;

public class LocalRepoActivity extends Activity {
    private static final String TAG = "LocalRepoActivity";
    private ProgressDialog repoProgress;

    private WifiManager wifiManager;
    private ToggleButton repoSwitch;

    private int SET_IP_ADDRESS = 7345;
    private int UPDATE_REPO = 7346;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((FDroidApp) getApplication()).applyTheme(this);
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
        // if no local repo exists, create one with only FDroid in it
        if (!FDroidApp.localRepo.xmlIndex.exists())
            new UpdateAsyncTask(this, new String[] {
                    getPackageName(),
            }).execute();
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
        if (Build.VERSION.SDK_INT < 11) // TODO remove after including appcompat-v7
            menu.findItem(R.id.menu_setup_repo).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_setup_repo:
                startActivityForResult(new Intent(this, SelectLocalAppsActivity.class), UPDATE_REPO);
                return true;
            case R.id.menu_send_fdroid_via_wifi:
                startActivity(new Intent(this, QrWizardWifiNetworkActivity.class));
                return true;
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
        } else if (requestCode == UPDATE_REPO) {
            setUIFromWifi();
            new UpdateAsyncTask(this, FDroidApp.selectedApps.toArray(new String[0]))
                    .execute();
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
        if (Build.VERSION.SDK_INT >= 8) // zxing requires >= 8
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

    class UpdateAsyncTask extends AsyncTask<Void, String, Void> {
        private static final String TAG = "UpdateAsyncTask";
        private ProgressDialog progressDialog;
        private String[] selectedApps;
        private Uri sharingUri;

        public UpdateAsyncTask(Context c, String[] apps) {
            selectedApps = apps;
            progressDialog = new ProgressDialog(c);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setTitle(R.string.updating);
            sharingUri = Utils.getSharingUri(c, FDroidApp.repo);
        }

        @Override
        protected void onPreExecute() {
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                publishProgress(getString(R.string.deleting_repo));
                FDroidApp.localRepo.deleteRepo();
                for (String app : selectedApps) {
                    publishProgress(String.format(getString(R.string.adding_apks_format), app));
                    FDroidApp.localRepo.addApp(getApplicationContext(), app);
                }
                FDroidApp.localRepo.writeIndexPage(sharingUri.toString());
                publishProgress(getString(R.string.writing_index_xml));
                FDroidApp.localRepo.writeIndexXML();
                publishProgress(getString(R.string.linking_apks));
                FDroidApp.localRepo.copyApksToRepo();
                publishProgress(getString(R.string.copying_icons));
                // run the icon copy without progress, its not a blocker
                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... params) {
                        FDroidApp.localRepo.copyIconsToRepo();
                        return null;
                    }
                }.execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            super.onProgressUpdate(progress);
            progressDialog.setMessage(progress[0]);
        }

        @Override
        protected void onPostExecute(Void result) {
            progressDialog.dismiss();
            Toast.makeText(getBaseContext(), R.string.updated_local_repo, Toast.LENGTH_SHORT)
                    .show();
        }
    }
}
