package org.fdroid.fdroid;

import android.app.Application;
import android.content.ContentValues;

import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.FDroidProviderTest;
import org.fdroid.fdroid.data.Schema;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@Config(constants = BuildConfig.class, application = Application.class, sdk = 24)
@RunWith(RobolectricTestRunner.class)
public class AntiFeaturesTest extends FDroidProviderTest {

    @Test
    public void testPerApkAntiFeatures() throws IOException, RepoUpdater.UpdateException {
        ContentValues vulnValues = new ContentValues(1);
        vulnValues.put(Schema.ApkTable.Cols.AntiFeatures.ANTI_FEATURES, "KnownVuln,ContainsGreenButtons");

        App vulnAtV2 = Assert.insertApp(context, "com.vuln", "Fixed it");
        Assert.insertApk(context, vulnAtV2, 1);
        Assert.insertApk(context, vulnAtV2, 2, vulnValues);
        Assert.insertApk(context, vulnAtV2, 3);

        App notVuln = Assert.insertApp(context, "com.not-vuln", "It's Fine");
        Assert.insertApk(context, notVuln, 5);
        Assert.insertApk(context, notVuln, 10);
        Assert.insertApk(context, notVuln, 15);

        App allVuln = Assert.insertApp(context, "com.all-vuln", "Oops");
        Assert.insertApk(context, allVuln, 100, vulnValues);
        Assert.insertApk(context, allVuln, 101, vulnValues);
        Assert.insertApk(context, allVuln, 105, vulnValues);

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
