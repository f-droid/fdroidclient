package org.fdroid.fdroid.installer;

import android.content.ContextWrapper;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.data.Apk;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import androidx.test.core.app.ApplicationProvider;

import static org.junit.Assert.assertEquals;

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
            Apk apk = new Apk();
            apk.apkName = filename;
            apk.packageName = "test";
            Installer installer = InstallerFactory.create(context, apk);
            assertEquals(filename + " should use a DefaultInstaller",
                    DefaultInstaller.class,
                    installer.getClass());
        }
    }

    @Test
    public void testFileInstallerInstance() {
        for (String filename : new String[]{"org.fdroid.fdroid.privileged.ota_2110.zip", "test.ZIP"}) {
            Apk apk = new Apk();
            apk.apkName = filename;
            apk.packageName = "cafe0088";
            Installer installer = InstallerFactory.create(context, apk);
            assertEquals("should be a FileInstaller",
                    FileInstaller.class,
                    installer.getClass());
        }
    }
}
