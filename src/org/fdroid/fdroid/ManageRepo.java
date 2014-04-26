/*
 * Copyright (C) 2010-12  Ciaran Gultnieks, ciaran@ciarang.com
 * Copyright (C) 2009  Roberto Jacinto, roberto.jacinto@caixamagica.pt
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.fdroid.fdroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

import android.widget.LinearLayout;
import android.widget.Toast;

import org.fdroid.fdroid.compat.ActionBarCompat;
import org.fdroid.fdroid.views.fragments.RepoListFragment;

import java.util.Locale;
public class ManageRepo extends FragmentActivity {

    /**
     * If we have a new repo added, or the address of a repo has changed, then
     * we when we're finished, we'll set this boolean to true in the intent
     * that we finish with, to signify that we want the main list of apps
     * updated.
     */
    public static final String REQUEST_UPDATE = "update";

    private RepoListFragment listFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((FDroidApp) getApplication()).applyTheme(this);

        if (savedInstanceState == null) {

            // Need to set a dummy view (which will get overridden by the fragment manager
            // below) so that we can call setContentView(). This is a work around for
            // a (bug?) thing in 3.0, 3.1 which requires setContentView to be invoked before
            // the actionbar is played with:
            // http://blog.perpetumdesign.com/2011/08/strange-case-of-dr-action-and-mr-bar.html
            setContentView( new LinearLayout(this) );

            listFragment = new RepoListFragment();
            getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, listFragment)
                .commit();
        }

        ActionBarCompat.create(this).setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        /* let's see if someone is trying to send us a new repo */
        addRepoFromIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        addRepoFromIntent(intent);
    }

    @Override
    public void finish() {
        Intent ret = new Intent();
        markChangedIfRequired(ret);
        setResult(Activity.RESULT_OK, ret);
        super.finish();
    }

    private boolean hasChanged() {
        return listFragment != null && listFragment.hasChanged();
    }

    private void markChangedIfRequired(Intent intent) {
        if (hasChanged()) {
            Log.i("FDroid", "Repo details have changed, prompting for update.");
            intent.putExtra(REQUEST_UPDATE, true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent destIntent = new Intent(this, FDroid.class);
                markChangedIfRequired(destIntent);
                setResult(RESULT_OK, destIntent);
                NavUtils.navigateUpTo(this, destIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addRepoFromIntent(Intent intent) {
        /* an URL from a click, NFC, QRCode scan, etc */
        Uri uri = intent.getData();
        if (uri != null) {
            // scheme and host should only ever be pure ASCII aka Locale.ENGLISH
            String scheme = intent.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                String msg = String.format(getString(R.string.malformed_repo_uri), uri);
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                return;
            }
            if (scheme.equals("FDROIDREPO") || scheme.equals("FDROIDREPOS")) {
                /*
                 * QRCodes are more efficient in all upper case, so QR URIs are
                 * encoded in all upper case, then forced to lower case.
                 * Checking if the special F-Droid scheme being all is upper
                 * case means it should be downcased.
                 */
                uri = Uri.parse(uri.toString().toLowerCase(Locale.ENGLISH));
            } else if (uri.getPath().startsWith("/FDROID/REPO")) {
                /*
                 * some QR scanners chop off the fdroidrepo:// and just try
                 * http://, then the incoming URI does not get downcased
                 * properly, and the query string is stripped off. So just
                 * downcase the path, and carry on to get something working.
                 */
                uri = Uri.parse(uri.toString().toLowerCase(Locale.ENGLISH));
            }
            // make scheme and host lowercase so they're readable in dialogs
            scheme = scheme.toLowerCase(Locale.ENGLISH);
            host = host.toLowerCase(Locale.ENGLISH);
            String fingerprint = uri.getQueryParameter("fingerprint");
            Log.i("RepoListFragment", "onCreate " + fingerprint);
            if (scheme.equals("fdroidrepos") || scheme.equals("fdroidrepo")
                    || scheme.equals("https") || scheme.equals("http")) {

                /* sanitize and format for function and readability */
                String uriString = uri.toString()
                        .replaceAll("\\?.*$", "") // remove the whole query
                        .replaceAll("/*$", "") // remove all trailing slashes
                        .replace(uri.getHost(), host) // downcase host name
                        .replace(intent.getScheme(), scheme) // downcase scheme
                        .replace("fdroidrepo", "http"); // proper repo address
                listFragment.importRepo(uriString, fingerprint);

                // if this is a local repo, check we're on the same wifi
                String uriBssid = uri.getQueryParameter("bssid");
                if (!TextUtils.isEmpty(uriBssid)) {
                    if (uri.getPort() != 8888
                            && !host.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+")) {
                        Log.i("ManageRepo", "URI is not local repo: " + uri);
                        return;
                    }
                    WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    String bssid = wifiInfo.getBSSID().toLowerCase(Locale.ENGLISH);
                    uriBssid = Uri.decode(uriBssid).toLowerCase(Locale.ENGLISH);
                    if (!bssid.equals(uriBssid)) {
                        String msg = String.format(getString(R.string.not_on_same_wifi),
                                uri.getQueryParameter("ssid"));
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                    // TODO we should help the user to the right thing here,
                    // instead of just showing a message!
                }
            }
        }
    }
}
