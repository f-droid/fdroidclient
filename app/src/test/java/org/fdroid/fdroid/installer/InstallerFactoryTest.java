package org.fdroid.fdroid.installer;

import static org.junit.Assert.assertEquals;

import android.content.ContextWrapper;

import androidx.test.core.app.ApplicationProvider;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.index.v2.FileV1;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class InstallerFactoryTest {

    private ContextWrapper context;

    @Before
    public final void setUp() {
        context = ApplicationProvider.getApplicationContext();
        Preferences.setupForTests(context);
    }

    @Test
    public void testApkInstallerInstance() {
        for (String filename : new String[]{"test.apk", "A.APK", "b.ApK"}) {
            App app = new App();
            app.packageName = "test";
            Apk apk = new Apk();
            apk.apkFile = new FileV1(filename, "hash", null, null);
            apk.packageName = "test";
            Installer installer = InstallerFactory.create(context, app, apk);
            assertEquals(filename + " should use a DefaultInstaller",
                    DefaultInstaller.class,
                    installer.getClass());
        }
    }

    @Test
    public void testFileInstallerInstance() {
        for (String filename : new String[]{"org.fdroid.fdroid.privileged.ota_2110.zip", "test.ZIP"}) {
            App app = new App();
            app.packageName = "cafe0088";
            Apk apk = new Apk();
            apk.apkFile = new FileV1(filename, "hash", null, null);
            apk.packageName = "cafe0088";
            Installer installer = InstallerFactory.create(context, app, apk);
            assertEquals("should be a FileInstaller",
                    FileInstaller.class,
                    installer.getClass());
        }
    }
}
