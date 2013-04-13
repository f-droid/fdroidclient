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

import android.app.ActionBar;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Vector;

import android.support.v4.view.MenuItemCompat;
import org.fdroid.fdroid.DB.App;
import org.fdroid.fdroid.R;

import android.R.drawable;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import android.widget.TabHost.TabSpec;
import org.fdroid.fdroid.views.AppListFragmentPageAdapter;

public class FDroid extends FragmentActivity {

    public static final int REQUEST_APPDETAILS = 0;
    public static final int REQUEST_MANAGEREPOS = 1;
    public static final int REQUEST_PREFS = 2;

    public static final String EXTRA_TAB_UPDATE = "extraTab";

    private static final int UPDATE_REPO = Menu.FIRST;
    private static final int MANAGE_REPO = Menu.FIRST + 1;
    private static final int PREFERENCES = Menu.FIRST + 2;
    private static final int ABOUT = Menu.FIRST + 3;
    private static final int SEARCH = Menu.FIRST + 4;

    private ProgressDialog pd;

    private ViewPager viewPager;

    private AppListManager manager = null;

    // Used by pre 3.0 devices which don't have an ActionBar...
    private TabHost tabHost;

    public AppListManager getManager() {
        return manager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        manager = new AppListManager(this);
        setContentView(R.layout.fdroid);
        createViews();
        createTabs();

        // Must be done *after* createViews, because it will involve a
        // callback to update the tab label for the "update" tab. This
        // will fail unless the tabs have actually been created.
        repopulateViews();

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
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    protected void repopulateViews() {
        manager.repopulateLists();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, UPDATE_REPO, 1, R.string.menu_update_repo).setIcon(
                android.R.drawable.ic_menu_rotate);
        menu.add(Menu.NONE, MANAGE_REPO, 2, R.string.menu_manage).setIcon(
                android.R.drawable.ic_menu_agenda);
        MenuItem search = menu.add(Menu.NONE, SEARCH, 3, R.string.menu_search).setIcon(
                android.R.drawable.ic_menu_search);
        menu.add(Menu.NONE, PREFERENCES, 4, R.string.menu_preferences).setIcon(
                android.R.drawable.ic_menu_preferences);
        menu.add(Menu.NONE, ABOUT, 5, R.string.menu_about).setIcon(
                android.R.drawable.ic_menu_help);
        MenuItemCompat.setShowAsAction(search, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
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
                repopulateViews();
            }
            break;

        }
    }

    private void createViews() {
        viewPager = (ViewPager)findViewById(R.id.main_pager);
        AppListFragmentPageAdapter viewPageAdapter = new AppListFragmentPageAdapter(this);
        viewPager.setAdapter(viewPageAdapter);
        viewPager.setOnPageChangeListener( new ViewPager.SimpleOnPageChangeListener() {
            public void onPageSelected(int position) {
                selectTab(position);
            }
        });
    }

    private void createTabs() {
        if (Utils.hasApi(11)) {
            createActionBarTabs();
        } else {
            createOldTabs();
        }
    }

    private void selectTab(int index) {
        if (Utils.hasApi(11)) {
            getActionBar().setSelectedNavigationItem(index);
        } else {
            tabHost.setCurrentTab(index);
        }
    }

    public void refreshUpdateTabLabel() {
        final int INDEX = 2;
        CharSequence text = viewPager.getAdapter().getPageTitle(INDEX);
        if (Utils.hasApi(11)) {
            getActionBar().getTabAt(INDEX).setText(text);
        } else {
             // Update the count on the 'Updates' tab to show the number available.
            // This is quite unpleasant, but seems to be the only way to do it.
            TextView textView = (TextView) tabHost.getTabWidget().getChildAt(2)
                    .findViewById(android.R.id.title);
            textView.setText(text);
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
                repopulateViews();
            }
            if (pd.isShowing())
                pd.dismiss();
        }
    }

    private UpdateReceiver mUpdateReceiver;

    /**
     * The first time the app is run, we will have an empty app list.
     * If this is the case, we will attempt to update with the default repo.
     * However, if we have tried this at least once, then don't try to do
     * it automatically again, because the repos or internet connection may
     * be bad.
     */
    public boolean updateEmptyRepos() {
        final String TRIED_EMPTY_UPDATE = "triedEmptyUpdate";
        boolean hasTriedEmptyUpdate = getPreferences(MODE_PRIVATE).getBoolean(TRIED_EMPTY_UPDATE, false);
        if (!hasTriedEmptyUpdate) {
            Log.d("FDroid", "Empty app list, and we haven't done an update yet. Forcing repo update.");
            getPreferences(MODE_PRIVATE).edit().putBoolean(TRIED_EMPTY_UPDATE, true).apply();
            updateRepos();
            return true;
        } else {
            Log.d("FDroid", "Empty app list, but it looks like we've had an update previously. Will not force repo update.");
            return false;
        }
    }

    // Force a repo update now. A progress dialog is shown and the UpdateService
    // is told to do the update, which will result in the database changing. The
    // UpdateReceiver class should get told when this is finished.
    public void updateRepos() {

        pd = ProgressDialog.show(this, getString(R.string.process_wait_title),
                getString(R.string.process_update_msg), true, true);
        pd.setIcon(android.R.drawable.ic_dialog_info);
        pd.setCanceledOnTouchOutside(false);

        Intent intent = new Intent(this, UpdateService.class);
        mUpdateReceiver = new UpdateReceiver(new Handler());
        intent.putExtra("receiver", mUpdateReceiver);
        startService(intent);
    }

}
