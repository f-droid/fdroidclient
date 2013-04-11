/*
 * Copyright (C) 2010-12  Ciaran Gultnieks, ciaran@ciarang.com
 * Copyright (C) 2009  Roberto Jacinto, roberto.jacinto@caixamagica.pt
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
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

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Vector;

import android.app.*;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.*;
import android.widget.*;
import org.fdroid.fdroid.DB.App;

import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TabHost.TabSpec;
import org.fdroid.fdroid.views.AppListFragmentPageAdapter;
import org.fdroid.fdroid.views.AppListView;

public class FDroid extends FragmentActivity implements OnItemClickListener,
        OnItemSelectedListener {

    private static final int REQUEST_APPDETAILS = 0;
    private static final int REQUEST_MANAGEREPOS = 1;
    private static final int REQUEST_PREFS = 2;

    public static final String EXTRA_TAB_UPDATE = "extraTab";

    private static final int UPDATE_REPO = Menu.FIRST;
    private static final int MANAGE_REPO = Menu.FIRST + 1;
    private static final int PREFERENCES = Menu.FIRST + 2;
    private static final int ABOUT = Menu.FIRST + 3;
    private static final int SEARCH = Menu.FIRST + 4;

    // Apps that are available to be installed
    private AppListAdapter apps_av = new AppListAdapter(this);

    // Apps that are installed
    private AppListAdapter apps_in = new AppListAdapter(this);

    // Apps that can be upgraded
    private AppListAdapter apps_up = new AppListAdapter(this);

    // Category list
    private ArrayAdapter<String> categories;

    private String currentCategory = null;

    private ProgressDialog pd;

    private boolean triedEmptyUpdate;

    // List of apps.
    private Vector<DB.App> apps = null;

    private ViewPager viewPager;

    // Used by pre 3.0 devices which don't have an ActionBar...
    private TabHost tabHost;
    private AppListFragmentPageAdapter viewPageAdapter;

    // The following getters
    // (availableAdapter/installedAdapter/canUpdateAdapter/categoriesAdapter)
    // are used by the APpListViewFactory to construct views that can be used
    // either by Android 3.0+ devices with ActionBars, or earlier devices
    // with old fashioned tabs.

    public AppListAdapter getAvailableAdapter() {
        return apps_av;
    }

    public AppListAdapter getInstalledAdapter() {
        return apps_in;
    }

    public AppListAdapter getCanUpdateAdapter() {
        return apps_up;
    }

    public ArrayAdapter<String> getCategoriesAdapter() {
        return categories;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.fdroid);

        // Needs to be created before createViews(), because that will use the
        // getCategoriesAdapter() accessor which expects this object...
        categories = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, new Vector<String>());
        categories
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        createViews();
        createTabs();

        Intent i = getIntent();
        if (i.hasExtra("uri")) {
            Intent call = new Intent(this, ManageRepo.class);
            call.putExtra("uri", i.getStringExtra("uri"));
            startActivityForResult(call, REQUEST_MANAGEREPOS);
        } else if (i.hasExtra(EXTRA_TAB_UPDATE)) {
            boolean showUpdateTab = i.getBooleanExtra(EXTRA_TAB_UPDATE, false);
            if (showUpdateTab) {
                selectTab(2);
            }
        }

        triedEmptyUpdate = false;
    }

    @Override
    protected void onStart() {
        populateLists();
        super.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, UPDATE_REPO, 1, R.string.menu_update_repo).setIcon(
                android.R.drawable.ic_menu_rotate);
        menu.add(Menu.NONE, MANAGE_REPO, 2, R.string.menu_manage).setIcon(
                android.R.drawable.ic_menu_agenda);
        menu.add(Menu.NONE, SEARCH, 3, R.string.menu_search).setIcon(
                android.R.drawable.ic_menu_search);
        menu.add(Menu.NONE, PREFERENCES, 4, R.string.menu_preferences).setIcon(
                android.R.drawable.ic_menu_preferences);
        menu.add(Menu.NONE, ABOUT, 5, R.string.menu_about).setIcon(
                android.R.drawable.ic_menu_help);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

        case UPDATE_REPO:
            updateRepos();
            return true;

        case MANAGE_REPO:
            Intent i = new Intent(this, ManageRepo.class);
            startActivityForResult(i, REQUEST_MANAGEREPOS);
            return true;

        case PREFERENCES:
            Intent prefs = new Intent(getBaseContext(), Preferences.class);
            startActivityForResult(prefs, REQUEST_PREFS);
            return true;

        case SEARCH:
            onSearchRequested();
            return true;

        case ABOUT:
            LayoutInflater li = LayoutInflater.from(this);
            View view = li.inflate(R.layout.about, null);

            // Fill in the version...
            TextView tv = (TextView) view.findViewById(R.id.version);
            PackageManager pm = getPackageManager();
            try {
                PackageInfo pi = pm.getPackageInfo(getApplicationContext()
                        .getPackageName(), 0);
                tv.setText(pi.versionName);
            } catch (Exception e) {
            }

            Builder p = new AlertDialog.Builder(this).setView(view);
            final AlertDialog alrt = p.create();
            alrt.setIcon(R.drawable.icon);
            alrt.setTitle(getString(R.string.about_title));
            alrt.setButton(AlertDialog.BUTTON_NEUTRAL,
                    getString(R.string.about_website),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            Uri uri = Uri.parse("http://f-droid.org");
                            startActivity(new Intent(Intent.ACTION_VIEW, uri));
                        }
                    });
            alrt.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                        }
                    });
            alrt.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        triedEmptyUpdate = true;
        switch (requestCode) {
        case REQUEST_APPDETAILS:
            break;
        case REQUEST_MANAGEREPOS:
            if (data.hasExtra("update")) {
                AlertDialog.Builder ask_alrt = new AlertDialog.Builder(this);
                ask_alrt.setTitle(getString(R.string.repo_update_title));
                ask_alrt.setIcon(android.R.drawable.ic_menu_rotate);
                ask_alrt.setMessage(getString(R.string.repo_alrt));
                ask_alrt.setPositiveButton(getString(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                updateRepos();
                            }
                        });
                ask_alrt.setNegativeButton(getString(R.string.no),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                return;
                            }
                        });
                AlertDialog alert = ask_alrt.create();
                alert.show();
            }
            break;
        case REQUEST_PREFS:
            // The automatic update settings may have changed, so reschedule (or
            // unschedule) the service accordingly. It's cheap, so no need to
            // check if the particular setting has actually been changed.
            UpdateService.schedule(getBaseContext());
            if (data != null
                    && (data.hasExtra("reset") || data.hasExtra("update"))) {
                updateRepos();
            } else {
                populateLists();
            }
            break;

        }
    }

    private void createViews() {
        viewPager = (ViewPager)findViewById(R.id.main_pager);
        viewPageAdapter = new AppListFragmentPageAdapter(this);
        viewPager.setAdapter(viewPageAdapter);
        viewPager.setOnPageChangeListener( new ViewPager.SimpleOnPageChangeListener() {
            public void onPageSelected(int position) {
                selectTab(position);
            }
        });
    }

    private void createTabs() {
        if (Build.VERSION.SDK_INT >= 11) {
            createActionBarTabs();
        } else {
            createOldTabs();
        }
    }

    private void selectTab(int index) {
        if (Build.VERSION.SDK_INT >= 11) {
            getActionBar().setSelectedNavigationItem(index);
        } else {
            tabHost.setCurrentTab(index);
        }
    }

    private void updateTabText(int index) {
        CharSequence text = viewPager.getAdapter().getPageTitle(index);
        if ( Build.VERSION.SDK_INT >= 11) {
            getActionBar().getTabAt(index).setText(text);
        } else {

        }
    }

    private void createActionBarTabs() {
        final ActionBar actionBar = getActionBar();
        final ViewPager pager     = viewPager;
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        for (int i = 0; i < viewPager.getAdapter().getCount(); i ++) {
            CharSequence label = viewPager.getAdapter().getPageTitle(i);
            actionBar.addTab(
                actionBar.newTab()
                    .setText(label)
                    .setTabListener(new ActionBar.TabListener() {
                        public void onTabSelected(ActionBar.Tab tab,
                                                  FragmentTransaction ft) {
                            pager.setCurrentItem(tab.getPosition());
                        }

                        @Override
                        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
                        }

                        @Override
                        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
                        }
                    }));
        }
    }

    /**
     * There is a bit of boiler-plate code required to get a TabWidget showing,
     * which includes creating a TabHost, populating it with the TabWidget,
     * and giving it a FrameLayout as a child. This will make the tabs have
     * dummy empty contents and then hook them up to our ViewPager.
     */
    private void createOldTabs() {
        tabHost = new TabHost(this);
        tabHost.setLayoutParams(new TabHost.LayoutParams(
                TabHost.LayoutParams.MATCH_PARENT, TabHost.LayoutParams.WRAP_CONTENT));

        TabWidget tabWidget = new TabWidget(this);
        tabWidget.setId(android.R.id.tabs);
        tabHost.setLayoutParams(new TabHost.LayoutParams(
                TabWidget.LayoutParams.MATCH_PARENT, TabWidget.LayoutParams.WRAP_CONTENT));

        FrameLayout layout = new FrameLayout(this);
        layout.setId(android.R.id.tabcontent);
        layout.setLayoutParams(new TabWidget.LayoutParams(0, 0));

        tabHost.addView(tabWidget);
        tabHost.addView(layout);
        tabHost.setup();

        TabHost.TabContentFactory factory = new TabHost.TabContentFactory() {
            @Override
            public View createTabContent(String tag) {
                return new View(FDroid.this);
            }
        };

        TabSpec availableTabSpec = tabHost.newTabSpec("available")
                .setIndicator(
                        getString(R.string.tab_noninstalled),
                        getResources().getDrawable(android.R.drawable.ic_input_add))
                .setContent(factory);

        TabSpec installedTabSpec = tabHost.newTabSpec("installed")
                .setIndicator(
                        getString(R.string.tab_installed),
                        getResources().getDrawable(android.R.drawable.star_off))
                .setContent(factory);

        TabSpec canUpdateTabSpec = tabHost.newTabSpec("canUpdate")
                .setIndicator(
                        getString(R.string.tab_updates),
                        getResources().getDrawable(android.R.drawable.star_on))
                .setContent(factory);

        tabHost.addTab(availableTabSpec);
        tabHost.addTab(installedTabSpec);
        tabHost.addTab(canUpdateTabSpec);

        LinearLayout contentView = (LinearLayout)findViewById(R.id.fdroid_layout);
        contentView.addView(tabHost, 0);

        tabHost.setOnTabChangedListener( new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                viewPager.setCurrentItem(tabHost.getCurrentTab());
            }
        });
    }

    // Populate the lists.
    private void populateLists() {

        apps_in.clear();
        apps_av.clear();
        apps_up.clear();
        categories.clear();

        long startTime = System.currentTimeMillis();

        DB db;
        String cat_all, cat_whatsnew, cat_recentlyupdated;
        try {
            db = DB.getDB();

            // Populate the category list with the real categories, and the
            // locally generated meta-categories for "All", "What's New" and
            // "Recently  Updated"...
            cat_all = getString(R.string.category_all);
            cat_whatsnew = getString(R.string.category_whatsnew);
            cat_recentlyupdated = getString(R.string.category_recentlyupdated);
            categories.add(cat_whatsnew);
            categories.add(cat_recentlyupdated);
            categories.add(cat_all);
            for (String s : db.getCategories()) {
                categories.add(s);
            }
            if (currentCategory == null)
                currentCategory = cat_whatsnew;

        } finally {
            DB.releaseDB();
        }

        apps = ((FDroidApp) getApplication()).getApps();

        if (apps.isEmpty()) {
            // Don't attempt this more than once - we may have invalid
            // repositories.
            if (triedEmptyUpdate)
                return;
            // If there are no apps, update from the repos - it must be a
            // new installation.
            Log.d("FDroid", "Empty app list forces repo update");
            updateRepos();
            triedEmptyUpdate = true;
            return;
        }

        // Calculate the cutoff date we'll use for What's New and Recently
        // Updated...
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());
        String sint = prefs.getString("updateHistoryDays", "14");
        int history_days = Integer.parseInt(sint);

        Calendar recent = Calendar.getInstance();
        recent.add(Calendar.DAY_OF_YEAR, -history_days);
        Date recentDate = recent.getTime();

        AppFilter appfilter = new AppFilter(this);

        boolean incat;
        Vector<DB.App> availapps = new Vector<DB.App>();
        for (DB.App app : apps) {
            if (currentCategory.equals(cat_all)) {
                incat = true;
            } else if (currentCategory.equals(cat_whatsnew)) {
                if (app.added == null)
                    incat = false;
                else if (app.added.compareTo(recentDate) < 0)
                    incat = false;
                else
                    incat = true;
            } else if (currentCategory.equals(cat_recentlyupdated)) {
                if (app.lastUpdated == null)
                    incat = false;
                // Don't include in the recently updated category if the
                // 'update' was actually it being added.
                else if (app.lastUpdated.compareTo(app.added) == 0)
                    incat = false;
                else if (app.lastUpdated.compareTo(recentDate) < 0)
                    incat = false;
                else
                    incat = true;
            } else {
                incat = currentCategory.equals(app.category);
            }

            boolean filtered = appfilter.filter(app);

            // Add it to the list(s). Always to installed and updates, but
            // only to available if it's not filtered.
            if (!filtered && incat)
                availapps.add(app);
            if (app.installedVersion != null) {
                apps_in.addItem(app);
                if (app.hasUpdates)
                    apps_up.addItem(app);
            }
        }

        if (currentCategory.equals(cat_whatsnew)) {
            class WhatsNewComparator implements Comparator<DB.App> {
                @Override
                public int compare(App lhs, App rhs) {
                    return rhs.added.compareTo(lhs.added);
                }
            }
            Collections.sort(availapps, new WhatsNewComparator());
        } else if (currentCategory.equals(cat_recentlyupdated)) {
            class UpdatedComparator implements Comparator<DB.App> {
                @Override
                public int compare(App lhs, App rhs) {
                    return rhs.lastUpdated.compareTo(lhs.lastUpdated);
                }
            }
            Collections.sort(availapps, new UpdatedComparator());
        }
        for (DB.App app : availapps)
            apps_av.addItem(app);

        updateTabText(2);

        // Tell the lists that the data behind the adapter has changed, so
        // they can refresh...
        apps_av.notifyDataSetChanged();
        apps_in.notifyDataSetChanged();
        apps_up.notifyDataSetChanged();
        categories.notifyDataSetChanged();

        Log.d("FDroid", "Updated lists - " + apps.size() + " apps in total"
                + " (update took " + (System.currentTimeMillis() - startTime)
                + " ms)");
    }

    // For receiving results from the UpdateService when we've told it to
    // update in response to a user request.
    private class UpdateReceiver extends ResultReceiver {
        public UpdateReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if (resultCode == 1) {
                Toast.makeText(FDroid.this, resultData.getString("errmsg"),
                        Toast.LENGTH_LONG).show();
            } else {
                populateLists();
            }
            if (pd.isShowing())
                pd.dismiss();
        }
    }

    private UpdateReceiver mUpdateReceiver;

    // Force a repo update now. A progress dialog is shown and the UpdateService
    // is told to do the update, which will result in the database changing. The
    // UpdateReceiver class should get told when this is finished.
    private void updateRepos() {
        pd = ProgressDialog.show(this, getString(R.string.process_wait_title),
                getString(R.string.process_update_msg), true, true);
        pd.setIcon(android.R.drawable.ic_dialog_info);
        pd.setCanceledOnTouchOutside(false);

        Intent intent = new Intent(this, UpdateService.class);
        mUpdateReceiver = new UpdateReceiver(new Handler());
        intent.putExtra("receiver", mUpdateReceiver);
        startService(intent);
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos,
            long id) {
        currentCategory = parent.getItemAtPosition(pos).toString();
        populateLists();
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // We always have at least "All"
    }

    // Handler for a click on one of the items in an application list. Pops
    // up a dialog that shows the details of the application and all its
    // available versions, with buttons to allow installation etc.
    public void onItemClick(AdapterView<?> arg0, View arg1, final int arg2,
            long arg3) {

        int currentItem = viewPager.getCurrentItem();
        Fragment fragment = viewPageAdapter.getItem(currentItem);

        // The fragment.getView() returns a wrapper object which has the
        // actual view we're interested in inside:
        //   http://stackoverflow.com/a/13684505
        ViewGroup group = (ViewGroup)fragment.getView();
        AppListView view  = (AppListView)group.getChildAt(0);
        final DB.App app = (DB.App)view.getAppList().getAdapter().getItem(arg2);

        Intent intent = new Intent(this, AppDetails.class);
        intent.putExtra("appid", app.id);
        startActivityForResult(intent, REQUEST_APPDETAILS);

    }

}
