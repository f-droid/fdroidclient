package org.fdroid.fdroid.data;

import android.app.Application;

import org.fdroid.fdroid.Assert;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.TestUtils;
import org.fdroid.fdroid.data.Schema.AppPrefsTable.Cols;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

@Config(constants = BuildConfig.class, application = Application.class)
@RunWith(RobolectricGradleTestRunner.class)
public class AppPrefsProviderTest extends FDroidProviderTest {

    private static final String[] PROJ = Cols.ALL;

    @Before
    public void setup() {
        ShadowContentResolver.registerProvider(AppProvider.getAuthority(), new AppProvider());
    }

    @Test
    public void newPreferences() {
        App withPrefs = Assert.insertApp(context, "com.example.withPrefs", "With Prefs");
        App withoutPrefs = Assert.insertApp(context, "com.example.withoutPrefs", "Without Prefs");

        assertNull(AppPrefsProvider.Helper.getPrefsOrNull(context, withPrefs));
        assertNull(AppPrefsProvider.Helper.getPrefsOrNull(context, withoutPrefs));

        AppPrefs defaultPrefs = AppPrefsProvider.Helper.getPrefsOrDefault(context, withPrefs);
        assertEquals(0, defaultPrefs.ignoreThisUpdate);
        assertFalse(defaultPrefs.ignoreAllUpdates);

        AppPrefsProvider.Helper.update(context, withPrefs, new AppPrefs(12, false));
        AppPrefs newPrefs = AppPrefsProvider.Helper.getPrefsOrDefault(context, withPrefs);
        assertEquals(12, newPrefs.ignoreThisUpdate);
        assertFalse(newPrefs.ignoreAllUpdates);

        AppPrefsProvider.Helper.update(context, withPrefs, new AppPrefs(14, true));
        AppPrefs evenNewerPrefs = AppPrefsProvider.Helper.getPrefsOrDefault(context, withPrefs);
        assertEquals(14, evenNewerPrefs.ignoreThisUpdate);
        assertTrue(evenNewerPrefs.ignoreAllUpdates);

        assertNull(AppPrefsProvider.Helper.getPrefsOrNull(context, withoutPrefs));
    }
}
