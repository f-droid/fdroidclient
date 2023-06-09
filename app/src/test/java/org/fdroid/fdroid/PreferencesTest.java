/*
 * Copyright (C) 2018  Senecto Limited
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class PreferencesTest {
    private static final String TAG = "PreferencesTest";

    private static final Context CONTEXT = ApplicationProvider.getApplicationContext();

    private SharedPreferences defaults;

    /**
     * Manually read the {@code preferences.xml} defaults to a separate
     * instance. Clear the preference state before each test so that each
     * test starts as if it was a first time install.
     */
    @Before
    public void setup() {
        ShadowLog.stream = System.out;

        defaults = getSharedPreferences(CONTEXT);
        assertTrue(defaults.getAll().size() > 0);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CONTEXT);
        Log.d(TAG, "Clearing DefaultSharedPreferences containing: " + sharedPreferences.getAll().size());
        sharedPreferences.edit().clear().commit();
        assertEquals(0, sharedPreferences.getAll().size());

        SharedPreferences defaultValueSp = CONTEXT.getSharedPreferences(PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES,
                Context.MODE_PRIVATE);
        defaultValueSp.edit().remove(PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES).commit();
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        String sharedPreferencesName = context.getPackageName() + "_preferences_defaults";
        PreferenceManager pm = new PreferenceManager(context);
        pm.setSharedPreferencesName(sharedPreferencesName);
        pm.setSharedPreferencesMode(Context.MODE_PRIVATE);
        pm.inflateFromResource(context, R.xml.preferences, null);
        return pm.getSharedPreferences();
    }

    /**
     * Check that the defaults are being set when using
     * {@link PreferenceManager#getDefaultSharedPreferences(Context)}, and that
     * the values match.  {@link Preferences#Preferences(Context)} sets the
     * values of {@link Preferences#PREF_LOCAL_REPO_NAME} and
     * {@link Preferences#PREF_AUTO_DOWNLOAD_INSTALL_UPDATES} dynamically, so
     * there are two more preferences.
     */
    @Test
    public void testSetDefaultValues() {
        Preferences.setupForTests(CONTEXT);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CONTEXT);
        assertEquals(defaults.getAll().size() + 2, sharedPreferences.getAll().size());
        assertTrue(sharedPreferences.contains(Preferences.PREF_LOCAL_REPO_NAME));
        assertTrue(sharedPreferences.contains(Preferences.PREF_AUTO_DOWNLOAD_INSTALL_UPDATES));

        Map<String, ?> entries = sharedPreferences.getAll();
        for (Map.Entry<String, ?> entry : defaults.getAll().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            assertTrue(sharedPreferences.contains(entry.getKey()));
            assertEquals(entries.get(key), value);
        }

        String key = Preferences.PREF_EXPERT;
        boolean defaultValue = defaults.getBoolean(key, false);
        sharedPreferences.edit().putBoolean(key, !defaultValue).commit();
        assertNotEquals(defaultValue, sharedPreferences.getBoolean(key, false));
    }

    @Test
    public void testMethodsUseDefaults() {
        Preferences.setupForTests(CONTEXT);
        Preferences preferences = Preferences.get();

        assertEquals(defaults.getBoolean(Preferences.PREF_EXPERT, false),
                preferences.expertMode());
        assertEquals(defaults.getBoolean(Preferences.PREF_FORCE_TOUCH_APPS, false),
                preferences.isForceOldIndexEnabled());
        assertEquals(defaults.getBoolean(Preferences.PREF_FORCE_OLD_INDEX, false),
                preferences.isForceOldIndexEnabled());
        assertEquals(defaults.getBoolean(Preferences.PREF_PREVENT_SCREENSHOTS, false),
                preferences.preventScreenshots());
        assertEquals(defaults.getStringSet(Preferences.PREF_SHOW_ANTI_FEATURES, null),
                preferences.showAppsWithAntiFeatures());
        assertEquals(defaults.getBoolean(Preferences.PREF_SHOW_INCOMPAT_VERSIONS, false),
                preferences.showIncompatibleVersions());
        assertEquals(defaults.getBoolean(Preferences.PREF_UPDATE_NOTIFICATION_ENABLED, false),
                preferences.isUpdateNotificationEnabled());

        assertEquals(Long.parseLong(defaults.getString(Preferences.PREF_KEEP_CACHE_TIME, null)),
                preferences.getKeepCacheTime());

        assertEquals(Preferences.Theme.valueOf(defaults.getString(Preferences.PREF_THEME, null)),
                preferences.getTheme());

        // now test setting the prefs
        boolean defaultValue = defaults.getBoolean(Preferences.PREF_EXPERT, false);
        preferences.setExpertMode(!defaultValue);
        assertNotEquals(defaultValue, preferences.expertMode());
    }

    /**
     * When {@link Preferences#Preferences(Context)} calls
     * {@link PreferenceManager#setDefaultValues(Context, int, boolean)}, any
     * existing preference values should not be overridden.
     */
    @Test
    public void testMigrationWithSetDefaultValues() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CONTEXT);
        long testValue = (long) (Math.random() * Long.MAX_VALUE);
        String testValueString = String.valueOf(testValue);
        assertNotEquals(testValueString, defaults.getString(Preferences.PREF_KEEP_CACHE_TIME, "not a long"));
        sharedPreferences.edit().putString(Preferences.PREF_KEEP_CACHE_TIME, testValueString).commit();

        Preferences.setupForTests(CONTEXT);
        assertEquals(testValue, Preferences.get().getKeepCacheTime());
        assertNotEquals(Long.parseLong(defaults.getString(Preferences.PREF_KEEP_CACHE_TIME, null)),
                Preferences.get().getKeepCacheTime());
    }
}
