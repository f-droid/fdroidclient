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

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.ListPreference;
import android.preference.CheckBoxPreference;
import android.view.MenuItem;

import android.support.v4.app.NavUtils;

import org.fdroid.fdroid.compat.ActionBarCompat;

public class PreferencesActivity extends PreferenceActivity implements
        OnPreferenceChangeListener {

    public static final int RESULT_RELOAD = 1;
    public static final int RESULT_REFILTER = 2;
    private int result = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBarCompat.create(this).setDisplayHomeAsUpEnabled(true);
        addPreferencesFromResource(R.xml.preferences);
        for (String prefkey : new String[] {
                "updateInterval", "rooted", "incompatibleVersions" }) {
            findPreference(prefkey).setOnPreferenceChangeListener(this);
        }
        CheckBoxPreference onlyOnWifi = (CheckBoxPreference)
                findPreference("updateOnWifiOnly");
        onlyOnWifi.setEnabled(Integer.parseInt(
                        ((ListPreference)findPreference("updateInterval"))
                        .getValue()) > 0);
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

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (key.equals("updateInterval")) {
            int interval = Integer.parseInt(newValue.toString());
            CheckBoxPreference onlyOnWifi = (CheckBoxPreference)
                    findPreference("updateOnWifiOnly");
            onlyOnWifi.setEnabled(interval > 0);
            return true;
        }
        if (key.equals("incompatibleVersions")) {
            result ^= RESULT_RELOAD;
            setResult(result);
            return true;
        }
        if (key.equals("rooted")) {
            result ^= RESULT_REFILTER;
            setResult(result);
            return true;
        }
        return false;
    }

}
