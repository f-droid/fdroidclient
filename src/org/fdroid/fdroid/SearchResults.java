/*
 * Copyright (C) 2011-13  Ciaran Gultnieks, ciaran@ciarang.com
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

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuItemCompat;

import org.fdroid.fdroid.compat.ActionBarCompat;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.views.AppListAdapter;
import org.fdroid.fdroid.views.AvailableAppListAdapter;
import org.fdroid.fdroid.views.fragments.AppListFragment;

public class SearchResults extends ListActivity {

    private static final int REQUEST_APPDETAILS = 0;

    private static final int SEARCH = Menu.FIRST;

    private AppListAdapter adapter;

    protected String getQuery() {
        Intent intent = getIntent();
        String query;
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            query = intent.getStringExtra(SearchManager.QUERY);
        } else {
            Uri data = intent.getData();
            if (data != null && data.isHierarchical()) {
                query = data.getQueryParameter("q");
                if (query != null && query.startsWith("pname:"))
                    query = query.substring(6);
            } else {
                query = data.getEncodedSchemeSpecificPart();
            }
        }
        return query;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        ((FDroidApp) getApplication()).applyTheme(this);

        super.onCreate(savedInstanceState);
        ActionBarCompat.create(this).setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.searchresults);

        // Start a search by just typing
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateView();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private void updateView() {

        String query = getQuery();

        if (query != null)
            query = query.trim();

        if (query == null || query.length() == 0)
            finish();

        Cursor cursor = getContentResolver().query(
            AppProvider.getSearchUri(query), AppListFragment.APP_PROJECTION,
            null, null, AppListFragment.APP_SORT);

        TextView tv = (TextView) findViewById(R.id.description);
        String headertext;
        int count = cursor != null ? cursor.getCount() : 0;
        if (count == 0) {
            headertext = getString(R.string.searchres_noapps, query);
        } else if (count == 1) {
            headertext = getString(R.string.searchres_oneapp, query);
        } else {
            headertext = getString(R.string.searchres_napps, count, query);
        }
        tv.setText(headertext);
        Log.d("FDroid", "Search for '" + query + "' returned " + count + " results");

        adapter = new AvailableAppListAdapter(this, cursor);
        getListView().setFastScrollEnabled(true);
        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final App app;
        app = new App((Cursor) adapter.getItem(position));

        Intent intent = new Intent(this, AppDetails.class);
        intent.putExtra("appid", app.id);
        startActivityForResult(intent, REQUEST_APPDETAILS);
        super.onListItemClick(l, v, position, id);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);
        MenuItem search = menu.add(Menu.NONE, SEARCH, 1, R.string.menu_search).setIcon(
                android.R.drawable.ic_menu_search);
        MenuItemCompat.setShowAsAction(search, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

        case android.R.id.home:
            NavUtils.navigateUpFromSameTask(this);
            return true;

        case SEARCH:
            onSearchRequested();
            return true;

        }
        return super.onOptionsItemSelected(item);
    }

}
