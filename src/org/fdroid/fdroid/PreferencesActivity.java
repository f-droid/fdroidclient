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
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
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

    private static String[] summariesToUpdate = {
                    Preferences.PREF_UPD_INTERVAL,
                    Preferences.PREF_UPD_WIFI_ONLY,
                    Preferences.PREF_UPD_HISTORY,
                    Preferences.PREF_ROOTED,
                    Preferences.PREF_INCOMP_VER,
                    Preferences.PREF_THEME,
                    Preferences.PREF_PERMISSIONS,
                    Preferences.PREF_COMPACT_LAYOUT,
                    Preferences.PREF_IGN_TOUCH,
                    Preferences.PREF_DB_SYNC,
                    Preferences.PREF_CACHE_APK };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((FDroidApp) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);
        ActionBarCompat.create(this).setDisplayHomeAsUpEnabled(true);
        addPreferencesFromResource(R.xml.preferences);
    }

    protected void updateSummary(String key) {

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
            CheckBoxPreference pref = (CheckBoxPreference)findPreference(
                    Preferences.PREF_UPD_WIFI_ONLY);
            if (pref.isChecked()) {
                pref.setSummary(R.string.automatic_scan_wifi_on);
            } else {
                pref.setSummary(R.string.automatic_scan_wifi_off);
            }
            return;
        }

        if (key.equals(Preferences.PREF_UPD_HISTORY)) {
            EditTextPreference pref = (EditTextPreference)findPreference(
                    Preferences.PREF_UPD_HISTORY);
            pref.setSummary(getString(R.string.update_history_summ,
                        pref.getText()));
            return;
        }

        if (key.equals(Preferences.PREF_COMPACT_LAYOUT)) {
            CheckBoxPreference pref = (CheckBoxPreference)findPreference(
                    Preferences.PREF_COMPACT_LAYOUT);
            if (pref.isChecked()) {
                pref.setSummary(R.string.compactlayout_on);
            } else {
                pref.setSummary(R.string.compactlayout_off);
            }
            return;
        }

        if (key.equals(Preferences.PREF_INCOMP_VER)) {
            result ^= RESULT_RELOAD;
            setResult(result);
            CheckBoxPreference pref = (CheckBoxPreference)findPreference(
                    Preferences.PREF_INCOMP_VER);
            if (pref.isChecked()) {
                pref.setSummary(R.string.show_incompat_versions_on);
            } else {
                pref.setSummary(R.string.show_incompat_versions_off);
            }
            return;
        }

        if (key.equals(Preferences.PREF_ROOTED)) {
            result ^= RESULT_REFILTER;
            setResult(result);
            CheckBoxPreference pref = (CheckBoxPreference)findPreference(
                    Preferences.PREF_ROOTED);
            if (pref.isChecked()) {
                pref.setSummary(R.string.rooted_on);
            } else {
                pref.setSummary(R.string.rooted_off);
            }
            return;
        }

        if (key.equals(Preferences.PREF_IGN_TOUCH)) {
            CheckBoxPreference pref = (CheckBoxPreference)findPreference(
                    Preferences.PREF_IGN_TOUCH);
            if (pref.isChecked()) {
                pref.setSummary(R.string.ignoreTouch_on);
            } else {
                pref.setSummary(R.string.ignoreTouch_off);
            }
            return;
        }

        if (key.equals(Preferences.PREF_THEME)) {
            result |= RESULT_RESTART;
            setResult(result);
            ListPreference pref = (ListPreference)findPreference(
                    Preferences.PREF_THEME);
            pref.setSummary(pref.getEntry());
            return;
        }

        if (key.equals(Preferences.PREF_PERMISSIONS)) {
            CheckBoxPreference pref = (CheckBoxPreference)findPreference(
                    Preferences.PREF_PERMISSIONS);
            if (pref.isChecked()) {
                pref.setSummary(R.string.showPermissions_on);
            } else {
                pref.setSummary(R.string.showPermissions_off);
            }
            return;
        }

        if (key.equals(Preferences.PREF_CACHE_APK)) {
            CheckBoxPreference pref = (CheckBoxPreference)findPreference(
                    Preferences.PREF_CACHE_APK);
            if (pref.isChecked()) {
                pref.setSummary(R.string.cache_downloaded_on);
            } else {
                pref.setSummary(R.string.cache_downloaded_off);
            }
            return;
        }

        if (key.equals(Preferences.PREF_DB_SYNC)) {
            ListPreference pref = (ListPreference)findPreference(
                    Preferences.PREF_DB_SYNC);
            pref.setSummary(pref.getEntry());
            return;
        }
    }

    @Override
    protected void onResume() {

        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(
                            (OnSharedPreferenceChangeListener)this);

        for (String key : summariesToUpdate) {
            updateSummary(key);
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

        updateSummary(key);
    }

}
