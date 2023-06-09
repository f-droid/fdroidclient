/*
 * Copyright (C) 2016-2017 Peter Serwylo
 * Copyright (C) 2017 Christine Emrich
 * Copyright (C) 2017 Hans-Christoph Steiner
 * Copyright (C) 2018 Senecto Limited
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.fdroid.fdroid.views.main;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.fdroid.fdroid.AppUpdateStatusManager;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.NfcHelper;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.NewRepoConfig;
import org.fdroid.fdroid.nearby.SDCardScannerService;
import org.fdroid.fdroid.nearby.SwapService;
import org.fdroid.fdroid.nearby.SwapWorkflowActivity;
import org.fdroid.fdroid.nearby.TreeUriScannerIntentService;
import org.fdroid.fdroid.nearby.WifiStateChangeService;
import org.fdroid.fdroid.views.AppDetailsActivity;
import org.fdroid.fdroid.views.ManageReposActivity;
import org.fdroid.fdroid.views.apps.AppListActivity;

/**
 * Main view shown to users upon starting F-Droid.
 * <p>
 * Shows a bottom navigation bar, with the following entries:
 * + What's new
 * + Categories list
 * + App swap
 * + Updates
 * + Settings
 * <p>
 * Users navigate between items by using the bottom navigation bar, or by swiping left and right.
 * When switching from one screen to the next, we stay within this activity. The new screen will
 * get inflated (if required)
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final String EXTRA_VIEW_UPDATES = "org.fdroid.fdroid.views.main.MainActivity.VIEW_UPDATES";
    private static final String EXTRA_VIEW_NEARBY = "org.fdroid.fdroid.views.main.MainActivity.VIEW_NEARBY";
    public static final String EXTRA_VIEW_SETTINGS = "org.fdroid.fdroid.views.main.MainActivity.VIEW_SETTINGS";

    static final int REQUEST_LOCATION_PERMISSIONS = 0xEF0F;
    static final int REQUEST_STORAGE_PERMISSIONS = 0xB004;
    static final int REQUEST_STORAGE_ACCESS = 0x40E5;

    private static final String ADD_REPO_INTENT_HANDLED = "addRepoIntentHandled";

    private static final String ACTION_ADD_REPO = "org.fdroid.fdroid.MainActivity.ACTION_ADD_REPO";
    public static final String ACTION_REQUEST_SWAP = "requestSwap";

    private RecyclerView pager;
    private MainViewAdapter adapter;
    private BottomNavigationView bottomNavigation;
    private BadgeDrawable updatesBadge;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                // no-op
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        FDroidApp fdroidApp = (FDroidApp) getApplication();
        fdroidApp.setSecureWindow(this);

        fdroidApp.applyPureBlackBackgroundInDarkTheme(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        adapter = new MainViewAdapter(this);

        pager = findViewById(R.id.main_view_pager);
        pager.setHasFixedSize(true);
        pager.setLayoutManager(new NonScrollingHorizontalLayoutManager(this));
        pager.setAdapter(adapter);

        bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            pager.scrollToPosition(item.getOrder());

            if (item.getItemId() == R.id.nearby) {
                NearbyViewBinder.updateUsbOtg(MainActivity.this);
            }

            return true;

        });
        updatesBadge = bottomNavigation.getOrCreateBadge(R.id.updates);
        updatesBadge.setVisible(false);

        initialRepoUpdateIfRequired();

        AppUpdateStatusManager.getInstance(this).getNumUpdatableApps().observe(this, this::refreshUpdatesBadge);

        Intent intent = getIntent();
        handleSearchOrAppViewIntent(intent);
    }

    private void setSelectedMenuInNav(int menuId) {
        int position = adapter.adapterPositionFromItemId(menuId);
        pager.scrollToPosition(position);
        bottomNavigation.setSelectedItemId(position);
    }

    private void initialRepoUpdateIfRequired() {
        if (Preferences.get().isIndexNeverUpdated() && !UpdateService.isUpdating()) {
            Utils.debugLog(TAG, "We haven't done an update yet. Forcing repo update.");
            UpdateService.updateNow(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        FDroidApp.checkStartTor(this, Preferences.get());

        if (getIntent().hasExtra(EXTRA_VIEW_UPDATES)) {
            getIntent().removeExtra(EXTRA_VIEW_UPDATES);
            setSelectedMenuInNav(R.id.updates);
        } else if (getIntent().hasExtra(EXTRA_VIEW_NEARBY)) {
            getIntent().removeExtra(EXTRA_VIEW_NEARBY);
            setSelectedMenuInNav(R.id.nearby);
        } else if (getIntent().hasExtra(EXTRA_VIEW_SETTINGS)) {
            getIntent().removeExtra(EXTRA_VIEW_SETTINGS);
            setSelectedMenuInNav(R.id.settings);
        }

        // AppDetailsActivity and RepoDetailsActivity set different NFC actions, so reset here
        NfcHelper.setAndroidBeam(this, getApplication().getPackageName());
        checkForAddRepoIntent(getIntent());
    }

    @Override
    protected void onStart() {
        super.onStart();
        // don't request this in onResume, because the launcher causes a call to that,
        // even if permission permanently denied, so we'll get an infinite loop
        if (Build.VERSION.SDK_INT >= 33) {
            String notificationPerm = Manifest.permission.POST_NOTIFICATIONS;
            if (ContextCompat.checkSelfPermission(this, notificationPerm) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(notificationPerm);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleSearchOrAppViewIntent(intent);

        // This is called here as well as onResume(), because onNewIntent() is not called the first
        // time the activity is created. An alternative option to make sure that the add repo intent
        // is always handled is to call setIntent(intent) here. However, after this good read:
        // http://stackoverflow.com/a/7749347 it seems that adding a repo is not really more
        // important than the original intent which caused the activity to start (even though it
        // could technically have been an add repo intent itself).
        // The end result is that this method will be called twice for one add repo intent. Once
        // here and once in onResume(). However, the method deals with this by ensuring it only
        // handles the same intent once.
        checkForAddRepoIntent(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_STORAGE_ACCESS) {
            TreeUriScannerIntentService.onActivityResult(this, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSIONS) {
            WifiStateChangeService.start(this, null);
            ContextCompat.startForegroundService(this, new Intent(this, SwapService.class));
        } else if (requestCode == REQUEST_STORAGE_PERMISSIONS) {
            Toast.makeText(this,
                    this.getString(R.string.scan_removable_storage_toast, ""),
                    Toast.LENGTH_SHORT).show();
            SDCardScannerService.scan(this);
        }
    }

    /**
     * Since any app could send this {@link Intent}, and the search terms are
     * fed into a SQL query, the data must be strictly sanitized to avoid
     * SQL injection attacks.
     */
    private void handleSearchOrAppViewIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            performSearch(query);
            return;
        }

        final Uri data = intent.getData();
        if (data == null) {
            return;
        }

        final String scheme = data.getScheme();
        final String path = data.getPath();
        String packageName = null;
        String query = null;
        if (data.isHierarchical()) {
            final String host = data.getHost();
            if (host == null) {
                return;
            }
            switch (host) {
                case "f-droid.org":
                case "www.f-droid.org":
                case "staging.f-droid.org":
                    if (path.startsWith("/app/") || path.startsWith("/packages/")
                            || path.matches("^/[a-z][a-z][a-zA-Z_-]*/packages/.*")) {
                        // http://f-droid.org/app/packageName
                        packageName = data.getLastPathSegment();
                    } else if (path.startsWith("/repository/browse")) {
                        // http://f-droid.org/repository/browse?fdfilter=search+query
                        query = data.getQueryParameter("fdfilter");

                        // http://f-droid.org/repository/browse?fdid=packageName
                        packageName = data.getQueryParameter("fdid");
                    } else if ("/app".equals(data.getPath()) || "/packages".equals(data.getPath())) {
                        packageName = null;
                    }
                    break;
                case "details":
                    // market://details?id=app.id
                    packageName = data.getQueryParameter("id");
                    break;
                case "search":
                    // market://search?q=query
                    query = data.getQueryParameter("q");
                    break;
                case "play.google.com":
                    if (path.startsWith("/store/apps/details")) {
                        // http://play.google.com/store/apps/details?id=app.id
                        packageName = data.getQueryParameter("id");
                    } else if (path.startsWith("/store/search")) {
                        // http://play.google.com/store/search?q=foo
                        query = data.getQueryParameter("q");
                    }
                    break;
                case "apps":
                case "amazon.com":
                case "www.amazon.com":
                    // amzn://apps/android?p=app.id
                    // http://amazon.com/gp/mas/dl/android?s=app.id
                    packageName = data.getQueryParameter("p");
                    query = data.getQueryParameter("s");
                    break;
            }
        } else if ("fdroid.app".equals(scheme)) {
            // fdroid.app:app.id
            packageName = data.getSchemeSpecificPart();
        } else if ("fdroid.search".equals(scheme)) {
            // fdroid.search:query
            query = data.getSchemeSpecificPart();
        }

        if (!TextUtils.isEmpty(query)) {
            // an old format for querying via packageName
            if (query.startsWith("pname:")) {
                packageName = query.split(":")[1];
            }

            // sometimes, search URLs include pub: or other things before the query string
            if (query.contains(":")) {
                query = query.split(":")[1];
            }
        }

        if (!TextUtils.isEmpty(packageName)) {
            // sanitize packageName to be a valid Java packageName and prevent exploits
            packageName = packageName.replaceAll("[^A-Za-z\\d_.]", "");
            Utils.debugLog(TAG, "FDroid launched via app link for '" + packageName + "'");
            Intent intentToInvoke = new Intent(this, AppDetailsActivity.class);
            intentToInvoke.putExtra(AppDetailsActivity.EXTRA_APPID, packageName);
            startActivity(intentToInvoke);
            finish();
        } else if (!TextUtils.isEmpty(query)) {
            Utils.debugLog(TAG, "FDroid launched via search link for '" + query + "'");
            performSearch(query);
        }
    }

    /**
     * These strings might end up in a SQL query, so strip all non-alpha-num
     */
    static String sanitizeSearchTerms(String query) {
        return query.replaceAll("[^\\p{L}\\d_ -]", " ");
    }

    /**
     * Initiates the {@link AppListActivity} with the relevant search terms passed in via the query arg.
     */
    private void performSearch(String query) {
        Intent searchIntent = new Intent(this, AppListActivity.class);
        searchIntent.putExtra(AppListActivity.EXTRA_SEARCH_TERMS, sanitizeSearchTerms(query));
        startActivity(searchIntent);
    }

    private void checkForAddRepoIntent(Intent intent) {
        // Don't handle the intent after coming back to this view (e.g. after hitting the back button)
        // http://stackoverflow.com/a/14820849
        if (!intent.hasExtra(ADD_REPO_INTENT_HANDLED)) {
            intent.putExtra(ADD_REPO_INTENT_HANDLED, true);
            NewRepoConfig parser = new NewRepoConfig(this, intent);
            if (parser.isValidRepo()) {
                if (parser.isFromSwap()) {
                    SwapWorkflowActivity.requestSwap(this, intent.getData());
                } else {
                    Intent clean = new Intent(ACTION_ADD_REPO, intent.getData(), this, ManageReposActivity.class);
                    if (intent.hasExtra(ManageReposActivity.EXTRA_FINISH_AFTER_ADDING_REPO)) {
                        clean.putExtra(ManageReposActivity.EXTRA_FINISH_AFTER_ADDING_REPO,
                                intent.getBooleanExtra(ManageReposActivity.EXTRA_FINISH_AFTER_ADDING_REPO, true));
                    }
                    startActivity(clean);
                }
                finish();
            } else if (parser.getErrorMessage() != null) {
                Toast.makeText(this, parser.getErrorMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void refreshUpdatesBadge(int canUpdateCount) {
        if (canUpdateCount <= 0) {
            updatesBadge.setVisible(false);
            updatesBadge.clearNumber();
        } else {
            updatesBadge.setNumber(canUpdateCount);
            updatesBadge.setVisible(true);
        }
    }

    private static class NonScrollingHorizontalLayoutManager extends LinearLayoutManager {
        NonScrollingHorizontalLayoutManager(Context context) {
            super(context, LinearLayoutManager.HORIZONTAL, false);
        }

        @Override
        public boolean canScrollHorizontally() {
            return false;
        }

        @Override
        public boolean canScrollVertically() {
            return false;
        }
    }

}