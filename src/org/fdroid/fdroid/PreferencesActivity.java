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
import android.preference.ListPreference;
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

        ListPreference updateInterval = (ListPreference)findPreference(
                Preferences.PREF_UPD_INTERVAL);

        int interval = Integer.parseInt(updateInterval.getValue().toString());

        Preference onlyOnWifi = findPreference(
                Preferences.PREF_UPD_WIFI_ONLY);
        onlyOnWifi.setEnabled(interval > 0);

        if (interval == 0) {
            updateInterval.setSummary(R.string.update_interval_zero);
        } else {
            updateInterval.setSummary(updateInterval.getEntry());
        }
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
    public void onSharedPreferenceChanged(
            SharedPreferences sharedPreferences, String key) {

        if (key.equals(Preferences.PREF_UPD_INTERVAL)) {
            ListPreference pref = (ListPreference)findPreference(
                    Preferences.PREF_UPD_INTERVAL);
            int interval = Integer.parseInt(pref.getValue().toString());
            Preference onlyOnWifi = findPreference(
                    Preferences.PREF_UPD_WIFI_ONLY);
            onlyOnWifi.setEnabled(interval > 0);
            if (interval == 0) {
                pref.setSummary(R.string.update_interval_zero);
            } else {
                pref.setSummary(pref.getEntry());
            }
            return;
        }

        if (key.equals(Preferences.PREF_UPD_WIFI_ONLY)) {
            Preference pref = findPreference(Preferences.PREF_UPD_WIFI_ONLY);
            if (sharedPreferences.getBoolean(
                        Preferences.PREF_UPD_WIFI_ONLY, false)) {
                pref.setSummary(R.string.automatic_scan_wifi_on);
            } else {
                pref.setSummary(R.string.automatic_scan_wifi_off);
            }
            return;
        }

        if (key.equals(Preferences.PREF_COMPACT_LAYOUT)) {
            Preference pref = findPreference(Preferences.PREF_COMPACT_LAYOUT);
            if (sharedPreferences.getBoolean(
                        Preferences.PREF_COMPACT_LAYOUT, false)) {
                pref.setSummary(R.string.compactlayout_on);
            } else {
                pref.setSummary(R.string.compactlayout_off);
            }
            return;
        }

        if (key.equals(Preferences.PREF_COMPACT_LAYOUT)) {
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
            Preference pref = findPreference(Preferences.PREF_ROOTED);
            if (sharedPreferences.getBoolean(
                        Preferences.PREF_ROOTED, true)) {
                pref.setSummary(R.string.rooted_on);
            } else {
                pref.setSummary(R.string.rooted_off);
            }
            return;
        }

        if (key.equals(Preferences.PREF_IGN_TOUCH)) {
            Preference pref = findPreference(Preferences.PREF_IGN_TOUCH);
            if (sharedPreferences.getBoolean(
                        Preferences.PREF_IGN_TOUCH, false)) {
                pref.setSummary(R.string.ignoreTouch_on);
            } else {
                pref.setSummary(R.string.ignoreTouch_off);
            }
            return;
        }

        if (key.equals(Preferences.PREF_THEME)) {
            result |= RESULT_RESTART;
            setResult(result);
            return;
        }
    }

}
