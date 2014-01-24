
package org.fdroid.fdroid.views;

import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;

import org.fdroid.fdroid.compat.ActionBarCompat;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.views.fragments.RepoDetailsFragment;

public class RepoDetailsActivity extends FragmentActivity {

    private WifiManager wifiManager;
    private Repo repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        repo = RepoProvider.Helper.findById(getContentResolver(), repoId, projection);

        ActionBarCompat.create(this).setDisplayHomeAsUpEnabled(true);
        setTitle(repo.getName());

        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
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
