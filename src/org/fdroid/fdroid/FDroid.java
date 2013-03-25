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

import org.fdroid.fdroid.DB.App;
import org.fdroid.fdroid.R;

import android.R.drawable;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TabHost.TabSpec;

public class FDroid extends TabActivity implements OnItemClickListener,
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

    // Tags for the tabs
    private static final String TAB_Installed = "I";
    private static final String TAB_Available = "A";
    private static final String TAB_Updates = "U";

    private TabHost tabHost;
    private TabSpec ts;
    private TabSpec ts1;
    private TabSpec tsUp;

    private boolean triedEmptyUpdate;

    // List of apps.
    private Vector<DB.App> apps = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.fdroid);

        categories = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, new Vector<String>());
        categories
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        tabHost = getTabHost();
        createTabs();

        Intent i = getIntent();
        if (i.hasExtra("uri")) {
            Intent call = new Intent(this, ManageRepo.class);
            call.putExtra("uri", i.getStringExtra("uri"));
            startActivityForResult(call, REQUEST_MANAGEREPOS);
        } else if (i.hasExtra(EXTRA_TAB_UPDATE)) {
            boolean updateTab = i.getBooleanExtra(EXTRA_TAB_UPDATE, false);
            if (updateTab) {
                tabHost.setCurrentTab(2);
            }
        }

        triedEmptyUpdate = false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        populateLists();
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

    private void createTabs() {
        tabHost.clearAllTabs();

        // TabContentFactory that can generate the appropriate list for each
        // tab...
        TabHost.TabContentFactory tf = new TabHost.TabContentFactory() {
            @Override
            public View createTabContent(String tag) {

                AppListAdapter ad;
                if (tag.equals(TAB_Installed))
                    ad = apps_in;
                else if (tag.equals(TAB_Updates))
                    ad = apps_up;
                else
                    ad = apps_av;

                ListView lst = new ListView(FDroid.this);
                lst.setFastScrollEnabled(true);
                lst.setOnItemClickListener(FDroid.this);
                lst.setAdapter(ad);

                if (!tag.equals(TAB_Available))
                    return lst;

                LinearLayout v = new LinearLayout(FDroid.this);
                v.setOrientation(LinearLayout.VERTICAL);
                Spinner cats = new Spinner(FDroid.this);
                // Giving it an ID lets the default save/restore state
                // functionality do its stuff.
                cats.setId(R.id.categorySpinner);
                cats.setAdapter(categories);
                cats.setOnItemSelectedListener(FDroid.this);
                v.addView(cats, new LayoutParams(
                        LinearLayout.LayoutParams.FILL_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                v.addView(lst, new LayoutParams(
                        LinearLayout.LayoutParams.FILL_PARENT,
                        LinearLayout.LayoutParams.FILL_PARENT));
                return v;
            }
        };

        // Create the tab of installed apps...
        ts = tabHost.newTabSpec(TAB_Installed);
        ts.setIndicator(getString(R.string.tab_installed), getResources()
                .getDrawable(drawable.star_off));
        ts.setContent(tf);

        // Create the tab of apps with updates...
        tsUp = tabHost.newTabSpec(TAB_Updates);
        tsUp.setIndicator(getString(R.string.tab_updates), getResources()
                .getDrawable(drawable.star_on));
        tsUp.setContent(tf);

        // Create the tab of available apps...
        ts1 = tabHost.newTabSpec(TAB_Available);
        ts1.setIndicator(getString(R.string.tab_noninstalled), getResources()
                .getDrawable(drawable.ic_input_add));
        ts1.setContent(tf);

        tabHost.addTab(ts1);
        tabHost.addTab(ts);
        tabHost.addTab(tsUp);

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

        // Update the count on the 'Updates' tab to show the number available.
        // This is quite unpleasant, but seems to be the only way to do it.
        TextView uptext = (TextView) tabHost.getTabWidget().getChildAt(2)
                .findViewById(android.R.id.title);
        uptext.setText(getString(R.string.tab_updates) + " ("
                + Integer.toString(apps_up.getCount()) + ")");

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

        final DB.App app;
        String curtab = tabHost.getCurrentTabTag();
        if (curtab.equalsIgnoreCase(TAB_Installed)) {
            app = (DB.App) apps_in.getItem(arg2);
        } else if (curtab.equalsIgnoreCase(TAB_Updates)) {
            app = (DB.App) apps_up.getItem(arg2);
        } else {
            app = (DB.App) apps_av.getItem(arg2);
        }

        Intent intent = new Intent(this, AppDetails.class);
        intent.putExtra("appid", app.id);
        startActivityForResult(intent, REQUEST_APPDETAILS);

    }

}
