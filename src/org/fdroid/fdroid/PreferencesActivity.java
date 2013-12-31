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
import android.preference.PreferenceActivity;
import android.preference.ListPreference;
import android.preference.CheckBoxPreference;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.view.MenuItem;

import android.support.v4.app.NavUtils;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.compat.ActionBarCompat;

public class PreferencesActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {

    public static final int RESULT_RELOAD = 1;
    public static final int RESULT_REFILTER = 2;
    public static final int RESULT_RESTART = 4;
    private int result = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((FDroidApp) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);
        ActionBarCompat.create(this).setDisplayHomeAsUpEnabled(true);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(
                            (OnSharedPreferenceChangeListener)this);
        CheckBoxPreference onlyOnWifi = (CheckBoxPreference)
                findPreference(Preferences.PREF_UPD_WIFI_ONLY);
        onlyOnWifi.setEnabled(Integer.parseInt(((ListPreference)
                        findPreference(Preferences.PREF_UPD_INTERVAL))
                        .getValue()) > 0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
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
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (key.equals(Preferences.PREF_UPD_INTERVAL)) {
            int interval = Integer.parseInt(
                    sharedPreferences.getString(key, "").toString());
            CheckBoxPreference onlyOnWifi = (CheckBoxPreference)
                    findPreference(Preferences.PREF_UPD_WIFI_ONLY);
            onlyOnWifi.setEnabled(interval > 0);
            return;
        }

        if (key.equals(Preferences.PREF_INCOMP_VER)) {
            result ^= RESULT_RELOAD;
            setResult(result);
            return;
        }

        if (key.equals(Preferences.PREF_ROOTED)) {
            result ^= RESULT_REFILTER;
            setResult(result);
            return;
        }
        if (key.equals(Preferences.PREF_THEME)) {
            result |= RESULT_RESTART;
            setResult(result);
            return;
        }
    }

}
