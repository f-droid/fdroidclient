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

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.Toast;
import com.ashokvarma.bottomnavigation.BottomNavigationBar;
import com.ashokvarma.bottomnavigation.BottomNavigationItem;
import com.ashokvarma.bottomnavigation.TextBadgeItem;
import org.fdroid.fdroid.AppDetails2;
import org.fdroid.fdroid.AppUpdateStatusManager;
import org.fdroid.fdroid.AppUpdateStatusManager.AppUpdateStatus;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.NfcHelper;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.NewRepoConfig;
import org.fdroid.fdroid.views.ManageReposActivity;
import org.fdroid.fdroid.views.apps.AppListActivity;
import org.fdroid.fdroid.views.swap.SwapWorkflowActivity;

/**
 * Main view shown to users upon starting F-Droid.
 * <p>
 * Shows a bottom navigation bar, with the following entries:
 * + Whats new
 * + Categories list
 * + App swap
 * + Updates
 * + Settings
 * <p>
 * Users navigate between items by using the bottom navigation bar, or by swiping left and right.
 * When switching from one screen to the next, we stay within this activity. The new screen will
 * get inflated (if required)
 */
public class MainActivity extends AppCompatActivity implements BottomNavigationBar.OnTabSelectedListener {

    private static final String TAG = "MainActivity";

    public static final String EXTRA_VIEW_UPDATES = "org.fdroid.fdroid.views.main.MainActivity.VIEW_UPDATES";
    public static final String EXTRA_VIEW_SETTINGS = "org.fdroid.fdroid.views.main.MainActivity.VIEW_SETTINGS";

    private static final String ADD_REPO_INTENT_HANDLED = "addRepoIntentHandled";

    private static final String ACTION_ADD_REPO = "org.fdroid.fdroid.MainActivity.ACTION_ADD_REPO";

    private static final String STATE_SELECTED_MENU_ID = "selectedMenuId";

    private static final int REQUEST_SWAP = 3;

    private RecyclerView pager;
    private MainViewAdapter adapter;
    private BottomNavigationBar bottomNavigation;
    private int selectedMenuId;
    private TextBadgeItem updatesBadge;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ((FDroidApp) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        adapter = new MainViewAdapter(this);

        pager = (RecyclerView) findViewById(R.id.main_view_pager);
        pager.setHasFixedSize(true);
        pager.setLayoutManager(new NonScrollingHorizontalLayoutManager(this));
        pager.setAdapter(adapter);

        // Without this, the focus is completely busted on pre 15 devices. Trying to use them
        // without this ends up with each child view showing for a fraction of a second, then
        // reverting back to the "Latest" screen again, in completely non-deterministic ways.
        if (Build.VERSION.SDK_INT <= 15) {
            pager.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        }

        updatesBadge = new TextBadgeItem().hide(false);

        bottomNavigation = (BottomNavigationBar) findViewById(R.id.bottom_navigation);
        bottomNavigation
                .addItem(new BottomNavigationItem(R.drawable.ic_latest, R.string.main_menu__latest_apps));
        if (BuildConfig.FLAVOR.startsWith("full")) {
            bottomNavigation
                    .addItem(new BottomNavigationItem(R.drawable.ic_categories, R.string.main_menu__categories))
                    .addItem(new BottomNavigationItem(R.drawable.ic_nearby, R.string.main_menu__swap_nearby));
        }
        bottomNavigation.setTabSelectedListener(this)
                .setBarBackgroundColor(getBottomNavigationBackgroundColorResId())
                .setInActiveColor(R.color.bottom_nav_items)
                .setActiveColor(android.R.color.white)
                .setMode(BottomNavigationBar.MODE_FIXED)
                .addItem(new BottomNavigationItem(R.drawable.ic_updates, R.string.updates).setBadgeItem(updatesBadge))
                .addItem(new BottomNavigationItem(R.drawable.ic_settings, R.string.menu_settings))
                .initialise();

        IntentFilter updateableAppsFilter = new IntentFilter(AppUpdateStatusManager.BROADCAST_APPSTATUS_LIST_CHANGED);
        updateableAppsFilter.addAction(AppUpdateStatusManager.BROADCAST_APPSTATUS_CHANGED);
        updateableAppsFilter.addAction(AppUpdateStatusManager.BROADCAST_APPSTATUS_REMOVED);
        LocalBroadcastManager.getInstance(this).registerReceiver(onUpdateableAppsChanged, updateableAppsFilter);

        if (savedInstanceState != null) {
            selectedMenuId = savedInstanceState.getInt(STATE_SELECTED_MENU_ID, (int) adapter.getItemId(0));
        } else {
            selectedMenuId = (int) adapter.getItemId(0);
        }
        setSelectedMenuInNav();

        initialRepoUpdateIfRequired();

        Intent intent = getIntent();
        handleSearchOrAppViewIntent(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_SELECTED_MENU_ID, selectedMenuId);
        super.onSaveInstanceState(outState);
    }

    private void setSelectedMenuInNav() {
        bottomNavigation.selectTab(adapter.adapterPositionFromItemId(selectedMenuId));
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

        FDroidApp.checkStartTor(this);

        if (getIntent().hasExtra(EXTRA_VIEW_UPDATES)) {
            getIntent().removeExtra(EXTRA_VIEW_UPDATES);
            pager.scrollToPosition(adapter.adapterPositionFromItemId(R.id.updates));
            selectedMenuId = R.id.updates;
            setSelectedMenuInNav();
        } else if (getIntent().hasExtra(EXTRA_VIEW_SETTINGS)) {
            getIntent().removeExtra(EXTRA_VIEW_SETTINGS);
            pager.scrollToPosition(adapter.adapterPositionFromItemId(R.id.settings));
            selectedMenuId = R.id.settings;
            setSelectedMenuInNav();
        }

        // AppDetails2 and RepoDetailsActivity set different NFC actions, so reset here
        NfcHelper.setAndroidBeam(this, getApplication().getPackageName());
        checkForAddRepoIntent(getIntent());
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
    public void onTabSelected(int position) {
        pager.scrollToPosition(position);
        selectedMenuId = (int) adapter.getItemId(position);
    }

    @Override
    public void onTabUnselected(int position) {

    }

    @Override
    public void onTabReselected(int position) {

    }

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
            Utils.debugLog(TAG, "FDroid launched via app link for '" + packageName + "'");
            Intent intentToInvoke = new Intent(this, AppDetails2.class);
            intentToInvoke.putExtra(AppDetails2.EXTRA_APPID, packageName);
            startActivity(intentToInvoke);
            finish();
        } else if (!TextUtils.isEmpty(query)) {
            Utils.debugLog(TAG, "FDroid launched via search link for '" + query + "'");
            performSearch(query);
        }
    }

    /**
     * Initiates the {@link AppListActivity} with the relevant search terms passed in via the query arg.
     */
    private void performSearch(String query) {
        Intent searchIntent = new Intent(this, AppListActivity.class);
        searchIntent.putExtra(AppListActivity.EXTRA_SEARCH_TERMS, query);
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
                    Intent confirmIntent = new Intent(this, SwapWorkflowActivity.class);
                    confirmIntent.putExtra(SwapWorkflowActivity.EXTRA_CONFIRM, true);
                    confirmIntent.setData(intent.getData());
                    startActivityForResult(confirmIntent, REQUEST_SWAP);
                } else {
                    startActivity(new Intent(ACTION_ADD_REPO, intent.getData(), this, ManageReposActivity.class));
                }
                finish();
            } else if (parser.getErrorMessage() != null) {
                Toast.makeText(this, parser.getErrorMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void refreshUpdatesBadge(int canUpdateCount) {
        if (canUpdateCount == 0) {
            updatesBadge.hide(true);
        } else {
            updatesBadge.setText(Integer.toString(canUpdateCount));
            updatesBadge.show(true);
        }
    }

    private int getBottomNavigationBackgroundColorResId() {
        switch (FDroidApp.getCurThemeResId()) {
            case R.style.AppThemeNight:
                return R.color.fdroid_night;
            default:
                return R.color.fdroid_blue;
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

    /**
     * There are a bunch of reasons why we would get notified about app statuses.
     * The ones we are interested in are those which would result in the "items requiring user interaction"
     * to increase or decrease:
     * * Change in status to:
     * * {@link AppUpdateStatusManager.Status#ReadyToInstall} (Causes the count to go UP by one)
     * * {@link AppUpdateStatusManager.Status#Installed} (Causes the count to go DOWN by one)
     */
    private final BroadcastReceiver onUpdateableAppsChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean updateBadge = false;

            AppUpdateStatusManager manager = AppUpdateStatusManager.getInstance(context);

            String reason = intent.getStringExtra(AppUpdateStatusManager.EXTRA_REASON_FOR_CHANGE);
            switch (intent.getAction()) {
                // Apps which are added/removed from the list due to becoming ready to install or a repo being
                // disabled both cause us to increase/decrease our badge count respectively.
                case AppUpdateStatusManager.BROADCAST_APPSTATUS_LIST_CHANGED:
                    if (AppUpdateStatusManager.REASON_READY_TO_INSTALL.equals(reason) ||
                            AppUpdateStatusManager.REASON_REPO_DISABLED.equals(reason)) {
                        updateBadge = true;
                    }
                    break;

                // Apps which were previously "Ready to install" but have been removed. We need to lower our badge
                // count in response to this.
                case AppUpdateStatusManager.BROADCAST_APPSTATUS_REMOVED:
                    AppUpdateStatus status = intent.getParcelableExtra(AppUpdateStatusManager.EXTRA_STATUS);
                    if (status != null && status.status == AppUpdateStatusManager.Status.ReadyToInstall) {
                        updateBadge = true;
                    }
                    break;
            }

            // Check if we have moved into the ReadyToInstall or Installed state.
            AppUpdateStatus status = manager.get(intent.getStringExtra(AppUpdateStatusManager.EXTRA_APK_URL));
            boolean isStatusChange = intent.getBooleanExtra(AppUpdateStatusManager.EXTRA_IS_STATUS_UPDATE, false);
            if (isStatusChange
                    && status != null
                    && (status.status == AppUpdateStatusManager.Status.ReadyToInstall || status.status == AppUpdateStatusManager.Status.Installed)) { // NOCHECKSTYLE LineLength
                updateBadge = true;
            }

            if (updateBadge) {
                int count = 0;
                for (AppUpdateStatus s : manager.getAll()) {
                    if (s.status == AppUpdateStatusManager.Status.ReadyToInstall) {
                        count++;
                    }
                }

                refreshUpdatesBadge(count);
            }
        }
    };
}