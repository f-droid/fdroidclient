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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;

import org.fdroid.fdroid.compat.ActionBarCompat;
import org.fdroid.fdroid.views.fragments.RepoListFragment;

public class ManageRepo extends FragmentActivity {

    /**
     * If we have a new repo added, or the address of a repo has changed, then
     * we when we're finished, we'll set this boolean to true in the intent
     * that we finish with, to signify that we want the main list of apps
     * updated.
     */
    public static final String REQUEST_UPDATE = "update";

    private RepoListFragment listFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((FDroidApp) getApplication()).applyTheme(this);

        if (savedInstanceState == null) {
            listFragment = new RepoListFragment();
            getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, listFragment)
                .commit();
        }

        ActionBarCompat.create(this).setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void finish() {
        Intent ret = new Intent();
        markChangedIfRequired(ret);
        setResult(Activity.RESULT_OK, ret);
        super.finish();
    }

    private boolean hasChanged() {
        return listFragment != null && listFragment.hasChanged();
    }

    private void markChangedIfRequired(Intent intent) {
        if (hasChanged()) {
            Log.i("FDroid", "Repo details have changed, prompting for update.");
            intent.putExtra(REQUEST_UPDATE, true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent destIntent = new Intent(this, FDroid.class);
                markChangedIfRequired(destIntent);
                setResult(RESULT_OK, destIntent);
                NavUtils.navigateUpTo(this, destIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
