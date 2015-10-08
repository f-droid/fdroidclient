/*
 * Copyright (C) 2010-12  Ciaran Gultnieks, ciaran@ciarang.com
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

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.widget.LinearLayout;

import org.fdroid.fdroid.views.fragments.PreferencesFragment;

public class PreferencesActivity extends ActionBarActivity {

    public static final int RESULT_RESTART = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ((FDroidApp) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);

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

            PreferencesFragment preferencesFragment = new PreferencesFragment();
            fm.beginTransaction()
                    .add(android.R.id.content, preferencesFragment)
                    .commit();
        }

        // Actionbar cannot be accessed until after setContentView (on 3.0 and 3.1 devices)
        // see: http://blog.perpetumdesign.com/2011/08/strange-case-of-dr-action-and-mr-bar.html
        // for reason why.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
