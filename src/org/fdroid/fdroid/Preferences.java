/*
 * Copyright (C) 2010  Ciaran Gultnieks, ciaran@ciarang.com
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

import java.io.File;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;

public class Preferences extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        Preference r = (Preference) findPreference("reset");
        r.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                
                // TODO: Progress dialog + thread is needed, it can take a
                // while to delete all the icons and cached apks in a long
                // standing install!
                Toast.makeText(getBaseContext(),
                        "Hold on...", Toast.LENGTH_SHORT)
                        .show();

                // TODO: This is going to cause problems if there is background
                // update in progress at the time!

                try {
                    DB db = DB.getDB();
                    db.reset();
                } finally {
                    DB.releaseDB();
                }
                ((FDroidApp) getApplication()).invalidateApps();

                File dp = DB.getDataPath();
                deleteAll(dp);
                dp.mkdir();
                DB.getIconsPath().mkdir();

                Toast.makeText(getBaseContext(),
                        "Local cached data has been cleared", Toast.LENGTH_LONG)
                        .show();
                return true;
            }

        });

    }

    @Override
    public void finish() {
        Intent ret = new Intent();
        this.setResult(RESULT_OK, ret);
        super.finish();
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

}
