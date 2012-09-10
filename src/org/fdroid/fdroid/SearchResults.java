/*
 * Copyright (C) 2011-12  Ciaran Gultnieks, ciaran@ciarang.com
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

import java.util.Vector;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

public class SearchResults extends ListActivity {
       
    private static final int REQUEST_APPDETAILS = 0;

    private static final int SEARCH = Menu.FIRST;

    private AppListAdapter applist = new AppListAdapter(this);

    private String mQuery;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.searchresults);

        Intent intent = getIntent();

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            mQuery = intent.getStringExtra(SearchManager.QUERY);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction()))
            mQuery = intent.getStringExtra(SearchManager.QUERY);
        super.onNewIntent(intent);
    }

    @Override
    protected void onResume() {
        updateView();
        super.onResume();
    }

    private void updateView() {
        Vector<DB.App> apps;
        try {
            DB db = DB.getDB();
            apps = db.getApps(null, mQuery, false, true);
        } finally {
            DB.releaseDB();
        }
        TextView tv = (TextView) findViewById(R.id.description);
        String headertext;
        if(apps.size()==0)
            headertext = String.format(getString(R.string.searchres_noapps),mQuery);
        else if(apps.size()==1)
            headertext = String.format(getString(R.string.searchres_oneapp),mQuery);
        else
            headertext = String.format(getString(R.string.searchres_napps),apps.size(),mQuery);
        tv.setText(headertext);
        Log.d("FDroid", "Search for '" + mQuery + "' returned "
                + apps.size() + " results");
        applist.clear();
        for (DB.App app : apps) {
            applist.addItem(app);
        }
        applist.notifyDataSetChanged();
        setListAdapter(applist);
        
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final DB.App app;
        app = (DB.App) applist.getItem(position);

        Intent intent = new Intent(this, AppDetails.class);
        intent.putExtra("appid", app.id);
        startActivityForResult(intent, REQUEST_APPDETAILS);
        super.onListItemClick(l, v, position, id);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, SEARCH, 1, R.string.menu_search).setIcon(
                android.R.drawable.ic_menu_search);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

        case SEARCH:
            onSearchRequested();
            return true;
            
        }
        return super.onOptionsItemSelected(item);
    }
    
    
}
