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

import android.app.NotificationManager;
import android.app.SearchManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.fdroid.fdroid.compat.TabManager;
import org.fdroid.fdroid.compat.UriCompat;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.NewRepoConfig;
import org.fdroid.fdroid.privileged.install.InstallExtensionDialogActivity;
import org.fdroid.fdroid.views.AppListFragmentPagerAdapter;
import org.fdroid.fdroid.views.ManageReposActivity;
import org.fdroid.fdroid.views.swap.SwapWorkflowActivity;

public class FDroid extends AppCompatActivity implements SearchView.OnQueryTextListener {

    private static final String TAG = "FDroid";

    private static final int REQUEST_PREFS = 1;
    private static final int REQUEST_ENABLE_BLUETOOTH = 2;
    private static final int REQUEST_SWAP = 3;

    public static final String EXTRA_TAB_UPDATE = "extraTab";

    public static final String ACTION_ADD_REPO = "org.fdroid.fdroid.FDroid.ACTION_ADD_REPO";

    private FDroidApp fdroidApp;

    private ViewPager viewPager;

    @Nullable
    private TabManager tabManager;

    private AppListFragmentPagerAdapter adapter;

    @Nullable
    private MenuItem searchMenuItem;

    @Nullable
    private String pendingSearchQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        fdroidApp = (FDroidApp) getApplication();
        fdroidApp.applyTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.fdroid);
        createViews();

        getTabManager().createTabs();

        // Start a search by just typing
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        Intent intent = getIntent();
        handleSearchOrAppViewIntent(intent);

        if (intent.hasExtra(EXTRA_TAB_UPDATE)) {
            boolean showUpdateTab = intent.getBooleanExtra(EXTRA_TAB_UPDATE, false);
            if (showUpdateTab) {
                getTabManager().selectTab(2);
            }
        }

        Uri uri = AppProvider.getContentUri();
        getContentResolver().registerContentObserver(uri, true, new AppObserver());

        InstallExtensionDialogActivity.firstTime(this);

        // Re-enable once it can be disabled via a setting
        // See https://gitlab.com/fdroid/fdroidclient/issues/435
        //
        // if (UpdateService.isNetworkAvailableForUpdate(this)) {
        //     UpdateService.updateNow(this);
        // }
    }

    private void performSearch(String query) {
        if (searchMenuItem == null) {
            // Store this for later when we do actually have a search menu ready to use.
            pendingSearchQuery = query;
            return;
        }

        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
        MenuItemCompat.expandActionView(searchMenuItem);
        searchView.setQuery(query, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // AppDetails and RepoDetailsActivity set different NFC actions, so reset here
        NfcHelper.setAndroidBeam(this, getApplication().getPackageName());
        checkForAddRepoIntent();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleSearchOrAppViewIntent(intent);
    }

    private void handleSearchOrAppViewIntent(Intent intent) {
        final Uri data = intent.getData();
        if (data == null) {
            return;
        }

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            performSearch(query);
            return;
        }

        final String scheme = data.getScheme();
        final String path = data.getPath();
        String appId = null;
        String query = null;
        if (data.isHierarchical()) {
            final String host = data.getHost();
            if (host == null) {
                return;
            }
            switch (host) {
                case "f-droid.org":
                    if (path.startsWith("/repository/browse")) {
                        // http://f-droid.org/repository/browse?fdfilter=search+query
                        query = UriCompat.getQueryParameter(data, "fdfilter");

                        // http://f-droid.org/repository/browse?fdid=app.id
                        appId = UriCompat.getQueryParameter(data, "fdid");
                    } else if (path.startsWith("/app")) {
                        // http://f-droid.org/app/app.id
                        appId = data.getLastPathSegment();
                        if ("app".equals(appId)) {
                            appId = null;
                        }
                    }
                    break;
                case "details":
                    // market://details?id=app.id
                    appId = UriCompat.getQueryParameter(data, "id");
                    break;
                case "search":
                    // market://search?q=query
                    query = UriCompat.getQueryParameter(data, "q");
                    break;
                case "play.google.com":
                    if (path.startsWith("/store/apps/details")) {
                        // http://play.google.com/store/apps/details?id=app.id
                        appId = UriCompat.getQueryParameter(data, "id");
                    } else if (path.startsWith("/store/search")) {
                        // http://play.google.com/store/search?q=foo
                        query = UriCompat.getQueryParameter(data, "q");
                    }
                    break;
                case "apps":
                case "amazon.com":
                case "www.amazon.com":
                    // amzn://apps/android?p=app.id
                    // http://amazon.com/gp/mas/dl/android?s=app.id
                    appId = UriCompat.getQueryParameter(data, "p");
                    query = UriCompat.getQueryParameter(data, "s");
                    break;
            }
        } else if ("fdroid.app".equals(scheme)) {
            // fdroid.app:app.id
            appId = data.getSchemeSpecificPart();
        } else if ("fdroid.search".equals(scheme)) {
            // fdroid.search:query
            query = UriCompat.replacePlusWithSpace(data.getSchemeSpecificPart());
        }

        if (!TextUtils.isEmpty(query)) {
            // an old format for querying via packageName
            if (query.startsWith("pname:"))
                appId = query.split(":")[1];

            // sometimes, search URLs include pub: or other things before the query string
            if (query.contains(":"))
                query = query.split(":")[1];
        }

        Intent call = null;
        if (!TextUtils.isEmpty(appId)) {
            Utils.debugLog(TAG, "FDroid launched via app link for '" + appId + "'");
            call = new Intent(this, AppDetails.class);
            call.putExtra(AppDetails.EXTRA_APPID, appId);
        } else if (!TextUtils.isEmpty(query)) {
            Utils.debugLog(TAG, "FDroid launched via search link for '" + query + "'");
            performSearch(query);
        }

        if (call != null) {
            startActivity(call);
        }
    }

    private void checkForAddRepoIntent() {
        // Don't handle the intent after coming back to this view (e.g. after hitting the back button)
        // http://stackoverflow.com/a/14820849
        Intent intent = getIntent();
        if (!intent.hasExtra("handled")) {
            NewRepoConfig parser = new NewRepoConfig(this, intent);
            if (parser.isValidRepo()) {
                intent.putExtra("handled", true);
                if (parser.isFromSwap()) {
                    Intent confirmIntent = new Intent(this, SwapWorkflowActivity.class);
                    confirmIntent.putExtra(SwapWorkflowActivity.EXTRA_CONFIRM, true);
                    confirmIntent.setData(intent.getData());
                    startActivityForResult(confirmIntent, REQUEST_SWAP);
                } else {
                    startActivity(new Intent(ACTION_ADD_REPO, intent.getData(), this, ManageReposActivity.class));
                }
            } else if (parser.getErrorMessage() != null) {
                Toast.makeText(this, parser.getErrorMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getTabManager().onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (fdroidApp.bluetoothAdapter == null) {
            // ignore on devices without Bluetooth
            MenuItem btItem = menu.findItem(R.id.action_bluetooth_apk);
            btItem.setVisible(false);
        }

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchMenuItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        // LayoutParams.MATCH_PARENT does not work, use a big value instead
        searchView.setMaxWidth(1000000);
        searchView.setOnQueryTextListener(this);

        if (pendingSearchQuery != null) {
            performSearch(pendingSearchQuery);
            pendingSearchQuery = null;
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.action_update_repo:
                UpdateService.updateNow(this);
                return true;

            case R.id.action_manage_repos:
                startActivity(new Intent(this, ManageReposActivity.class));
                return true;

            case R.id.action_settings:
                Intent prefs = new Intent(getBaseContext(), PreferencesActivity.class);
                startActivityForResult(prefs, REQUEST_PREFS);
                return true;

            case R.id.action_swap:
                startActivity(new Intent(this, SwapWorkflowActivity.class));
                return true;

            case R.id.action_bluetooth_apk:
                /*
                 * If Bluetooth has not been enabled/turned on, then enabling
                 * device discoverability will automatically enable Bluetooth
                 */
                Intent discoverBt = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverBt.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 121);
                startActivityForResult(discoverBt, REQUEST_ENABLE_BLUETOOTH);
                // if this is successful, the Bluetooth transfer is started
                return true;

            case R.id.action_about:
                View view = LayoutInflater.from(this).inflate(R.layout.about, null);

                String versionName = Utils.getVersionName(this);
                if (versionName != null) {
                    ((TextView) view.findViewById(R.id.version)).setText(versionName);
                }

                AlertDialog alrt = new AlertDialog.Builder(this).setView(view).create();
                alrt.setTitle(R.string.about_title);
                alrt.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        });
                alrt.show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case REQUEST_PREFS:
                // The automatic update settings may have changed, so reschedule (or
                // unschedule) the service accordingly. It's cheap, so no need to
                // check if the particular setting has actually been changed.
                UpdateService.schedule(getBaseContext());

                if ((resultCode & PreferencesActivity.RESULT_RESTART) != 0) {
                    ((FDroidApp) getApplication()).reloadTheme();
                    final Intent intent = getIntent();
                    overridePendingTransition(0, 0);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    finish();
                    overridePendingTransition(0, 0);
                    startActivity(intent);
                }
                break;
            case REQUEST_ENABLE_BLUETOOTH:
                fdroidApp.sendViaBluetooth(this, resultCode, "org.fdroid.fdroid");
                break;
        }
    }

    private void createViews() {
        viewPager = (ViewPager) findViewById(R.id.main_pager);
        this.adapter = new AppListFragmentPagerAdapter(this);
        viewPager.setAdapter(this.adapter);
        viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                getTabManager().selectTab(position);
            }
        });
    }

    @NonNull
    private TabManager getTabManager() {
        if (tabManager == null) {
            tabManager = new TabManager(this, viewPager);
        }
        return tabManager;
    }

    private void refreshUpdateTabLabel() {
        getTabManager().refreshTabLabel(TabManager.INDEX_CAN_UPDATE);
        getTabManager().refreshTabLabel(TabManager.INDEX_INSTALLED);
    }

    public void removeNotification(int id) {
        NotificationManager nMgr = (NotificationManager) getBaseContext()
            .getSystemService(Context.NOTIFICATION_SERVICE);
        nMgr.cancel(id);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        // Do nothing, because we respond to the query being changed as it is updated
        // via onQueryTextChange(...)
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        adapter.updateSearchQuery(newText, getTabManager().getSelectedIndex());
        return true;
    }

    private class AppObserver extends ContentObserver {

        AppObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            FDroid.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshUpdateTabLabel();
                }
            });
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

    }

}
