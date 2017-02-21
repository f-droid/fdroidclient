package org.fdroid.fdroid.views.main;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.view.MenuItem;
import android.support.v7.widget.RecyclerView;

import org.fdroid.fdroid.AppDetails;
import org.fdroid.fdroid.AppDetails2;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.NfcHelper;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.compat.UriCompat;
import org.fdroid.fdroid.views.apps.AppListActivity;

/**
 * Main view shown to users upon starting F-Droid.
 *
 * Shows a bottom navigation bar, with the following entries:
 *  + Whats new
 *  + Categories list
 *  + App swap
 *  + My apps
 *  + Settings
 *
 *  Users navigate between items by using the bottom navigation bar, or by swiping left and right.
 *  When switching from one screen to the next, we stay within this activity. The new screen will
 *  get inflated (if required)
 */
public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";

    public static final String EXTRA_VIEW_MY_APPS = "org.fdroid.fdroid.views.main.MainActivity.VIEW_MY_APPS";

    private RecyclerView pager;
    private MainViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        adapter = new MainViewAdapter(this);

        pager = (RecyclerView) findViewById(R.id.main_view_pager);
        pager.setHasFixedSize(true);
        pager.setLayoutManager(new NonScrollingHorizontalLayoutManager(this));
        pager.setAdapter(adapter);

        BottomNavigationView bottomNavigation = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnNavigationItemSelectedListener(this);

        initialRepoUpdateIfRequired();

        Intent intent = getIntent();
        handleSearchOrAppViewIntent(intent);
    }

    /**
     * The first time the app is run, we will have an empty app list. To deal with this, we will
     * attempt to update with the default repo. However, if we have tried this at least once, then
     * don't try to do it automatically again.
     */
    private void initialRepoUpdateIfRequired() {
        final String triedEmptyUpdate = "triedEmptyUpdate";
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        boolean hasTriedEmptyUpdate = prefs.getBoolean(triedEmptyUpdate, false);
        if (!hasTriedEmptyUpdate) {
            Utils.debugLog(TAG, "We haven't done an update yet. Forcing repo update.");
            prefs.edit().putBoolean(triedEmptyUpdate, true).apply();
            UpdateService.updateNow(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        FDroidApp.checkStartTor(this);

        if (getIntent().hasExtra(EXTRA_VIEW_MY_APPS)) {
            getIntent().removeExtra(EXTRA_VIEW_MY_APPS);
            pager.scrollToPosition(adapter.adapterPositionFromItemId(R.id.my_apps));
        }

        // AppDetails  2 and RepoDetailsActivity set different NFC actions, so reset here
        NfcHelper.setAndroidBeam(this, getApplication().getPackageName());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleSearchOrAppViewIntent(intent);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        pager.scrollToPosition(((MainViewAdapter) pager.getAdapter()).adapterPositionFromItemId(item.getItemId()));
        return true;
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
                    if (path.startsWith("/repository/browse")) {
                        // http://f-droid.org/repository/browse?fdfilter=search+query
                        query = UriCompat.getQueryParameter(data, "fdfilter");

                        // http://f-droid.org/repository/browse?fdid=packageName
                        packageName = UriCompat.getQueryParameter(data, "fdid");
                    } else if (path.startsWith("/app")) {
                        // http://f-droid.org/app/packageName
                        packageName = data.getLastPathSegment();
                        if ("app".equals(packageName)) {
                            packageName = null;
                        }
                    }
                    break;
                case "details":
                    // market://details?id=app.id
                    packageName = UriCompat.getQueryParameter(data, "id");
                    break;
                case "search":
                    // market://search?q=query
                    query = UriCompat.getQueryParameter(data, "q");
                    break;
                case "play.google.com":
                    if (path.startsWith("/store/apps/details")) {
                        // http://play.google.com/store/apps/details?id=app.id
                        packageName = UriCompat.getQueryParameter(data, "id");
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
                    packageName = UriCompat.getQueryParameter(data, "p");
                    query = UriCompat.getQueryParameter(data, "s");
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
            intentToInvoke.putExtra(AppDetails.EXTRA_APPID, packageName);
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