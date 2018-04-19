package org.fdroid.fdroid;

import android.app.Application;
import android.content.ContentValues;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppPrefs;
import org.fdroid.fdroid.data.AppPrefsProvider;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.FDroidProviderTest;
import org.fdroid.fdroid.data.InstalledAppTestUtils;
import org.fdroid.fdroid.data.Schema;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@Config(constants = BuildConfig.class, application = Application.class)
@RunWith(RobolectricTestRunner.class)
public class AntiFeaturesTest extends FDroidProviderTest {

    private App notVuln;
    private App allVuln;
    private App vulnAtV2;

    @Before
    public void setup() {
        Preferences.setupForTests(context);

        ContentValues vulnValues = new ContentValues(1);
        vulnValues.put(Schema.ApkTable.Cols.AntiFeatures.ANTI_FEATURES, "KnownVuln,ContainsGreenButtons");

        vulnAtV2 = Assert.insertApp(context, "com.vuln", "Fixed it");
        insertApk(vulnAtV2, 1, false);
        insertApk(vulnAtV2, 2, true);
        insertApk(vulnAtV2, 3, false);

        notVuln = Assert.insertApp(context, "com.not-vuln", "It's Fine");
        insertApk(notVuln, 5, false);
        insertApk(notVuln, 10, false);
        insertApk(notVuln, 15, false);

        allVuln = Assert.insertApp(context, "com.all-vuln", "Oops");
        insertApk(allVuln, 100, true);
        insertApk(allVuln, 101, true);
        insertApk(allVuln, 105, true);

        AppProvider.Helper.recalculatePreferredMetadata(context);
    }

    private static String generateHash(String packageName, int versionCode) {
        return packageName + "-" + versionCode;
    }

    private void insertApk(App app, int versionCode, boolean isVuln) {
        ContentValues values = new ContentValues();
        values.put(Schema.ApkTable.Cols.HASH, generateHash(app.packageName, versionCode));
        if (isVuln) {
            values.put(Schema.ApkTable.Cols.AntiFeatures.ANTI_FEATURES, "KnownVuln,ContainsGreenButtons");
        }
        Assert.insertApk(context, app, versionCode, values);
    }

    private void install(App app, int versionCode) {
        String hash = generateHash(app.packageName, versionCode);
        InstalledAppTestUtils.install(context, app.packageName, versionCode, "v" + versionCode, null, hash);
    }

    @Test
    public void noVulnerableApps() {
        List<App> installed = AppProvider.Helper.findInstalledAppsWithKnownVulns(context);
        assertEquals(0, installed.size());
    }

    @Test
    public void futureVersionIsVulnerable() {
        install(vulnAtV2, 1);
        List<App> installed = AppProvider.Helper.findInstalledAppsWithKnownVulns(context);
        assertEquals(0, installed.size());
    }

    @Test
    public void vulnerableAndAbleToBeUpdated() {
        install(vulnAtV2, 2);
        List<App> installed = AppProvider.Helper.findInstalledAppsWithKnownVulns(context);
        assertEquals(1, installed.size());
        assertEquals(vulnAtV2.packageName, installed.get(0).packageName);
    }

    @Test
    public void vulnerableButUpToDate() {
        install(vulnAtV2, 3);
        List<App> installed = AppProvider.Helper.findInstalledAppsWithKnownVulns(context);
        assertEquals(0, installed.size());
    }

    @Test
    public void allVulnerableButIgnored() {
        install(allVuln, 101);
        List<App> installed = AppProvider.Helper.findInstalledAppsWithKnownVulns(context);
        assertEquals(1, installed.size());

        App app = installed.get(0);
        AppPrefs prefs = app.getPrefs(context);
        prefs.ignoreVulnerabilities = true;
        AppPrefsProvider.Helper.update(context, app, prefs);

        List<App> installedButIgnored = AppProvider.Helper.findInstalledAppsWithKnownVulns(context);
        assertEquals(0, installedButIgnored.size());
    }

    @Test
    public void antiFeaturesSaveCorrectly() {
        List<Apk> notVulnApks = ApkProvider.Helper.findByPackageName(context, notVuln.packageName);
        assertEquals(3, notVulnApks.size());

        List<Apk> allVulnApks = ApkProvider.Helper.findByPackageName(context, allVuln.packageName);
        assertEquals(3, allVulnApks.size());
        for (Apk apk : allVulnApks) {
            assertArrayEquals(new String[]{"KnownVuln", "ContainsGreenButtons"}, apk.antiFeatures);
        }

        List<Apk> vulnAtV2Apks = ApkProvider.Helper.findByPackageName(context, vulnAtV2.packageName);
        assertEquals(3, vulnAtV2Apks.size());
        for (Apk apk : vulnAtV2Apks) {
            if (apk.versionCode == 2) {
                assertArrayEquals(new String[]{"KnownVuln", "ContainsGreenButtons"}, apk.antiFeatures);
            } else {
                assertNull(apk.antiFeatures);
            }
        }
    }

}
