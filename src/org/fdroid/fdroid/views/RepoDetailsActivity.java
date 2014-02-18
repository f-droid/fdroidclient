
package org.fdroid.fdroid.views;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.compat.ActionBarCompat;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.views.fragments.RepoDetailsFragment;

public class RepoDetailsActivity extends FragmentActivity {
    public static final String TAG = "RepoDetailsActivity";

    private WifiManager wifiManager;
    private Repo repo;

    static final String MIME_TYPE = "application/vnd.org.fdroid.fdroid.repo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ((FDroidApp) getApplication()).applyTheme(this);

        super.onCreate(savedInstanceState);

        long repoId = getIntent().getLongExtra(RepoDetailsFragment.ARG_REPO_ID, 0);

        if (savedInstanceState == null) {
            RepoDetailsFragment fragment = new RepoDetailsFragment();
            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, fragment)
                    .commit();
        }

        String[] projection = new String[] {
                RepoProvider.DataColumns.NAME,
                RepoProvider.DataColumns.ADDRESS,
                RepoProvider.DataColumns.FINGERPRINT
        };
        repo = RepoProvider.Helper.findById(this, repoId, projection);

        ActionBarCompat.create(this).setDisplayHomeAsUpEnabled(true);
        setTitle(repo.getName());

        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        // required NFC support starts in android-14
        if (Build.VERSION.SDK_INT >= 14)
            setNfc();
    }

    @TargetApi(14)
    private void setNfc() {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            return;
        }
        nfcAdapter.setNdefPushMessage(new NdefMessage(new NdefRecord[] {
                NdefRecord.createUri(getSharingUri()),
        }), this);
        findViewById(android.R.id.content).post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Runnable.run()");
                onNewIntent(getIntent());
            }
        });
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        if (Build.VERSION.SDK_INT >= 9)
            processIntent(getIntent());
    }

    @Override
    public void onNewIntent(Intent i) {
        Log.i(TAG, "onNewIntent");
        Log.i(TAG, "action: " + i.getAction());
        Log.i(TAG, "data: " + i.getData());
        // onResume gets called after this to handle the intent
        setIntent(i);
    }

    @TargetApi(9)
    void processIntent(Intent i) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(i.getAction())) {
            Log.i(TAG, "ACTION_NDEF_DISCOVERED");
            Parcelable[] rawMsgs =
                    i.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage msg = (NdefMessage) rawMsgs[0];
            String url = new String(msg.getRecords()[0].getPayload());
            Log.i(TAG, "Got this URL: " + url);
            Toast.makeText(this, "Got this URL: " + url, Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            String packageName = getPackageName();
            intent.setClassName(packageName, packageName + ".ManageRepo");
            startActivity(intent);
            finish();
        }
    }

    protected Uri getSharingUri() {
        Uri uri = Uri.parse(repo.address.replaceFirst("http", "fdroidrepo"));
        Uri.Builder b = uri.buildUpon();
        b.appendQueryParameter("fingerprint", repo.fingerprint);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID().replaceAll("^\"(.*)\"$", "$1");
        String bssid = wifiInfo.getBSSID();
        if (!TextUtils.isEmpty(bssid)) {
            b.appendQueryParameter("bssid", Uri.encode(bssid));
            if (!TextUtils.isEmpty(ssid))
                b.appendQueryParameter("ssid", Uri.encode(ssid));
        }
        return b.build();
    }
}
