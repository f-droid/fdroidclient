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

package org.fdroid.fdroid.panic;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import org.fdroid.database.AppListItem;
import org.fdroid.database.AppPrefsDao;
import org.fdroid.database.FDroidDatabase;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.DBHelper;
import org.fdroid.fdroid.views.installed.InstalledAppListAdapter;

import java.util.List;

public class SelectInstalledAppsActivity extends AppCompatActivity {

    private InstalledAppListAdapter adapter;
    private RecyclerView appList;
    private TextView emptyState;
    private int checkId;

    private FDroidDatabase db;
    private Preferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FDroidApp fdroidApp = (FDroidApp) getApplication();
        fdroidApp.setSecureWindow(this);

        fdroidApp.applyPureBlackBackgroundInDarkTheme(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.installed_apps_layout);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.panic_add_apps_to_uninstall));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        adapter = new SelectInstalledAppListAdapter(this);

        appList = findViewById(R.id.app_list);
        appList.setHasFixedSize(true);
        appList.setLayoutManager(new LinearLayoutManager(this));
        appList.setAdapter(adapter);

        emptyState = findViewById(R.id.empty_state);

        db = DBHelper.getDb(this);
        db.getAppDao().getInstalledAppListItems(getPackageManager()).observe(this, this::onLoadFinished);
    }

    @Override
    protected void onResume() {
        super.onResume();

        prefs = Preferences.get();
    }

    public void onLoadFinished(List<AppListItem> items) {
        adapter.setApps(items);

        if (adapter.getItemCount() == 0) {
            appList.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            appList.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }

        // load app prefs for each app off the UiThread and update item if updates are ignored
        AppPrefsDao appPrefsDao = db.getAppPrefsDao();
        for (AppListItem item : items) {
            Utils.observeOnce(appPrefsDao.getAppPrefs(item.getPackageName()), this, appPrefs -> {
                if (appPrefs.getIgnoreVersionCodeUpdate() > 0) adapter.updateItem(item, appPrefs);
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem menuItem = menu.add(R.string.menu_select_for_wipe);
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        checkId = menuItem.getItemId();
        menuItem.setIcon(R.drawable.check);
        return true;
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        if (item.getItemId() == checkId) {
            prefs.setPanicWipeSet(prefs.getPanicTmpSelectedSet());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
