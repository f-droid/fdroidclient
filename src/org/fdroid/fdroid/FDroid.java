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

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.NotificationManager;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import android.support.v4.app.FragmentActivity;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;

import org.fdroid.fdroid.compat.TabManager;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.views.AppListFragmentPageAdapter;

public class FDroid extends FragmentActivity {

    public static final int REQUEST_APPDETAILS = 0;
    public static final int REQUEST_MANAGEREPOS = 1;
    public static final int REQUEST_PREFS = 2;

    public static final String EXTRA_TAB_UPDATE = "extraTab";

    private static final int MANAGE_REPO = Menu.FIRST;
    private static final int PREFERENCES = Menu.FIRST + 1;
    private static final int ABOUT = Menu.FIRST + 2;
    private static final int SEARCH = Menu.FIRST + 3;

    private ViewPager viewPager;

    private TabManager tabManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ((FDroidApp) getApplication()).applyTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.fdroid);
        createViews();
        getTabManager().createTabs();

        // Start a search by just typing
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        Intent i = getIntent();
        Uri data = i.getData();
        String appid = null;
        if (data != null) {
            if (data.isHierarchical()) {
                // http(s)://f-droid.org/repository/browse?fdid=app.id
                appid = data.getQueryParameter("fdid");
            }
        } else if (i.hasExtra(EXTRA_TAB_UPDATE)) {
            boolean showUpdateTab = i.getBooleanExtra(EXTRA_TAB_UPDATE, false);
            if (showUpdateTab) {
                getTabManager().selectTab(2);
            }
        }
        if (appid != null && appid.length() > 0) {
            Intent call = new Intent(this, AppDetails.class);
            call.putExtra("appid", appid);
            startActivityForResult(call, REQUEST_APPDETAILS);
        }

        Uri uri = AppProvider.getContentUri();
        getContentResolver().registerContentObserver(uri, true, new AppObserver());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getTabManager().onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, MANAGE_REPO, 2, R.string.menu_manage).setIcon(
                android.R.drawable.ic_menu_agenda);
        MenuItem search = menu.add(Menu.NONE, SEARCH, 3, R.string.menu_search).setIcon(
                android.R.drawable.ic_menu_search);
        menu.add(Menu.NONE, PREFERENCES, 4, R.string.menu_preferences).setIcon(
                android.R.drawable.ic_menu_preferences);
        menu.add(Menu.NONE, ABOUT, 5, R.string.menu_about).setIcon(
                android.R.drawable.ic_menu_help);
        MenuItemCompat.setShowAsAction(search, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

        case MANAGE_REPO:
            Intent i = new Intent(this, ManageRepo.class);
            startActivityForResult(i, REQUEST_MANAGEREPOS);
            return true;

        case PREFERENCES:
            Intent prefs = new Intent(getBaseContext(), PreferencesActivity.class);
            startActivityForResult(prefs, REQUEST_PREFS);
            return true;

        case SEARCH:
            onSearchRequested();
            return true;

        case ABOUT:
            View view = null;
            if (Build.VERSION.SDK_INT >= 11) {
                LayoutInflater li = LayoutInflater.from(this);
                view = li.inflate(R.layout.about, null);
            } else {
                view = View.inflate(
                        new ContextThemeWrapper(this, R.style.AboutDialogLight),
                        R.layout.about, null);
            }

            // Fill in the version...
            try {
                PackageInfo pi = getPackageManager()
                    .getPackageInfo(getApplicationContext()
                            .getPackageName(), 0);
                ((TextView) view.findViewById(R.id.version))
                    .setText(pi.versionName);
            } catch (Exception e) {
            }

            Builder p = null;
            if (Build.VERSION.SDK_INT >= 11) {
                p = new AlertDialog.Builder(this).setView(view);
            } else {
                p = new AlertDialog.Builder(
                        new ContextThemeWrapper(
                            this, R.style.AboutDialogLight)
                        ).setView(view);
            }
            final AlertDialog alrt = p.create();
            alrt.setIcon(R.drawable.ic_launcher);
            alrt.setTitle(getString(R.string.about_title));
            alrt.setButton(AlertDialog.BUTTON_NEUTRAL,
                    getString(R.string.about_website),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            Uri uri = Uri.parse("https://f-droid.org");
                            startActivity(new Intent(Intent.ACTION_VIEW, uri));
                        }
                    });
            alrt.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
                        @Override
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
            if (data.hasExtra(ManageRepo.REQUEST_UPDATE)) {
                AlertDialog.Builder ask_alrt = new AlertDialog.Builder(this);
                ask_alrt.setTitle(getString(R.string.repo_update_title));
                ask_alrt.setIcon(android.R.drawable.ic_menu_rotate);
                ask_alrt.setMessage(getString(R.string.repo_alrt));
                ask_alrt.setPositiveButton(getString(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                            updateRepos();
                            }
                        });
                ask_alrt.setNegativeButton(getString(R.string.no),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                // do nothing
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

            if ((resultCode & PreferencesActivity.RESULT_RELOAD) != 0) {
                ((FDroidApp) getApplication()).invalidateAllApps();
            }

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
        }
    }

    private void createViews() {
        viewPager = (ViewPager)findViewById(R.id.main_pager);
        AppListFragmentPageAdapter viewPageAdapter = new AppListFragmentPageAdapter(this);
        viewPager.setAdapter(viewPageAdapter);
        viewPager.setOnPageChangeListener( new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                getTabManager().selectTab(position);
            }
        });
    }

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
            getPreferences(MODE_PRIVATE).edit().putBoolean(TRIED_EMPTY_UPDATE, true).commit();
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
        UpdateService.updateNow(this);
    }

    private TabManager getTabManager() {
        if (tabManager == null) {
            tabManager = TabManager.create(this, viewPager);
        }
        return tabManager;
    }

    public void refreshUpdateTabLabel() {
        getTabManager().refreshTabLabel(TabManager.INDEX_CAN_UPDATE);
    }

    public void removeNotification(int id) {
        NotificationManager nMgr = (NotificationManager) getBaseContext()
            .getSystemService(Context.NOTIFICATION_SERVICE);
        nMgr.cancel(id);
    }

    private class AppObserver extends ContentObserver {

        public AppObserver() {
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
