package org.fdroid.fdroid.data;

import android.app.Application;
import org.fdroid.fdroid.Assert;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@Config(constants = BuildConfig.class, application = Application.class)
@RunWith(RobolectricTestRunner.class)
public class AppPrefsProviderTest extends FDroidProviderTest {

    @Before
    public void setup() {
        TestUtils.registerContentProvider(AppProvider.getAuthority(), AppProvider.class);
    }

    @SuppressWarnings({"PMD.EqualsNull", "EqualsWithItself", "EqualsBetweenInconvertibleTypes", "ObjectEqualsNull"})
    @Test
    public void prefEquality() {
        AppPrefs original = new AppPrefs(101, true, true);

        assertTrue(original.equals(new AppPrefs(101, true, true)));
        assertTrue(original.equals(original));

        assertFalse(original.equals(null));
        assertFalse(original.equals("String"));
        assertFalse(original.equals(new AppPrefs(102, true, true)));
        assertFalse(original.equals(new AppPrefs(101, false, true)));
        assertFalse(original.equals(new AppPrefs(100, false, true)));
        assertFalse(original.equals(new AppPrefs(102, true, false)));
        assertFalse(original.equals(new AppPrefs(101, false, false)));
        assertFalse(original.equals(new AppPrefs(100, false, false)));
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
        assertFalse(defaultPrefs.ignoreVulnerabilities);

        AppPrefsProvider.Helper.update(context, withPrefs, new AppPrefs(12, false, false));
        AppPrefs newPrefs = AppPrefsProvider.Helper.getPrefsOrDefault(context, withPrefs);
        assertEquals(12, newPrefs.ignoreThisUpdate);
        assertFalse(newPrefs.ignoreAllUpdates);
        assertFalse(newPrefs.ignoreVulnerabilities);

        AppPrefsProvider.Helper.update(context, withPrefs, new AppPrefs(14, true, true));
        AppPrefs evenNewerPrefs = AppPrefsProvider.Helper.getPrefsOrDefault(context, withPrefs);
        assertEquals(14, evenNewerPrefs.ignoreThisUpdate);
        assertTrue(evenNewerPrefs.ignoreAllUpdates);
        assertTrue(evenNewerPrefs.ignoreVulnerabilities);

        assertNull(AppPrefsProvider.Helper.getPrefsOrNull(context, withoutPrefs));
    }
}
