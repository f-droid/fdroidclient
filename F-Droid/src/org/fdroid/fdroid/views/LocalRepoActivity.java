package org.fdroid.fdroid.views;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.PreferencesActivity;
import org.fdroid.fdroid.QrGenAsyncTask;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.localrepo.LocalRepoManager;
import org.fdroid.fdroid.localrepo.LocalRepoService;
import org.fdroid.fdroid.net.WifiStateChangeService;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class LocalRepoActivity extends ActionBarActivity {

    private static final String TAG = "fdroid.LocalRepoActivity";
    private ProgressDialog repoProgress;

    private WifiManager wifiManager;
    private Button enableWifiButton;
    private CheckBox repoSwitch;

    private Timer stopTimer;

    private final int SET_IP_ADDRESS = 7345;
    private final int UPDATE_REPO = 7346;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        ((FDroidApp) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.local_repo_activity);

        enableWifiButton = (Button) findViewById(R.id.enable_wifi);
        repoSwitch = (CheckBox) findViewById(R.id.repoSwitch);
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
    }

    @Override
    public void onResume() {
        super.onResume();
        resetNetworkInfo();
        setRepoSwitchChecked(FDroidApp.isLocalRepoServiceRunning());

        LocalBroadcastManager.getInstance(this).registerReceiver(onWifiChange,
                new IntentFilter(WifiStateChangeService.BROADCAST));
        LocalBroadcastManager.getInstance(this).registerReceiver(onLocalRepoChange,
                new IntentFilter(LocalRepoService.STATE));
        // if no local repo exists, create one with only FDroid in it
        if (!LocalRepoManager.get(this).xmlIndex.exists())
            new UpdateAsyncTask(this, new String[] {
                    getPackageName(),
            }).execute();

        // start repo by default
        FDroidApp.startLocalRepoService(LocalRepoActivity.this);
        // reset the timer if viewing this Activity again
        if (stopTimer != null)
            stopTimer.cancel();
        // automatically turn off after 15 minutes
        stopTimer = new Timer();
        stopTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                FDroidApp.stopLocalRepoService(LocalRepoActivity.this);
            }
        }, 900000); // 15 minutes
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onWifiChange);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onLocalRepoChange);
    }

    private final BroadcastReceiver onWifiChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
            resetNetworkInfo();
        }
    };

    private final BroadcastReceiver onLocalRepoChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
            String state = i.getStringExtra(LocalRepoService.STATE);
            if (state != null && state.equals(LocalRepoService.STARTED))
                setRepoSwitchChecked(true);
            else
                setRepoSwitchChecked(false);
        }
    };

    private void resetNetworkInfo() {
        int wifiState = wifiManager.getWifiState();
        if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
            setUIFromWifi();
            wireRepoSwitchToWebServer();
            repoSwitch.setVisibility(View.VISIBLE);
            enableWifiButton.setVisibility(View.GONE);
        } else {
            repoSwitch.setChecked(false);
            repoSwitch.setVisibility(View.GONE);
            enableWifiButton.setVisibility(View.VISIBLE);
            enableWifiButton.setText(R.string.enable_wifi);
            enableWifiButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    enableWifiButton.setText(R.string.enabling_wifi);
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
        if (resultCode != RESULT_OK)
            return;
        if (requestCode == SET_IP_ADDRESS) {
            setUIFromWifi();
        } else if (requestCode == UPDATE_REPO) {
            setUIFromWifi();
            new UpdateAsyncTask(this, FDroidApp.selectedApps.toArray(new String[FDroidApp.selectedApps.size()]))
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
                setRepoSwitchChecked(repoSwitch.isChecked());
                if (repoSwitch.isChecked()) {
                    FDroidApp.startLocalRepoService(LocalRepoActivity.this);
                } else {
                    FDroidApp.stopLocalRepoService(LocalRepoActivity.this);
                    stopTimer.cancel(); // disable automatic stop
                }
            }
        });
    }

    private void setRepoSwitchChecked(boolean checked) {
        repoSwitch.setChecked(checked);
        if (checked) {
            repoSwitch.setText(R.string.local_repo_running);
        } else {
            repoSwitch.setText(R.string.touch_to_turn_on_local_repo);
        }
    }

    @TargetApi(14)
    private void setUIFromWifi() {
        if (TextUtils.isEmpty(FDroidApp.repo.address))
            return;
        // the fingerprint is not useful on the button label
        String buttonLabel = FDroidApp.repo.address.replaceAll("\\?.*$", "");
        TextView sharingUriTextView = (TextView) findViewById(R.id.sharing_uri);
        sharingUriTextView.setText(buttonLabel);
        /*
         * Set URL to UPPER for compact QR Code, FDroid will translate it back.
         * Remove the SSID from the query string since SSIDs are case-sensitive.
         * Instead the receiver will have to rely on the BSSID to find the right
         * wifi AP to join. Lots of QR Scanners are buggy and do not respect
         * custom URI schemes, so we have to use http:// or https:// :-(
         */
        final String qrUriString = Utils.getSharingUri(FDroidApp.repo).toString()
                .replaceFirst("fdroidrepo", "http")
                .replaceAll("ssid=[^?]*", "")
                .toUpperCase(Locale.ENGLISH);
        if (Build.VERSION.SDK_INT >= 8) // zxing requires >= 8
            new QrGenAsyncTask(this, R.id.repoQrCode).execute(qrUriString);

        TextView wifiNetworkNameTextView = (TextView) findViewById(R.id.wifi_network);
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
                    NdefRecord.createUri(Utils.getSharingUri(FDroidApp.repo)),
            }), this);
        }
    }

    class UpdateAsyncTask extends AsyncTask<Void, String, Void> {
        private static final String TAG = "fdroid.LocalRepoActivity.UpdateAsyncTask";
        private final ProgressDialog progressDialog;
        private final String[] selectedApps;
        private final Uri sharingUri;

        public UpdateAsyncTask(Context c, String[] apps) {
            selectedApps = apps;
            progressDialog = new ProgressDialog(c);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setTitle(R.string.updating);
            sharingUri = Utils.getSharingUri(FDroidApp.repo);
        }

        @Override
        protected void onPreExecute() {
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                final LocalRepoManager lrm = LocalRepoManager.get(LocalRepoActivity.this);
                publishProgress(getString(R.string.deleting_repo));
                lrm.deleteRepo();
                for (String app : selectedApps) {
                    publishProgress(String.format(getString(R.string.adding_apks_format), app));
                    lrm.addApp(getApplicationContext(), app);
                }
                lrm.writeIndexPage(sharingUri.toString());
                publishProgress(getString(R.string.writing_index_jar));
                lrm.writeIndexJar();
                publishProgress(getString(R.string.linking_apks));
                lrm.copyApksToRepo();
                publishProgress(getString(R.string.copying_icons));
                // run the icon copy without progress, its not a blocker
                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... params) {
                        lrm.copyIconsToRepo();
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
