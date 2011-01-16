/*
 * Copyright (C) 2011  Ciaran Gultnieks, ciaran@ciarang.com
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
import android.view.View;
import android.widget.ListView;

public class SearchResults extends ListActivity {
       
    private static final int REQUEST_APPDETAILS = 0;

    private AppListAdapter applist = new AppListAdapter(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.searchresults);

        Intent intent = getIntent();

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            DB db = new DB(this);
            Vector<DB.App> apps = db.getApps(null, query, false);
            Log.d("FDroid", "Search for '" + query + "' returned "
                    + apps.size() + " results");
            for (DB.App app : apps) {
                applist.addItem(app);
            }
            applist.notifyDataSetChanged();
            setListAdapter(applist);
            db.close();
        }
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

}
