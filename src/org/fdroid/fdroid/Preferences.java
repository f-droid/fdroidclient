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

import java.io.File;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;

public class Preferences extends PreferenceActivity implements
        OnPreferenceClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Utils.hasApi(11))
            getActionBar().setDisplayHomeAsUpEnabled(true);
        addPreferencesFromResource(R.xml.preferences);
        for (String prefkey : new String[] { "reset", "ignoreTouchscreen",
                "showIncompatible" }) {
            Preference pref = findPreference(prefkey);
            pref.setOnPreferenceClickListener(this);
        }
    }

    private void deleteAll(File dir) {

        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                deleteAll(new File(dir, children[i]));
            }
        }
        dir.delete();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (key.equals("ignoreTouchscreen") || key.equals("showIncompatible")) {
            Intent ret = new Intent();
            ret.putExtra("update", true);
            setResult(RESULT_OK, ret);
            return true;
        } else if (key.equals("reset")) {
            // TODO: Progress dialog + thread is needed, it can take a
            // while to delete all the icons and cached apks in a long
            // standing install!

            // TODO: This is going to cause problems if there is background
            // update in progress at the time!

            try {
                DB db = DB.getDB();
                db.reset();
            } finally {
                DB.releaseDB();
            }
            ((FDroidApp) getApplication()).invalidateAllApps();

            File dp = DB.getDataPath(this);
            deleteAll(dp);
            dp.mkdir();
            DB.getIconsPath(this).mkdir();

            Toast.makeText(getBaseContext(),
                    "Local cached data has been cleared", Toast.LENGTH_LONG)
                    .show();
            Intent ret = new Intent();
            ret.putExtra("reset", true);
            setResult(RESULT_OK, ret);
            finish();
            return true;
        }
        return false;
    }

}
