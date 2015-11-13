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

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

import org.fdroid.fdroid.views.fragments.SearchResultsFragment;

public class SearchResults extends ActionBarActivity {

    private static final int SEARCH = Menu.FIRST;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        ((FDroidApp) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);

        // Start a search by just typing
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentById(android.R.id.content) == null) {

            // Need to set a dummy view (which will get overridden by the fragment manager
            // below) so that we can call setContentView(). This is a work around for
            // a (bug?) thing in 3.0, 3.1 which requires setContentView to be invoked before
            // the actionbar is played with:
            // http://blog.perpetumdesign.com/2011/08/strange-case-of-dr-action-and-mr-bar.html
            if (Build.VERSION.SDK_INT >= 11 && Build.VERSION.SDK_INT <= 13) {
                setContentView(new LinearLayout(this));
            }

            SearchResultsFragment fragment = new SearchResultsFragment();
            fm.beginTransaction().add(android.R.id.content, fragment).commit();
        }

        // Actionbar cannot be accessed until after setContentView (on 3.0 and 3.1 devices)
        // see: http://blog.perpetumdesign.com/2011/08/strange-case-of-dr-action-and-mr-bar.html
        // for reason why.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
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
